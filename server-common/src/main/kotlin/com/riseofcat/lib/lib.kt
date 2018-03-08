package com.riseofcat.lib

import com.riseofcat.common.*

val createMs = Lib.timeMs

object Lib {
  object Log {
    enum class LogMode { INFO, ERROR, DEBUG }

    fun error(message:String,t:Throwable? = null) {
      _log(message,LogMode.ERROR)
      if(t!=null) Common.getStackTraceString(t)?.let {_println(it)}
    }

    fun info(s:String) = _log(s,LogMode.INFO)
    fun debug(s:String) = _log(s,LogMode.DEBUG)
    inline fun _log(str:CharSequence,mode:LogMode) = _println("$mode: $str | in ${Common.getCodeLineInfo(2)}")
    inline fun _println(str:CharSequence) = println(str)
  }

  object Const {
    const val MILLIS_IN_SECOND = 1000f
  }

  object Fun {
    fun arg0toInf(y:Double,middle:Float):Float {
      return (y/middle.toDouble()/(1+y/middle)).toFloat()
    }
  }

  val timeMs get() = Common.timeMs
  val timeS get() = Common.timeMs/Const.MILLIS_IN_SECOND
  val sinceStartS get() = (timeMs - createMs)/Const.MILLIS_IN_SECOND
}

fun <T> MutableList<T>.copy() = this.toMutableList()
fun <E> MutableList<E>.removeFirst():E {
  return this.removeAt(0)
}
