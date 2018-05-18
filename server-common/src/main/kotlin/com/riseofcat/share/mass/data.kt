package com.riseofcat.share.mass

import com.riseofcat.lib.*
import kotlinx.serialization.*
import kotlin.math.*

fun mod(value:Int,module:Int) = when {//todo удалить если не будут вылитать ошибки
  value<0->{
    lib.log.error("!!! value<0 !!!")
    value+module
  }
  value>=module->{
    lib.log.error("!!! value>=module !!!")
    value-module
  }
  else->{
    value
  }
}
class Mattr2D(val COLS:Int, val ROWS:Int){
  val all = (0 until COLS*ROWS).map{Cell(it/ROWS,it%ROWS)}
  val map:Map<Int,Map<Int,Cell>>
  init{
    map = mutableMapOf()
    for(col in 0 until COLS) {
      val m2 = mutableMapOf<Int,Cell>()
      map[col] = m2
      for(row in 0 until ROWS) m2[row] = if(true) all[col*ROWS+row] else Cell(col,row)
    }
  }
  inner class Cell(val col:Int, val row:Int) {
    val matrix = this@Mattr2D
    val food:MutableList<Food> = mutableListOf()
    val reactive:MutableList<Reactive> = mutableListOf()
  }
  inline operator fun get(col:Int,row:Int) = if(false) all[col*ROWS+row] else map[col]!![row]!!
}
fun Mattr2D.clearFood() = all.forEach {it.food.clear()}
fun Mattr2D.clearReactive() = all.forEach {it.reactive.clear()}
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
interface PosObject { var pos:XY }
interface SpeedObject:PosObject { var speed:XY }
interface SizeObject:PosObject { val size:Int }
interface EatMeWithSpeed:SizeObject, SpeedObject

@Serializable class NewCarCommand(var id:PlayerId):ICommand {
  override fun act(state:State) {
    if(state.cars.none{it.owner == id}) {
      val size = if(state.cars.size > 0) kotlin.math.max((state.cars.sumBy {it.size}/state.cars.size*0.7).toInt(), GameConst.MIN_SIZE) else GameConst.DEFAULT_CAR_SIZE
      state.cars.add(Car(id,size = size,speed = XY(),pos = state.random2.randomPos(state)))
    }
  }
}
@Serializable class MoveCommand(
  var id:PlayerId,
  val direction:Angle):ICommand {
  override fun act(state:State) {
    val car = state.cars.find{ it.owner == id}?:return
    car.speed = car.speed+direction.xy(100.0)
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
  override var pos:XY):EatMeWithSpeed
@Serializable data class Food(
  override var pos:XY):SizeObject,PosObject {
  override val size get() = GameConst.FOOD_SIZE
}
@Serializable data class Reactive(
  var owner:PlayerId,
  override var size:Int,
  override var speed:XY,
  override var pos:XY,
  val born:Tick):EatMeWithSpeed
@Serializable data class State(
  @Optional val cars:MutableList<Car> = mutableListOf()
  ,@Optional val foods:MutableList<Food> = mutableListOf()
  ,@Optional val reactive:MutableList<Reactive> = mutableListOf()
  ,var random:Random = Random()
  ,var random2:Random = Random()
  ,var size:Int = GameConst.BASE_SIZE
  ,var tick:Tick = Tick(0)
  ,@Transient var repeatTickCalls:Int = 0
  ,@Transient val cache:Mattr2D = Mattr2D(5,5)
)
fun State.getCar(id:PlayerId) = cars.firstOrNull {it.owner==id}
fun State.deepCopy() = lib.measure("State.deepCopy") {
  copy(
    cars = cars.map {it.copy()}.toMutableList()
    ,reactive = reactive.map {it.copy()}.toMutableList()
    ,foods = foods.map {it.copy()}.toMutableList()//todo Если не делать такое копирование для food то странно применяются 2-3 действия подряд когда кушаем. Сначала уменьшается резко, потом увеличивается.
    ,cache = cache
    ,random = random.copy()
    ,random2 = random2.copy()
  )
}
@Serializable data class Random(var seed:Int=0)
@Serializable data class PlayerId(var id:Int)
@Serializable data class XY(var x:Double=0.0,var y:Double=0.0) {//todo Int ?
  constructor(x:Float,y:Float):this(x.toDouble(), y.toDouble())
  constructor(x:Int,y:Int):this(x.toDouble(), y.toDouble())
}
val SizeObject.radius get() = size.radius
val Int.radius get():Float = GameConst.MIN_RADIUS + 5*sqrt(this.toDouble()).toFloat()
fun degreesAngle(degrees:Int) = Angle(degrees/180.0*PI)
infix fun State.act(actions:Iterator<ICommand>):State {
  actions.forEach {it.act(this)}
  return this
}
data class Rect(val pos:XY, val size:XY) {
  val topLeft = pos
  val topRight = pos + size.scale(XY(1.0,0.0))
  val bottomLeft = pos + size.scale(XY(0.0,1.0))
  val bottomRight = pos + size.scale(XY(1.0,1.0))
  val points = arrayOf(topLeft, topRight, bottomLeft, bottomRight)
}

class Bucket(val col:Int,val row:Int) {
  val foods:MutableList<Food> = mutableListOf()
  val cars:MutableList<Car> = mutableListOf()
  val reactive:MutableList<Food> = mutableListOf()
  override fun toString() = "[$col, $row]"
}
fun State.tick() = lib.measure("TICK") {
  repeatTickCalls = 0
  tick+=1
  lib.skip_measure("tick.move") {
    (cars+reactive).forEach {o->
      o.pos = o.pos msum o.speed*GameConst.UPDATE_S
      o.speed = o.speed mscale (1.0 - GameConst.FRICTION)
      if(o.pos.x>=width) o.pos.x = o.pos.x-width
      else if(o.pos.x<0) o.pos.x = o.pos.x+width
      if(o.pos.y>=height) o.pos.y = o.pos.y-height
      else if(o.pos.y<0) o.pos.y = o.pos.y+height
    }
  }

  var reactItr:MutableIterator<Reactive> = reactive.iterator()
  lib.skip_measure("tick.reactiveLife") {
    while(reactItr.hasNext()) if(tick-reactItr.next().born > GameConst.REACTIVE_LIVE) reactItr.remove()
  }

  lib.skip_measure("tick.sortCars") {
    cars.sortBy {it.owner.id}//todo примешивать рандом, основанный на tick. Чтобы в разный момент времени превосходство было у разных игроков
  }

  fun Rect.containsPoint(p:XY) = p.x>=topLeft.x&&p.y>=topLeft.y&&p.x<=bottomRight.x&&p.y<=bottomRight.y
  fun Rect.anyAlternative(lambda:Rect.()->Boolean):Boolean {
    if(lambda(this)) return true
    val result = mutableListOf<Rect>()
    result.add(this)

    if(points.any {it.x>=width}) this.copy(pos = pos.copy(x = pos.x-width))
      .also{if(lambda(it)) return true}
      .also {result.add(it)}
    else if(points.any {it.x<0}) this.copy(pos = pos.copy(x = pos.x+width))
      .also{if(lambda(it)) return true}
      .also {result.add(it)}

    if(points.any {it.y>=height}) {
      result.copy().forEach {
        if(lambda(it.copy(pos = it.pos.copy(y = it.pos.y-height)))) return true
      }
    } else if(points.any {it.y<0}) {
      result.copy().forEach {
        if(lambda(it.copy(pos = it.pos.copy(y = it.pos.y+height)))) return true
       }
    }
    return false
  }

  fun SizeObject.toRect() = Rect(pos-XY(radius,radius),XY(2*radius,2*radius))
  fun Rect.alternativesIsOverlapRect(rect:Rect) = anyAlternative{points.any{rect.containsPoint(it)} || rect.points.any{containsPoint(it)}}
  val w = width.toFloat()/cache.COLS
  val h = height.toFloat()/cache.ROWS
  infix fun SizeObject.overlap(xy:XY) = distance(this.pos, xy) <= this.radius
  fun Rect.overlapCell(matrix:Mattr2D) = matrix.all.filter {
    alternativesIsOverlapRect(Rect(XY(it.col*width/matrix.COLS,it.row*height/matrix.ROWS),XY(width/matrix.COLS,height/matrix.ROWS)))
  }
  fun PosObject.storeCol() = mod((pos.x/w).toInt(),cache.COLS)
  fun PosObject.storeRow() = mod((pos.y/h).toInt(),cache.ROWS)

  repeatTick(40) {//todo выполнять сразу если кэш пустой
    lib.skip_measure("tick.sortBuckets") {
      cache.clearFood()
      foods.forEach {cache.get(it.storeCol(), it.storeRow()).food.add(it)}
    }
  }
  repeatTick(10) {
    cache.clearReactive()
    reactive.forEach {cache.get(it.storeCol(), it.storeRow()).reactive.add(it)}
  }
  repeatTick(10) {
    lib.measure("tick.eatFoods") {
      var handleFoodCars = cars
      while(handleFoodCars.size > 0) {
        val changedSizeCars:MutableSet<Car> = mutableSetOf()
        for(car in handleFoodCars) {//очерёдность съедания вкусняшек важна. Если маленький съел вкусняшку первым, то большой его не съест
          val carRect = car.toRect()
          carRect.overlapCell(cache).forEach {cell->

            val foodItr = cell.food.iterator()
            while(foodItr.hasNext()) {
              val f = foodItr.next()
              if(car overlap f.pos) {
                if(foods.remove(f)) {
                  car.size += f.size
                  changedSizeCars.add(car)
                }
              }
            }

            reactItr = cell.reactive.iterator()
            while(reactItr.hasNext()) {
              val r = reactItr.next()
              if(r.owner!=car.owner) if(car overlap r.pos)
                if(reactive.remove(r)) {
                  car.size += r.size
                  changedSizeCars.add(car)
                }
            }

          }
        }
        handleFoodCars = changedSizeCars.toMutableList()
      }
    }
  }

  repeatTick(4) {
    lib.measure("tick.eatCars") {
      var handleCarsDestroy = true
      while(handleCarsDestroy) {
        handleCarsDestroy = false
        val carItr = cars.iterator()
        val copy = cars.toTypedArray()//copy нужно чтобы не было concurrent modification
        while(carItr.hasNext()) {
          val del = carItr.next()
//          del.overlapCell(cacheCars).forEach {cell->
            for(c in copy) {//c in cell.value
              if(del != c) if(del.size < c.size) if(distance(c.pos, del.pos)<=c.radius) if(cars.contains(c)) {
                c.size += del.size
                carItr.remove()
                handleCarsDestroy = true
                break
              }
            }
//          }
        }
      }
    }
  }

  repeatTick(20) {
    while(foods.size<targetFoods) foods.add(Food(random.randomPos(this)))
  }

  repeatTick(10) {
    lib.measure("tick.change size") {
      val delta = targetSize-size
      if(delta != 0) {
        val oldW = width
        val oldH = height
        val MAX_SIZE_DELTA = 30
        val smallDelta = if(delta.absoluteValue > MAX_SIZE_DELTA) {
          (delta*lib.Fun.arg0toInf(abs(delta),50)).toInt()
            .let {it.sign*min(abs(it),MAX_SIZE_DELTA)}
        } else {
          delta
        }
        size += smallDelta
        val widthD = width.toDouble()
        val heightD = height.toDouble()
        (cars+reactive+foods).forEach {p-> p.pos = p.pos mscale XY(widthD/oldW,heightD/oldH)}
      }
    }
  }
}

val Int.sign
  get() = when {
    this>0->1
    this<0->-1
    else->0
  }
val State.targetFoods get() = width*height/100_000
val State.targetSize get():Int = 1000 + kotlin.math.sqrt(cars.sumBy {it.size}.toDouble() * 7000 ).toInt()
inline val State.width get() = size
inline val State.height get() = size
fun State.distance(a:XY,b:XY):Double {
  var dx = min(abs(b.x-a.x),b.x+width-a.x)
  dx = min(dx,a.x+width-b.x)
  var dy = min(abs(b.y-a.y),b.y+height-a.y)
  dy = min(dy,a.y+height-b.y)
  return sqrt(dx*dx+dy*dy)
}
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

private inline fun Random._rnd(min:Int, max:Int):Int {
  seed = seed*1664525+1013904223 and 0x7fffffff
  return min+seed%(max-min+1)
}
fun Random.rnd(min:Int, max:Int):Int {
  val сдвиг = 0xffFe
  val diapasone = max-min
  return min + diapasone/сдвиг*_rnd(0,сдвиг) + diapasone%сдвиг*_rnd(0,сдвиг)/сдвиг
}
fun Random.randomPos(state:State) = XY(rnd(1,state.width-1), rnd(1, state.height-1))