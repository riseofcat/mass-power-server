package com.riseofcat.lib

object Lib {
  object Const {
    const val MILLIS_IN_SECOND = 1000f
  }

  object Fun {
    fun arg0toInf(y:Double,middle:Float):Float {
      return (y/middle.toDouble()/(1+y/middle)).toFloat()
    }
  }
}

fun <T> MutableList<T>.copy() = this.toMutableList()
