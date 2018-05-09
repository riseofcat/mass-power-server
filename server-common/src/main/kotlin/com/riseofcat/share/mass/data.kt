package com.riseofcat.share.mass

import com.riseofcat.lib.*
import kotlinx.serialization.*
import kotlin.math.*

const val SIMPLIFY_TEST_PERFORMANCE = true

@Deprecated("") val MAX_W = 5
@Deprecated("") val MAX_H = 5
class Mattr2D<T>(val COLS:Int, val ROWS:Int, init:(Int, Int)->T){
  val all = (0 until COLS*ROWS).map{Cell(it/ROWS,it%ROWS,init(it/ROWS,it%ROWS))}/*.toTypedArray()*/
  val map:Map<Int,Map<Int,Cell>>
  init{
    map = mutableMapOf()
    for(col in 0 until COLS) {
      val m2 = mutableMapOf<Int,Cell>()
      map[col] = m2
      for(row in 0 until ROWS) {
        m2[row] = if(true) all[col*ROWS+row] else Cell(col,row,init(col,row))
      }
    }
  }
  inner class Cell(val col:Int, val row:Int, val value:T) {
    val matrix = this@Mattr2D
  }
  inline operator fun get(col:Int,row:Int) = if(false) all[col*ROWS+row] else map[col]!![row]!!
}
fun <T>Mattr2D<MutableList<T>>.clearCache() {
  all.forEach {it.value.clear()}
}
inline fun State.repeatTick(ticks:Int, lambda:()->Unit) {
  if(!SIMPLIFY_TEST_PERFORMANCE) {
    repeatTickCalls++
    if((tick.tick-repeatTickCalls)%ticks==0) {
      lambda()
    }
  }
}

object GameConst {
  val CHANGE_SIZE_PER_CAR=400
  val UPDATE = Duration(40)
  val UPDATE_S = UPDATE.ms/lib.MILLIS_IN_SECOND
  val MIN_SIZE = 20
  val DEFAULT_CAR_SIZE = MIN_SIZE*6
  val FOOD_SIZE = 20
  val MIN_RADIUS = 1f
  val FOODS = 200
  val FOOD_PER_CAR = 10
  val BASE_WIDTH = 2300.0
  val BASE_HEIGHT = 2300.0
  val TITLE = "mass-power.io"
  val REACTIVE_LIVE = Tick(60)
}

interface ICommand { fun act(state:State) }
interface PosObject { var pos:XY }
interface SpeedObject:PosObject { var speed:XY }
interface SizeObject:PosObject { var size:Int }
interface EatMeWithSpeed:SizeObject, SpeedObject

@Serializable class NewCarCommand(var id:PlayerId):ICommand {
  override fun act(state:State) {
    if(state.cars.none{it.owner == id}) {
      state.cars.add(Car(
        id,
        GameConst.DEFAULT_CAR_SIZE,
        speed = XY(),
        pos = XY(state.rnd(0, state.width.toInt()).toDouble(), state.rnd(0, state.height.toInt()).toDouble())//todo проверить чтобы равномерно добавлялись новые машины (чтобы не было фантомных появлений из за случайного random-а), можно завести второй счётчик рандома под появление новых машин
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
  override var size:Int,
  override var pos:XY):SizeObject,PosObject
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
  ,var random:Int = 0
  ,var size:Int = 0
  ,var tick:Tick = Tick(0)
  ,@Transient var repeatTickCalls:Int = 0
  ,@Transient val cacheFood:Mattr2D<MutableList<Food>> = Mattr2D(MAX_W,MAX_H) {col,row-> mutableListOf<Food>()}
  ,@Transient val cacheReactive:Mattr2D<MutableList<Reactive>> = Mattr2D(MAX_W,MAX_H) {col,row-> mutableListOf<Reactive>()}
  ,@Transient val cacheCars:Mattr2D<MutableList<Car>> = Mattr2D(MAX_W,MAX_H) {col,row-> mutableListOf<Car>()}
)
fun State.deepCopy() = lib.measure("State.deepCopy") {
  copy(
    cars = cars.map {it.copy()}.toMutableList()
    ,reactive = reactive.map {it.copy()}.toMutableList()
    ,foods = foods.map {it.copy()}.toMutableList()//todo Если не делать такое копирование для food то странно применяются 2-3 действия подряд когда кушаем. Сначала уменьшается резко, потом увеличивается.
    ,cacheFood = cacheFood
    ,cacheReactive = cacheReactive
    ,cacheCars =  cacheCars
  )
}
@Serializable data class PlayerId(var id:Int)
@Serializable data class XY(var x:Double=0.0,var y:Double=0.0) {
  constructor(x:Float,y:Float):this(x.toDouble(), y.toDouble())
}
val SizeObject.radius get() = size.radius
val Int.radius get():Float = GameConst.MIN_RADIUS + 5*sqrt(this.toDouble()).toFloat()
fun degreesAngle(degrees:Int) = Angle(degrees/180.0*PI)
infix fun State.act(actions:Iterator<ICommand>):State {
  actions.forEach {it.act(this)}
  return this
}
data class Rect(val pos:XY, val size:XY)
val Rect.topLeft get() = pos.copy()
val Rect.topRight get() = pos + size.scale(XY(1.0,0.0))
val Rect.bottomLeft get() = pos + size.scale(XY(0.0,1.0))
val Rect.bottomRight get() = pos + size.scale(XY(1.0,1.0))
val Rect.points get() = arrayOf(topLeft, topRight, bottomLeft, bottomRight)

class Bucket(val col:Int,val row:Int) {
  val foods:MutableList<Food> = mutableListOf()
  val cars:MutableList<Car> = mutableListOf()
  val reactive:MutableList<Food> = mutableListOf()
  override fun toString() = "[$col, $row]"
}
fun State.tick() = lib.measure("tick") {  //23.447441085 %    count:3250  avrg100: 9.383111351 ms
  repeatTickCalls = 0
  tick+=1
  lib.skip_measure("tick.move") {
    (cars+reactive).forEach {o->
      o.pos = o.pos msum o.speed*GameConst.UPDATE_S
      o.speed = o.speed mscale 0.98
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
  fun Rect.isOverlap(other:Rect) = this.points.any{other.containsPoint(it)} || other.points.any{this.containsPoint(it)}
  fun Rect.alternative():List<Rect> {
    val result:MutableList<Rect> = mutableListOf()
    result.add(this)
    if(points.any {it.x>=width}) result.add(this.copy(pos = pos.copy(x = pos.x-width)))
    else if(points.any {it.x<0}) result.add(this.copy(pos = pos.copy(x = pos.x+width)))
    if(points.any {it.y>=height}) {
      result.copy().forEach {
        result.add(it.copy(pos = it.pos.copy(y = it.pos.y-height)))
      }
    } else if(points.any {it.y<0}) {
      result.copy().forEach {
        result.add(it.copy(pos = it.pos.copy(y = it.pos.y+height)))
      }
    }
    return result
  }

  fun SizeObject.toRect() = Rect(pos-XY(radius,radius),XY(2*radius,2*radius))
  fun SizeObject.isOverlapRect(rect:Rect) = toRect().alternative().any{it.isOverlap(rect)}
  fun <T>Mattr2D<T>.Cell.rect() = Rect(XY(col*width/matrix.COLS,row*height/matrix.ROWS),XY(width/matrix.COLS,height/matrix.ROWS))//todo проверить производительность
  val w = width.toFloat()/MAX_W
  val h = height.toFloat()/MAX_H

  infix fun SizeObject.overlap(xy:XY) = distance(this.pos, xy) <= this.radius
  fun <T>SizeObject.overlapCell(matrix:Mattr2D<T>) = matrix.all.filter {
    this.isOverlapRect(it.rect())
  }
  fun mod(value:Int,module:Int) = when {
    value<0->value+module
    value>=module->value-module
    else->value
  }
  fun PosObject.storeCol() = mod((pos.x/w).toInt(),MAX_W)
  fun PosObject.storeRow() = mod((pos.y/h).toInt(),MAX_H)

  repeatTick(20) {//todo выполнять сразу если кэш пустой
    lib.skip_measure("tick.sortBuckets") {
      cacheFood.clearCache()
      foods.forEach {cacheFood.get(it.storeCol(), it.storeRow()).value.add(it)}
    }
  }
  repeatTick(5) {
    cacheReactive.clearCache()
    reactive.forEach {cacheReactive.get(it.storeCol(), it.storeRow()).value.add(it)}
  }
  repeatTick(5) {
    lib.skip_measure("tick.eatFoods") { // 16.459389961 %    count:4251  avrg100: 7.000193569 ms
      var handleFoodCars = cars
      while(handleFoodCars.size > 0) {
        val changedSizeCars:MutableSet<Car> = mutableSetOf()
        for(car in handleFoodCars) {//очерёдность съедания вкусняшек важна. Если маленький съел вкусняшку первым, то большой его не съест
          val overlapBuckets = car.overlapCell(cacheFood)
          overlapBuckets.forEach {cell->
            val foodItr = cell.value.iterator()
            while(foodItr.hasNext()) {
              val f = foodItr.next()
              if(car overlap f.pos) {
                if(foods.contains(f)) {
                  foods.remove(f)
                  car.size += f.size
                  changedSizeCars.add(car)
                }
              }
            }
          }

          val overlapBuckets2 = car.overlapCell(cacheReactive)
          overlapBuckets2.forEach {cell->
            reactItr = cell.value.iterator()
            while(reactItr.hasNext()) {
              val r = reactItr.next()
              if(r.owner!=car.owner) if(car overlap r.pos) if(reactive.contains(r)) {
                reactive.remove(r)
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

  repeatTick(3) {
    cacheCars.clearCache()
    cars.forEach {cacheCars.get(it.storeCol(), it.storeRow()).value.add(it)}
  }
  repeatTick(3) {
    lib.skip_measure("tick.eatCars") {
      var handleCarsDestroy = true
      while(handleCarsDestroy) {
        handleCarsDestroy = false
        val carItr = cars.iterator()
//        val copy = cars.copy()//copy() нужно чтобы не было concurrent modification
        while(carItr.hasNext()) {
          val del = carItr.next()
          val overlapBuckets = del.overlapCell(cacheCars)
          overlapBuckets.forEach {cell->
            for(c in cell.value) {
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
  }

  while(foods.size<targetFoods) foods.add(Food(GameConst.FOOD_SIZE + rnd(0,GameConst.FOOD_SIZE),rndPos()))

  repeatTick(3) {
    if(targetSize!=size) lib.skip_measure("tick.resize") {
      val oldW = width
      val oldH = height
      val delta = targetSize-size
      val delta2 = (delta*lib.Fun.arg0toInf(abs(delta),50)).toInt()
      size = size+delta2.sign*min(abs(delta2),30)
      (cars+reactive+foods).forEach {p-> p.pos = p.pos mscale XY(width/oldW,height/oldH)}
    }
  }
}

val Int.sign
  get() = when {
    this>0->1
    this<0->-1
    else->0
  }
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