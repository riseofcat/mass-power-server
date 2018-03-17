package com.riseofcat.share.mass.unused

import com.riseofcat.share.mass.*
import kotlin.math.*

fun XY.rotate(angleA:Angle):XY {
  val result = copy()
  val angle = calcAngle() + angleA
  val len = len
  result.x = len*angle.cos
  result.y = len*angle.sin
  return result
}
fun degreesAngle(degrees:Double) = Angle(degrees/180*PI)
fun State.deepCopyOld() = copy(//todo data класс не копирует массивы
  cars = cars.map {it.copy()}.toMutableList(),//todo deep copy pos
  foods = foods.map {it.copy()}.toMutableList(),
  reactive = reactive.map {it.copy()}.toMutableList()
)