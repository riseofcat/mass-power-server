package com.riseofcat.share.mass

import com.riseofcat.client.*
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
  val REACTIVE_LIVE = 60*10
}

interface InStateAction { fun act(state:State) }
interface PosObject { var pos:XY }
interface SpeedObject:PosObject { var speed:XY }
interface EatMe:SpeedObject { var size:Int }
@Serializable class BigAction(
  @Optional val n:NewCarAction? = null,
  @Optional val p:PlayerAction? = null
):InStateAction {
  override fun act(state:State) {
    n?.act(state)
    p?.act(state)
  }
}
@Serializable class NewCarAction(var id:PlayerId):InStateAction {
  override fun act(state:State) {
    state.changeSize(100)
    state.cars.add(Car(id,GameConst.MIN_SIZE*6,XY(),XY()))
  }
}
@Serializable class PlayerAction(
  var id:PlayerId,
  var action:Action):InStateAction {
  override fun act(state:State) {
    val car = state.cars.find{ it.owner == id}?:return
    car.speed = car.speed+action.direction.xy*100f
    val s = car.size/15+1
    if(car.size-s>=GameConst.MIN_SIZE) car.size = car.size-s
    state.reactive.add(Reactive(id,s,(action.direction+degreesAngle(180f)).xy*300f,car.pos.copy()))
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
}
@Serializable data class Car(
  var owner:PlayerId,
  override var size:Int,
  override var speed:XY,
  override var pos:XY):EatMe
@Serializable data class Food(
  override var size:Int,
  override var speed:XY,
  override var pos:XY):EatMe
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
  var size:Int = 0)
@Serializable data class PlayerId(var id:Int)
@Serializable data class XY(var x:Float=0f,var y:Float=0f)

val EatMe.radius get() = (kotlin.math.sqrt(size.toDouble())*5f).toFloat()+GameConst.MIN_RADIUS
fun degreesAngle(degrees:Float) = Angle(degrees/180*PI.toFloat())
fun degreesAngle(degrees:Int) = Angle(degrees/180*PI.toFloat())
operator fun Angle.plus(deltaAngle:Angle) = Angle(this.radians+deltaAngle.radians)
operator fun Angle.minus(sub:Angle) = Angle(this.radians-sub.radians)
fun Angle.sin() = kotlin.math.sin(radians.toDouble()).toFloat()
fun Angle.cos() = kotlin.math.cos(radians.toDouble()).toFloat()
val Angle.xy get() = XY(cos(),sin())
val Angle.degrees:Float get() = (radians*180/kotlin.math.PI).toFloat()
val Angle.gdxTransformRotation:Float get() = degrees
fun NewCarAction.toBig() = BigAction(n = this)
fun PlayerAction.toBig() = BigAction(p = this)
fun State.act(actions:Iterator<InStateAction>):State {
  actions.forEach {it.act(this)}
  return this
}

fun State.tick() = apply {
  (cars+reactive).forEach {o->
    o.pos msum o.speed*GameConst.UPDATE_S
    o.speed mscale 0.98f

    if(o.pos.x>=width) o.pos.x = o.pos.x-width
    else if(o.pos.x<0) o.pos.x = o.pos.x+width
    if(o.pos.y>=height) o.pos.y = o.pos.y-height
    else if(o.pos.y<0) o.pos.y = o.pos.y+height
  }
  var reactItr:MutableIterator<Reactive> = reactive.iterator()
  while(reactItr.hasNext()) if(reactItr.next().ticks++>GameConst.REACTIVE_LIVE) reactItr.remove()
  for(car in cars) {
    val foodItr = foods.iterator()
    while(foodItr.hasNext()) {
      val (size1,_,pos) = foodItr.next()
      if(distance(car.pos,pos)<=car.radius) {
        car.size += size1
        foodItr.remove()
      }
    }
    reactItr = reactive.iterator()
    while(reactItr.hasNext()) {
      val r = reactItr.next()
      if(r.owner!=car.owner&&distance(car.pos,r.pos)<=car.radius) {
        car.size += r.size
        reactItr.remove()
      }
    }
  }
  if(foods.size<GameConst.FOODS) foods.add(Food(GameConst.FOOD_SIZE,XY(),rndPos()))
}

val State.width get() = (GameConst.BASE_WIDTH+size).toFloat()
val State.height get() = (GameConst.BASE_HEIGHT+size).toFloat()
fun State.distance(a:XY,b:XY):Float {
  var dx = kotlin.math.min(kotlin.math.abs(b.x-a.x),b.x+width-a.x)
  dx = kotlin.math.min(dx,a.x+width-b.x)
  var dy = kotlin.math.min(kotlin.math.abs(b.y-a.y),b.y+height-a.y)
  dy = kotlin.math.min(dy,a.y+height-b.y)
  return kotlin.math.sqrt((dx*dx+dy*dy).toDouble()).toFloat()
}
fun State.rnd(min:Int,max:Int):Int {
  random = random*1664525+1013904223 and 0x7fffffff
  return min+random%(max-min+1)
}
fun State.rnd(max:Int) = rnd(0,max)
fun State.rndf(min:Float,max:Float) = min+rnd(999)/1000f*(max-min)//todo optimize
fun State.rndf(max:Float = 1f) = rndf(0f,max)
fun State.rndPos() = XY(rndf(width),rndf(height))
fun State.changeSize(delta:Int) {
  val oldW = width
  val oldH = height
  size += delta
  (cars + reactive + foods).forEach {p -> p.pos mscale XY(width/oldW,height/oldH)}
}

inline operator fun XY.plus(a:XY) = copy(x+a.x,y+a.y)
inline operator fun XY.minus(a:XY) = copy(x-a.x,y-a.y)
internal inline infix fun XY.scale(xy:XY) = copy().also {it mscale xy}
internal inline infix fun XY.mscale(scl:Float) = this mscale XY(scl, scl)
internal inline infix fun XY.scale(scl:Float) = copy().also {it mscale scl}
internal inline infix fun XY.msum(b:XY):XY {
  x += b.x
  y += b.y
  return this
}
internal inline infix fun XY.sum(b:XY) = copy() msum b
internal inline infix fun XY.mscale(xy:XY) {
  x*=xy.x
  y*=xy.y
}
fun XY.rotate(angleA:Angle):XY {
  val result = copy()
  val angle = calcAngle() + angleA
  val len = len()
  result.x = (len*angle.cos()).toFloat()
  result.y = (len*angle.sin()).toFloat()
  return result
}
operator fun XY.times(scl:Float) = this scale scl
fun XY.len() = dst(XY(0f,0f))//todo get() =
fun XY.dst(xy:XY) = sqrt(((xy.x-x)*(xy.x-x)+(xy.y-y)*(xy.y-y)).toDouble())
fun XY.calcAngle():Angle = Angle(atan2(y.toDouble(),x.toDouble()).toFloat())