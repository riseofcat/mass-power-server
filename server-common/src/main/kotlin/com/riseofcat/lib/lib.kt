package com.riseofcat.lib

import com.riseofcat.common.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*

val createMs = lib.timeMs
inline fun <reified /*@Serializable*/T:Any> T.deepCopy():T = try {
  CBOR.load<T>(CBOR.dump(this))
} catch(t:Throwable) {
  lib.log.fatalError("deepCopy",t)
}

object lib {
  const val MILLIS_IN_SECOND = 1000.0
  const val MEGA = 1E6f;
  val timeMs:Long get() = Common.timeMs
  val timeS:Long get() = Common.timeMs/1000L
  val sinceStartS get() = (timeMs-createMs)/MILLIS_IN_SECOND
  fun pillarTimeMs(max:Long) = Fun.pillar(timeMs, max)
  fun pillarTimeS(max:Float) = pillarTimeMs((max*1000).toLong())/MILLIS_IN_SECOND
  val json = JSON(unquoted = true, nonstrict = true)
  val objStrSer = json
  inline fun <reified T:Any>getKClass() = T::class

  object cbor {
    fun <T:Any> stringify(saver:KSerialSaver<T>, obj: T): String {
      return HexConverter.printHexBinary(CBOR.dump(saver, obj), lowerCase = true)
    }
    fun <T:Any> parse(loader: KSerialLoader<T>, str: String): T {
      return CBOR.load(loader, HexConverter.parseHexBinary(str))
    }
  }

  object const {

  }

  object log {
    enum class LogMode { FATAL_ERROR, ERROR, INFO, DEBUG, BREAKPOINT }

    private inline fun handleThrowable(t:Throwable?) {
      if(t!=null) Common.getStackTraceString(t)?.let {_println(it)}
    }
    fun fatalError(message:String,t:Throwable? = null):Nothing {
      _log(message,LogMode.FATAL_ERROR)
      handleThrowable(t)
      throw Throwable("${LogMode.FATAL_ERROR}: $message")
    }

    fun error(message:String,t:Throwable? = null) {
      _log(message,LogMode.ERROR)
      handleThrowable(t)
    }

    fun info(s:String) = _log(s,LogMode.INFO)
    fun debug(s:String) = _log(s,LogMode.DEBUG)
    fun breakpoint(s:String = "") = _log(s,LogMode.BREAKPOINT)
    inline fun _log(str:CharSequence,mode:LogMode) {
      if(mode.ordinal<LogMode.BREAKPOINT.ordinal) {
        _println("$mode: $str | in ${Common.getCodeLineInfo(2)}")
      }
    }
    inline fun _println(str:CharSequence) = println(str)
  }

  object Fun {
    fun arg0toInf(y:Double,middle:Float):Float {
      return (y/middle.toDouble()/(1+y/middle)).toFloat()
    }
    fun pillar(value:Long, max:Long) = if((value/max)%2==0L) { value%max } else { max-value%max }//Имеет график /\/\/\/\
  }
}

fun <T> MutableList<T>.copy() = this.toMutableList()
fun <E> MutableList<E>.removeFirst() = removeAt(0)
