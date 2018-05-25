package com.riseofcat.share.mass

import com.riseofcat.lib.*
import kotlinx.serialization.*
import kotlin.math.*
const val PERFORMANCE_KOEFF = 1
inline fun State.repeatTick(ticks:Int, lambda:()->Unit) {
  repeatTickCalls++
  if((tick.tick-repeatTickCalls)%ticks==0) lambda()
}

object GameConst {
  val UPDATE = Duration(16)
  val UPDATE_S = UPDATE.ms/lib.MILLIS_IN_SECOND
  const val MIN_SIZE = 20
  const val DEFAULT_CAR_SIZE = MIN_SIZE*6
  const val FOOD_SIZE = 20
  const val MIN_RADIUS = 1f
  const val TITLE = "mass-power.io"
  val REACTIVE_LIVE = Tick(Duration(2500)/UPDATE)
  const val FRICTION:Double = 0.01
  const val BASE_SIZE = 3_000
}

interface ICommand { fun act(state:State) }
interface PosObject { var pos:SXY }
interface SpeedObject:PosObject { var speed:SXY }
interface SizeObject:PosObject { val size:Int }
interface MovedObject:SizeObject, SpeedObject

@Serializable class NewCarCommand(var id:PlayerId):ICommand {
  override fun act(state:State) {
    if(state.cars.none{it.owner == id}) {
      val size = if(state.cars.size > 0) kotlin.math.max((state.cars.sumBy {it.size}/state.cars.size*0.7).toInt(), GameConst.MIN_SIZE) else GameConst.DEFAULT_CAR_SIZE
      state.cars.add(Car(id,size = size,speed = SXY(),pos = state.random2.randomPos()))
    }
  }
}
@Serializable class MoveCommand(
  var id:PlayerId,
  val direction:Angle):ICommand {
  override fun act(state:State) {
    fun Angle.xy(size:Double) = SXY((cos*size).toShort(),(sin*size).toShort())
    val car = state.cars.find{ it.owner == id}?:return
    car.speed = car.speed+direction.xy(Short.MAX_VALUE/100.0)
    val size = car.size/20+1
    if(car.size-size>=GameConst.MIN_SIZE) car.size = car.size-size
    state.reactive.add(Reactive(id,size,car.speed + (direction+degreesAngle(180)).xy(400.0),car.pos.copy(),state.tick.copy()))
  }
}
@Serializable data class Angle(var radians:Double) {
  init { fix() }
  fun fix() {
    if(abs(degrees) > 360) degrees %= 360
    if(degrees < 0) degrees+=360
  }
}
operator fun Angle.plus(deltaAngle:Angle) = Angle(this.radians+deltaAngle.radians)
operator fun Angle.minus(sub:Angle) = Angle(this.radians-sub.radians)
var Angle.degrees
  get() = radians*180/PI
  set(value) {
    radians = value * PI/ 180
  }
inline val Angle.sin get() = sin(radians)
inline val Angle.cos get() = cos(radians)

@Serializable data class Car(
  var owner:PlayerId,
  override var size:Int,
  override var speed:SXY,//todo speed and pos change order
  override var pos:SXY):MovedObject
@Serializable data class Food(
  override var pos:SXY
):SizeObject {
  override val size:Int = GameConst.FOOD_SIZE
}
@Serializable data class Reactive(
  var owner:PlayerId,
  override var size:Int,
  override var speed:SXY,
  override var pos:SXY,
  val born:Tick):MovedObject

@Serializable data class State(
  @Optional val cars:MutableList<Car> = mutableListOf()
  ,@Optional val foods:MutableList<Food> = mutableListOf()
  ,@Optional val reactive:MutableList<Reactive> = mutableListOf()
  ,var random:Random = Random()
  ,var random2:Random = Random()
  ,var size:Int = GameConst.BASE_SIZE
  ,var tick:Tick = Tick(0)
  ,@Transient var repeatTickCalls:Int = 0
) {
  val semiWidth get() = width
  val Float.short get() = (this/semiWidth*(1 shl 16)).toShort()
  val Double.int get() = (this/semiWidth*(1 shl 16)).toInt()
  val Float.byte get() = (this/semiWidth*(1 shl 8)).toByte()
  val Double.short get() = (this/semiWidth*(1 shl 16)).toShort()
  val Double.byte get() = (this/semiWidth*(1 shl 8)).toByte()
  val Short.real get() = this*semiWidth/(1 shl 16)
  val Int.realLikeShortResult get() = this*semiWidth/(1 shl 16)
  val Float.realLikeShortResult get() = this*semiWidth/(1 shl 16)
  val Byte.real get() = this*semiWidth/(1 shl 8)

  fun floatToShort(f:Float) = f.short
  fun floatToShortInt(f:Double) = f.int
  fun doubleToShort(d:Double) = d.toFloat().short
  fun shortToReal(s:Short) = s.real
  fun shortToReal(s:Int) = s.realLikeShortResult
  fun realXY(pos:SXY) = XY(pos.x.real, pos.y.real)
  fun realToShort(pos:XY) = SXY(pos.x.short, pos.y.short)

  val SizeObject.shortRadius get() = size.radius.short
  val SizeObject.shortDiameter get() = (size.radius.short*2).toShort()
}
val SizeObject.radius get() = size.radius
fun State.getCar(id:PlayerId) = cars.firstOrNull {it.owner==id}
fun State.deepCopy() = lib.measure("State.deepCopy") {
  copy(
    cars = cars.map {it.copy()}.toMutableList()
    ,reactive = reactive.map {it.copy()}.toMutableList()
    ,foods = foods.map{it.copy()}.toMutableList()
    ,random = random.copy()
    ,random2 = random2.copy()
  )
}
@Serializable data class Random(var seed:Int=0)
@Serializable data class PlayerId(var id:Int)
@Serializable data class SXY(var x:Short=0,var y:Short=0) {
  constructor(x:Int, y:Int):this(x.toShort(), y.toShort())
}
data class XY(var x:Double=0.0, var y:Double=0.0)
val Int.radius get():Float = GameConst.MIN_RADIUS + 5*sqrt(this.toDouble()).toFloat()
fun degreesAngle(degrees:Int) = Angle(degrees/180.0*PI)
infix fun State.act(actions:Iterator<ICommand>):State {
  actions.forEach {it.act(this)}
  return this
}

val Short.leftByte get() = (this/256).toByte()
val Short.rightByte get() = (this%256).toByte()

fun State.tick() = lib.measure("TICK") {
  repeatTickCalls = 0
  tick+=1
  reactive.removeAll {tick-it.born>GameConst.REACTIVE_LIVE}
  (cars+reactive).forEach {o->
    o.pos = o.pos msum o.speed*(GameConst.UPDATE_S*15_000/size)
    o.speed = o.speed * (1.0 - GameConst.FRICTION)//todo mutable?
  }

  lib.skip_measure("tick.sortCars") {
    cars.sortBy {it.owner.id}//todo примешивать рандом, основанный на tick. Чтобы в разный момент времени превосходство было у разных игроков
  }

  infix fun SizeObject.overlap(xy:SXY) = distance(this.pos, xy) <= this.radius

  repeatTick(1) {
    lib.measure("tick.eatFoods and reactive") {
      var handleFoodCars = cars
      while(handleFoodCars.size > 0) {
        val changedSizeCars:MutableSet<Car> = mutableSetOf()
        for(car in handleFoodCars) {//очерёдность съедания вкусняшек важна. Если маленький съел вкусняшку первым, то большой его не съест
          val foodItr = foods.iterator()
          while(foodItr.hasNext()) {
            val f = foodItr.next()
            if(car overlap f.pos) {
              foodItr.remove()
              car.size += f.size
              changedSizeCars.add(car)
            }
          }

          val reactItr = reactive.iterator()
          while(reactItr.hasNext()) {
            val r = reactItr.next()
            if(r.owner!=car.owner) if(car overlap r.pos) {
              reactItr.remove()
              car.size += r.size
              changedSizeCars.add(car)
            }
          }

        }
        handleFoodCars = changedSizeCars.toMutableList()
      }
    }
  }

  repeatTick(1) {
    lib.measure("tick.eatCars") {
      var handleCarsDestroy = true
      while(handleCarsDestroy) {
        handleCarsDestroy = false
        val carItr = cars.iterator()
        val copy = cars.toTypedArray()//copy нужно чтобы не было concurrent modification
        while(carItr.hasNext()) {
          val del = carItr.next()
          for(c in copy) {
            if(del != c) if(del.size < c.size) if(distance(c.pos, del.pos)<=c.radius) if(cars.contains(c)) {
              c.size += del.size
              carItr.remove()
              handleCarsDestroy = true
              break
            }
          }
        }
      }
    }
  }

  repeatTick(1) {
    while(foods.size < targetFoods) {
      foods.add(Food(random.randomPos()))
    }
  }

  repeatTick(4) {
    val delta = targetSize-size
    if(delta != 0) {
      val MAX_SIZE_DELTA = 10*PERFORMANCE_KOEFF*PERFORMANCE_KOEFF*PERFORMANCE_KOEFF
      val smallDelta = if(delta.absoluteValue > MAX_SIZE_DELTA) {
        (delta*lib.Fun.arg0toInf(abs(delta),50)).toInt()
          .let {it.sign*min(abs(it),MAX_SIZE_DELTA)}
      } else {
        delta
      }
      size += smallDelta
    }
  }
}

val State.targetFoods get() = width*height/100_000*PERFORMANCE_KOEFF
val State.targetSize get():Int = PERFORMANCE_KOEFF*100 + kotlin.math.sqrt(cars.sumBy {it.size}.toDouble() * 7000 ).toInt()
inline val State.width get() = size.toDouble()
inline val State.height get() = size
const val relativeWidth = Short.MAX_VALUE-Short.MIN_VALUE//2^16-1
fun sabs(a:Int) = abs(a.toShort().toInt())
inline fun dx(a:SXY,b:SXY) = min(sabs(b.x-a.x),sabs(b.x-a.x+relativeWidth))
inline fun dy(a:SXY,b:SXY) = min(sabs(b.y-a.y),sabs(b.y-a.y+relativeWidth))
fun State.distance(a:SXY,b:SXY):Double {
  val dx = dx(a,b)
  val dy = dy(a,b)
  val sqrt = 2*sqrt((dx*dx/4+dy*dy/4).toFloat())//todo можно закэшировать
  return sqrt.realLikeShortResult
}
inline operator fun SXY.plus(a:SXY) = SXY(x+a.x,y+a.y)
inline operator fun SXY.minus(a:SXY) = SXY(x-a.x,y-a.y)
inline operator fun XY.plus(a:XY) = XY(x+a.x,y+a.y)
inline operator fun XY.minus(a:XY) = XY(x-a.x,y-a.y)
internal inline infix fun SXY.msum(b:SXY) = copy()./*todo no copy*/apply {x = (x+ b.x).toShort(); y = (y + b.y).toShort()}
internal inline infix fun SXY.sum(b:SXY) = copy() msum b
internal inline infix fun SXY.mtimes(scl:Double) = copy()./*todo no copy*/apply {x = (x*scl).toShort(); y = (y*scl).toShort()}
operator fun SXY.times(scl:Double) = copy() mtimes scl

internal inline infix fun XY.mtimes(scl:Double) = copy()./*todo no copy*/apply {x = x*scl; y = y*scl}
operator fun XY.times(scl:Double) = copy() mtimes scl

private inline fun Random._rnd(min:Int, max:Int):Int {
  seed = seed*1664525+1013904223 and 0x7fffffff
  return min+seed%(max-min+1)
}
fun Random.rnd(min:Short, max:Short) = rnd(min.toInt(), max.toInt())
fun Random.rnd(min:Int, max:Int):Int {
  val сдвиг = 256*256
  val diapasone = max-min
  return min + diapasone/сдвиг*_rnd(0,сдвиг) + diapasone%сдвиг*_rnd(0,сдвиг)/сдвиг
}
fun Random.randomPos() = SXY(rnd(Short.MIN_VALUE, Short.MAX_VALUE), rnd(Short.MIN_VALUE, Short.MAX_VALUE))