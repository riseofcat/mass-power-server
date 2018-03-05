package com.riseofcat.share.data

import kotlin.math.*

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

  fun dst(xy:XY) = sqrt(((xy.x-x)*(xy.x-x)+(xy.y-y)*(xy.y-y)).toDouble())
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
      Angle(atan2(y.toDouble(),x.toDouble()).toFloat())
    else
      try {
        Angle(atan((y/x).toDouble()).toFloat()).add(Angle.degreesAngle(if(x<0) 180f else 0f))
      } catch(t:Throwable) {
        Angle.degreesAngle(y.sign*90f)
      }

  }
}