package com.riseofcat.share.mass

import com.riseofcat.lib.*
import kotlinx.serialization.*
import kotlin.math.*

object GameConst {
  val CHANGE_SIZE_PER_CAR=200
  val MIN_CHANGE_SIZE_QUANT = 20
  val UPDATE = Duration(40)
  val UPDATE_S = UPDATE.ms/lib.MILLIS_IN_SECOND
  val MIN_SIZE = 20
  val FOOD_SIZE = 20
  val MIN_RADIUS = 1f
  val FOODS = 150
  val FOOD_PER_CAR = 20
  val BASE_WIDTH = 2000.0
  val BASE_HEIGHT = 2000.0
  val TITLE = "mass-power.io"
  val REACTIVE_LIVE = Tick(60)
}

interface ICommand { fun act(state:State) }
interface PosObject { var pos:XY }
interface SpeedObject:PosObject { var speed:XY }
interface EatMe:SpeedObject { var size:Int }

@Serializable class NewCarCommand(var id:PlayerId):ICommand {
  override fun act(state:State) {
    if(state.cars.none{it.owner == id}) {
      state.cars.add(Car(
        id,
        GameConst.MIN_SIZE*6,
        speed = XY(),
        pos = XY(state.rnd(0, state.width.toInt()).toDouble(), state.rnd(0, state.height.toInt()).toDouble())//todo проверить чтобы равномерно добавлялись новые машины (чтобы не было фантомных появлений из за случайного random-а)
      ))
    }
  }
}
@Serializable class MoveCommand(
  var id:PlayerId,
  val direction:Angle):ICommand {
  override fun act(state:State) {
    val car = state.cars.find{ it.owner == id}?:return
    car.speed = car.speed+direction.xy(100.0)
    val size = car.size/15+1
    if(car.size-size>=GameConst.MIN_SIZE) car.size = car.size-size
    state.reactive.add(Reactive(id,size,(direction+degreesAngle(180)).xy(300.0),car.pos.copy(), state.tick.copy()))
    if(false)repeat(50){//Для тетсирования производительности
      state.reactive.add(Reactive(id,size,(degreesAngle(state.rnd(360))).xy(300.0),car.pos.copy(), state.tick.copy()))}
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
fun Angle.xy(size:Double = 1.0) = XY(cos*size,sin*size)
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
  override var speed:XY,//todo speed and pos change order
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
  val born:Tick):EatMe
@Serializable data class State(
  @Optional val cars:MutableList<Car> = mutableListOf(),
  @Optional val foods:MutableList<Food> = mutableListOf(),
  @Optional val reactive:MutableList<Reactive> = mutableListOf(),
  var random:Int = 0,
  var size:Int = 0,
  var tick:Tick = Tick(0))
fun State.deepCopy() = lib.measure("State.deepCopy") {
  copy(
    cars = cars.map {it.copy()}.toMutableList()
    ,reactive = reactive.map {it.copy()}.toMutableList()
    ,foods = foods.map {it.copy()}.toMutableList()//todo Если не делать такое копирование для food то странно применяются 2-3 действия подряд когда кушаем. Сначала уменьшается резко, потом увеличивается.
  )
}
@Serializable data class PlayerId(var id:Int)
@Serializable data class XY(var x:Double=0.0,var y:Double=0.0) {
  constructor(x:Float,y:Float):this(x.toDouble(), y.toDouble())
}
val EatMe.radius get() = (sqrt(size.toDouble())*5f).toFloat()+GameConst.MIN_RADIUS
fun degreesAngle(degrees:Int) = Angle(degrees/180.0*PI)
infix fun State.act(actions:Iterator<ICommand>):State {
  actions.forEach {it.act(this)}
  return this
}

fun State.tick() = lib.measure("tick") {
  tick+=1
  (cars+reactive).forEach {o->
    o.pos = o.pos msum o.speed*GameConst.UPDATE_S
    o.speed = o.speed mscale 0.98
    if(o.pos.x>=width) o.pos.x = o.pos.x-width
    else if(o.pos.x<0) o.pos.x = o.pos.x+width
    if(o.pos.y>=height) o.pos.y = o.pos.y-height
    else if(o.pos.y<0) o.pos.y = o.pos.y+height
  }
  var reactItr:MutableIterator<Reactive> = reactive.iterator()
  while(reactItr.hasNext()) if(tick-reactItr.next().born > GameConst.REACTIVE_LIVE) reactItr.remove()
  cars.sortBy {it.owner.id}//todo примешивать рандом, основанный на tick. Чтобы в разный момент времени превосходство было у разных игроков
  var handleFoodCars = cars
  while(handleFoodCars.size > 0) {
    val changedSizeCars:MutableSet<Car> = mutableSetOf()
    for(car in handleFoodCars) {//очерёдность съедания вкусняшек важна. Если маленький съел вкусняшку первым, то большой его не съест
      val foodItr = foods.iterator()
      while(foodItr.hasNext()) {
        val (size1,_,pos) = foodItr.next()
        if(distance(car.pos,pos)<=car.radius) {
          car.size += size1
          foodItr.remove()
          changedSizeCars.add(car)
        }
      }
      reactItr = reactive.iterator()
      while(reactItr.hasNext()) {
        val r = reactItr.next()
        if(r.owner!=car.owner&&distance(car.pos,r.pos)<=car.radius) {
          car.size += r.size
          reactItr.remove()
          changedSizeCars.add(car)
        }
      }
    }
    handleFoodCars = changedSizeCars.toMutableList()
  }

  var handleCarsDestroy = true
  while(handleCarsDestroy) {
    handleCarsDestroy = false
    val carItr = cars.iterator()
    val copy = cars.copy()//copy() нужно чтобы не было concurrent modification
    while(carItr.hasNext()) {
      val del = carItr.next()
      for(c in copy) {
        if(del != c && del.size < c.size && distance(c.pos, del.pos)<=c.radius) {
          c.size += del.size
          carItr.remove()
          handleCarsDestroy = true
          break
        }
      }
      if(handleCarsDestroy) copy rm del
    }
  }
  while(foods.size<targetFoods) foods.add(Food(GameConst.FOOD_SIZE,XY(),rndPos()))

  if(tick.tick%5 == 0 && targetSize != size) lib.measure("resize"){
    //todo потестить проивзодительность если x, y будет в относительных координатах и каждый раз высчиываться рендеринг renderX = x*width
    val oldW = width
    val oldH = height
    val delta = targetSize-size
    size = size + delta.sign*min(abs(delta), GameConst.MIN_CHANGE_SIZE_QUANT)
    (cars + reactive + foods).forEach {p -> p.pos = p.pos mscale XY(width/oldW,height/oldH)}
  }
}

val Int.sign get() = this/abs(this)
val State.targetFoods get() = GameConst.FOODS + GameConst.FOOD_PER_CAR*cars.size
val State.targetSize get() = cars.size*GameConst.CHANGE_SIZE_PER_CAR
val widthCache:MutableMap<Int, Double> = mutableMapOf()
val heightCache:MutableMap<Int, Double> = mutableMapOf()
val State.width get() = widthCache.getOrPut(size){kotlin.math.sqrt(GameConst.BASE_WIDTH*GameConst.BASE_WIDTH + size*size)}
val State.height get() = heightCache.getOrPut(size){kotlin.math.sqrt(GameConst.BASE_HEIGHT*GameConst.BASE_HEIGHT + size*size)}
fun State.distance(a:XY,b:XY):Double {
  var dx = min(abs(b.x-a.x),b.x+width-a.x)
  dx = min(dx,a.x+width-b.x)
  var dy = min(abs(b.y-a.y),b.y+height-a.y)
  dy = min(dy,a.y+height-b.y)
  return sqrt(dx*dx+dy*dy)
}
fun State.rnd(min:Int,max:Int):Int {
  random = random*1664525+1013904223 and 0x7fffffff
  return min+random%(max-min+1)
}
fun State.rnd(max:Int) = rnd(0,max)
fun State.rnd(min:Double = 0.0,max:Double = 1.0) = min+rnd(999)/1000f*(max-min)//todo optimize
fun State.rndPos() = XY(rnd(width),rnd(height))
inline operator fun XY.plus(a:XY) = copy(x+a.x,y+a.y)
inline operator fun XY.minus(a:XY) = copy(x-a.x,y-a.y)
internal inline infix fun XY.mscale(xy:XY) = copy()./*todo no copy*/apply {x *= xy.x;y *= xy.y}
internal inline infix fun XY.mscale(scl:Double) = this mscale XY(scl, scl)
internal inline infix fun XY.scale(xy:XY) = copy() mscale xy
internal inline infix fun XY.scale(scl:Double) = copy() mscale scl
internal inline infix fun XY.msum(b:XY) = copy()./*todo no copy*/apply {x += b.x; y += b.y}
internal inline infix fun XY.sum(b:XY) = copy() msum b
operator fun XY.times(scl:Double) = this scale scl
val XY.len get() = dst(XY(0.0,0.0))
fun XY.dst(xy:XY) = sqrt(((xy.x-x)*(xy.x-x)+(xy.y-y)*(xy.y-y)))
fun XY.calcAngle():Angle = Angle(atan2(y,x))