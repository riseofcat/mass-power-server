package com.n8cats.share.data

import com.n8cats.lib_gwt.*
import com.n8cats.share.*

val BASE_WIDTH = 1000
val BASE_HEIGHT = 1000

interface PosObject {
  var pos:XY
}
interface SpeedObject:PosObject {
  var speed:XY
}
interface EatMe:SpeedObject {
  var size:Int
  fun radius() = (Math.sqrt(size.toDouble())*5f).toFloat()+Logic.MIN_RADIUS
}

data class Action (var direction:Angle)
data class Angle(var radians:Float) {
  init {
    val circles = (radians/(2*Math.PI)).toInt()
    if(Math.abs(circles)>0) {
      val a = 1+1//todo breakpoint
    }
    //	radians -= circles * 2 * Math.PI;
    //	if(radians < 0) {
    //		radians += 2 * Math.PI;
    //	}
  }
  val degrees:Float get() = (radians*180/Math.PI).toFloat()
  val gdxTransformRotation:Float get() = degrees
  fun sin() = Math.sin(radians.toDouble()).toFloat()
  fun cos() = Math.cos(radians.toDouble()).toFloat()
  fun xy():XY = XY(cos(),sin())
  fun add(deltaAngle:Angle) = Angle(this.radians+deltaAngle.radians)
  fun subtract(sub:Angle) = Angle(this.radians-sub.radians)
  companion object {
    @JvmStatic fun degreesAngle(degrees:Float) = Angle(degrees/180*Math.PI.toFloat())
  }
}

data class Car(var owner:PlayerId, override var size:Int,override var speed:XY,override var pos:XY):EatMe
//		size = FOOD_SIZE;//todo fixed size
data class Food(
  override var size:Int,
  override var speed:XY,
  override var pos:XY):EatMe

data class NewCarAction(var id:PlayerId):Logic.InStateAction {
  override fun act(state:State,getCar:Logic.GetCarById) {
    state.changeSize(100)
    state.cars.add(Car(id,Logic.MIN_SIZE*6,XY(true),XY()))
  }
  fun toBig() = BigAction().also {it.n = this}
}

open class PlayerAction(var id:PlayerId,var action:Action):Logic.InStateAction {
  override fun act(state:State,getCar:Logic.GetCarById) {
    val scl = 100f
    val car = getCar.getCar(id) ?: return //todo handle null ?
    car.speed = car.speed.add(action.direction.xy(),scl)
    val s = car.size/15+1
    if(car.size-s>=Logic.MIN_SIZE) car.size = car.size-s
    state.reactive.add(Reactive(id,s,XY(action.direction.add(Angle.degreesAngle(180f)).xy().scale(3f*scl),true),XY(car.pos,true)))
  }
  fun toBig() = BigAction().also{it.p=this}
}

data class Reactive(
  var owner:PlayerId,
  override var size:Int,
  override var speed:XY,
  override var pos:XY,
  var ticks:Int = 0):EatMe

data class State(
  var cars:MutableList<Car> = mutableListOf(),
  var foods:MutableList<Food> = mutableListOf(),
  var reactive:MutableList<Reactive> = mutableListOf(),
  var random:Int = 0,
  var size:Int = 0) {
  fun width() = (BASE_WIDTH+size).toFloat()
  fun height() = (BASE_HEIGHT+size).toFloat()
  fun act(iterator:Iterator<Logic.InStateAction>):State {
    class Cache:Logic.GetCarById {
      override fun getCar(id:PlayerId):Car? {
        for(car in cars) if(id==car.owner) return car
        return null
      }
    }
    val cache = Cache()
    while(iterator.hasNext()) {
      val p = iterator.next()
      p.act(this,cache)
    }
    return this
  }
  private fun distance(a:XY,b:XY):Float {
    var dx = Math.min(Math.abs(b.x-a.x),b.x+width()-a.x)
    dx = Math.min(dx,a.x+width()-b.x)
    var dy = Math.min(Math.abs(b.y-a.y),b.y+height()-a.y)
    dy = Math.min(dy,a.y+height()-b.y)
    return Math.sqrt((dx*dx+dy*dy).toDouble()).toFloat()
  }
  fun tick():State {
    val iterator = CompositeIterator<SpeedObject>(cars,reactive)
    while(iterator.hasNext()) {
      val o = iterator.next()
      o!!.pos = o.pos.add(XY(o.speed,true),Logic.UPDATE_S)
      if(o.pos.x>=width())
        o.pos.x = o.pos.x-width()
      else if(o.pos.x<0) o.pos.x = o.pos.x+width()
      if(o.pos.y>=height())
        o.pos.y = o.pos.y-height()
      else if(o.pos.y<0) o.pos.y = o.pos.y+height()
      o.speed = o.speed.scale(0.98f)
    }
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
    if(foods.size<Logic.FOODS) foods.add(Food(Logic.FOOD_SIZE,XY(),rndPos()))
    return this
  }

  private fun rnd(min:Int,max:Int):Int {
    random = random*1664525+1013904223 and 0x7fffffff
    return min+random%(max-min+1)
  }

  private fun rnd(max:Int) = rnd(0,max)
  private fun rndf(min:Float,max:Float) = min+rnd(999)/1000f*(max-min)//todo optimize
  private fun rndf(max:Float = 1f) = rndf(0f,max)
  private fun rndPos() = XY(rndf(width()),rndf(height()),true)

  fun changeSize(delta:Int) {
    val oldW = width()
    val oldH = height()
    size += delta
    val itr = CompositeIterator(cars,reactive,foods)
    while(itr.hasNext()) {
      val p = itr.next()
      p!!.pos = p.pos.scale(width()/oldW,height()/oldH)
    }
  }

  fun copy2():State {
    return this.copy();
  }
}

data class XY(var x:Float,var y:Float) {
  private var mutable:Boolean = false
  constructor(x:Float,y:Float,mutable:Boolean):this(x,y) {
    this.mutable = mutable
  }
  constructor(xy:XY, mutable:Boolean):this(xy.x, xy.y) {
    this.mutable = mutable
  }
  constructor(mutable:Boolean):this(0f, 0f) {
    this.mutable = mutable
  }
  constructor():this(0f,0f)

  fun add(a:XY,scale:Float = 1f):XY {
    val result = if(mutable) this else copy()
    result.x += a.x*scale
    result.y += a.y*scale
    return result
  }

  fun sub(a:XY):XY {
    val result = if(mutable) this else copy()
    result.x -= a.x
    result.y -= a.y
    return result
  }

  fun scale(scl:Float):XY {
    return scale(scl,scl)
  }

  fun scale(sx:Float,sy:Float):XY {
    val result = if(mutable) this else copy()
    result.x *= sx
    result.y *= sy
    return result
  }

  fun dst(xy:XY) = Math.sqrt(((xy.x-x)*(xy.x-x)+(xy.y-y)*(xy.y-y)).toDouble())
  fun len() = dst(XY(0f,0f))

  fun rotate(angleA:Angle):XY {
    val result = if(mutable) this else copy()
    val angle = calcAngle().add(angleA)
    val len = len()
    result.x = (len*angle.cos()).toFloat()
    result.y = (len*angle.sin()).toFloat()
    return result
  }

  fun calcAngle():Angle {
    return if(true)
      Angle(Math.atan2(y.toDouble(),x.toDouble()).toFloat())
    else
      try {
        Angle(Math.atan((y/x).toDouble()).toFloat()).add(Angle.degreesAngle(if(x<0) 180f else 0f))
      } catch(t:Throwable) {
        Angle.degreesAngle(LibAllGwt.Fun.sign(y)*90f)
      }

  }
}