package com.riseofcat.share.mass

import com.riseofcat.client.*
import com.riseofcat.common.*
import kotlinx.serialization.*
import kotlin.math.*

object GameConst {
  val UPDATE_MS = 40
  val UPDATE_S = UPDATE_MS/1000f
  val MIN_SIZE = 20
  val FOOD_SIZE = 20
  val MIN_RADIUS = 1f
  val FOODS = 20
  val BASE_WIDTH = 1000
  val BASE_HEIGHT = 1000
  val TITLE = "mass-power.io"
  val DELAY_TICKS = PingClient.DEFAULT_LATENCY_MS*3/GameConst.UPDATE_MS+1//количество тиков для хранения действий //bigger delayed
  val REMOVE_TICKS = DELAY_TICKS*3//bigger removed
  val FUTURE_TICKS = DELAY_TICKS*3
}


interface GetCarById {
  fun getCar(id:PlayerId):Car?
}

interface InStateAction {
  fun act(state:State,getCar:GetCarById)
}

interface PosObject {
  var pos:XY
}

interface SpeedObject:PosObject {
  var speed:XY
}

interface EatMe:SpeedObject {
  var size:Int
  fun radius() = (kotlin.math.sqrt(size.toDouble())*5f).toFloat()+GameConst.MIN_RADIUS
}

@Serializable class BigAction(
  @Optional val n:NewCarAction? = null,
  @Optional val p:PlayerAction? = null
):InStateAction {
  override fun act(state:State,getCar:GetCarById) {
    n?.act(state,getCar)
    p?.act(state,getCar)
  }
}

@Serializable data class Action(var direction:Angle)
@Serializable data class Angle(var radians:Float) {
  init {
    val circles = radians/(2*kotlin.math.PI)
    if(kotlin.math.abs(circles)>0) {
      circles.toInt()
      circles.sign
    }
  }

  fun sin() = kotlin.math.sin(radians.toDouble()).toFloat()
  fun cos() = kotlin.math.cos(radians.toDouble()).toFloat()
  fun xy():XY = XY(cos(),sin())
  fun add(deltaAngle:Angle) = Angle(this.radians+deltaAngle.radians)
  fun subtract(sub:Angle) = Angle(this.radians-sub.radians)

  companion object {
    fun degreesAngle(degrees:Float) = Angle(degrees/180*PI.toFloat())
  }
}

val Angle.degrees:Float get() = (radians*180/kotlin.math.PI).toFloat()
val Angle.gdxTransformRotation:Float get() = degrees

@Serializable data class Car(
  var owner:PlayerId,
  override var size:Int,
  override var speed:XY,
  override var pos:XY):EatMe

@Serializable data class Food(
  override var size:Int,
  override var speed:XY,
  override var pos:XY):EatMe

@Serializable class NewCarAction(var id:PlayerId):InStateAction {
  override fun act(state:State,getCar:GetCarById) {
    state.changeSize(100)
    state.cars.add(Car(id,GameConst.MIN_SIZE*6,XY().mutable(),XY()))
  }

  fun toBig() = BigAction(n = this)
}

@Serializable class PlayerAction(
  var id:PlayerId,
  var action:Action):InStateAction {
  override fun act(state:State,getCar:GetCarById) {
    val scl = 100f
    val car = getCar.getCar(id) ?: return //todo handle null ?
    car.speed = car.speed.add(action.direction.xy(),scl)
    val s = car.size/15+1
    if(car.size-s>=GameConst.MIN_SIZE) car.size = car.size-s
    state.reactive.add(Reactive(id,s,action.direction.add(Angle.degreesAngle(180f)).xy().scale(3f*scl).mutable(),car.pos.mutable()))
  }

  fun toBig() = BigAction(p = this)
}

@Serializable data class Reactive(
  var owner:PlayerId,
  override var size:Int,
  override var speed:XY,
  override var pos:XY,
  var ticks:Int = 0):EatMe

@Serializable data class State(
  @Optional var cars:MutableList<Car> = mutableListOf(),
  @Optional var foods:MutableList<Food> = mutableListOf(),
  @Optional var reactive:MutableList<Reactive> = mutableListOf(),
  var random:Int = 0,
  var size:Int = 0):MayClone<State> {

  override fun clone():State {
    return copy()
  }

  fun act(actions:Iterator<InStateAction>):State {
    class Cache:GetCarById {
      override fun getCar(id:PlayerId):Car? {
        for(car in cars) if(id==car.owner) return car
        return null
      }
    }

    val cache = Cache()
    actions.forEach {
      it.act(this,cache)
    }
    return this
  }

  fun tick():State {
    val iterateFun:(SpeedObject)->Unit = {o->
      o.pos = o.pos.add(o.speed.mutable(),GameConst.UPDATE_S)
      if(o.pos.x>=width())
        o.pos.x = o.pos.x-width()
      else if(o.pos.x<0) o.pos.x = o.pos.x+width()
      if(o.pos.y>=height())
        o.pos.y = o.pos.y-height()
      else if(o.pos.y<0) o.pos.y = o.pos.y+height()
      o.speed = o.speed.scale(0.98f)
    }
    cars.forEach(iterateFun)
    reactive.forEach(iterateFun)
    var reactItr:MutableIterator<Reactive> = reactive.iterator()
    while(reactItr.hasNext()) if(reactItr.next().ticks++>60) reactItr.remove()
    for(car in cars) {
      val foodItr = foods.iterator()
      while(foodItr.hasNext()) {
        val (size1,_,pos) = foodItr.next()
        if(distance(car.pos,pos)<=car.radius()) {
          car.size = car.size+size1
          foodItr.remove()
        }
      }
      reactItr = reactive.iterator()
      while(reactItr.hasNext()) {
        val r = reactItr.next()
        if(r.owner!=car.owner&&distance(car.pos,r.pos)<=car.radius()) {
          car.size = car.size+r.size
          reactItr.remove()
        }
      }
    }
    if(foods.size<GameConst.FOODS) foods.add(Food(GameConst.FOOD_SIZE,XY(),rndPos()))
    return this
  }
}

fun State.width() = (GameConst.BASE_WIDTH+size).toFloat()
fun State.height() = (GameConst.BASE_HEIGHT+size).toFloat()
fun State.distance(a:XY,b:XY):Float {
  var dx = kotlin.math.min(kotlin.math.abs(b.x-a.x),b.x+width()-a.x)
  dx = kotlin.math.min(dx,a.x+width()-b.x)
  var dy = kotlin.math.min(kotlin.math.abs(b.y-a.y),b.y+height()-a.y)
  dy = kotlin.math.min(dy,a.y+height()-b.y)
  return kotlin.math.sqrt((dx*dx+dy*dy).toDouble()).toFloat()
}

fun State.rnd(min:Int,max:Int):Int {
  random = random*1664525+1013904223 and 0x7fffffff
  return min+random%(max-min+1)
}

fun State.rnd(max:Int) = rnd(0,max)
fun State.rndf(min:Float,max:Float) = min+rnd(999)/1000f*(max-min)//todo optimize
fun State.rndf(max:Float = 1f) = rndf(0f,max)
fun State.rndPos() = XY(rndf(width()),rndf(height())).mutable()

fun State.changeSize(delta:Int) {
  val oldW = width()
  val oldH = height()
  size += delta
  val changePosFun:(PosObject)->Unit = {p-> p.pos = p.pos.scale(width()/oldW,height()/oldH)}
  cars.forEach(changePosFun)
  reactive.forEach(changePosFun)
  foods.forEach(changePosFun)
}

@Serializable data class XY(var x:Float=0f,var y:Float=0f) {
  @Transient private var _mutable:Boolean = false

  fun add(a:XY,scale:Float = 1f):XY {
    val result = if(_mutable) this else copy()
    result.x += a.x*scale
    result.y += a.y*scale
    return result
  }

  fun sub(a:XY):XY {//todo operator minus
    val result = if(_mutable) this else copy()
    result.x -= a.x
    result.y -= a.y
    return result
  }

  fun scale(scl:Float):XY {
    return scale(scl,scl)
  }

  fun scale(sx:Float,sy:Float):XY {
    val result = if(_mutable) this else copy()
    result.x *= sx
    result.y *= sy
    return result
  }

  fun dst(xy:XY) = sqrt(((xy.x-x)*(xy.x-x)+(xy.y-y)*(xy.y-y)).toDouble())
  fun len() = dst(XY(0f,0f))

  fun rotate(angleA:Angle):XY {
    val result = if(_mutable) this else copy()
    val angle = calcAngle().add(angleA)
    val len = len()
    result.x = (len*angle.cos()).toFloat()
    result.y = (len*angle.sin()).toFloat()
    return result
  }

  fun calcAngle():Angle {
    return if(true)
      Angle(atan2(y.toDouble(),x.toDouble()).toFloat())
    else
      try {
        Angle(atan((y/x).toDouble()).toFloat()).add(Angle.degreesAngle(if(x<0) 180f else 0f))
      } catch(t:Throwable) {
        Angle.degreesAngle(y.sign*90f)
      }
  }

  fun mutable() = copy().apply {_mutable = true}

}

@Serializable data class PlayerId(var id:Int)