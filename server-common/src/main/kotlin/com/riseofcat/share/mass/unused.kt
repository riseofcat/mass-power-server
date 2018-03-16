package com.riseofcat.share.mass.unused

import com.riseofcat.share.mass.*

fun XY.rotate(angleA:Angle):XY {
  val result = copy()
  val angle = calcAngle() + angleA
  val len = len
  result.x = len*angle.cos
  result.y = len*angle.sin
  return result
}