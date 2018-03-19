package com.riseofcat.lib

import com.riseofcat.common.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*

interface Time {
  val ms:Long
}
operator fun Time.minus(t:Time):Time = TimeSimple(ms - t.ms)
operator fun Time.plus(t:Time):Time = TimeSimple(ms + t.ms)
operator fun Time.div(int:Int):Time = TimeSimple(ms / int)
operator fun Time.div(f:Float):Time = TimeSimple(ms/f.toLong())
operator fun Time.compareTo(time:Time):Int = ms.compareTo(time.ms)
operator fun Time.times(d:Double):Time = TimeSimple((ms * d).toLong())
val Time.s get():Long = ms / 1000
val Time.sf get():Float = ms / 1000f
val Time.sd get():Double = ms / 1000.0

data class TimeSimple(override val ms:Long):Time
@Serializable data class Timestamp(override val ms:Long):Time
@Serializable data class Duration(override val ms:Long):Time
fun Time.toDuration():Duration = Duration(ms)

val createTime = lib.time
inline fun <reified /*@Serializable*/T:Any> T.deepCopy():T = try {
  CBOR.load<T>(CBOR.dump(this))
} catch(t:Throwable) {
  lib.log.fatalError("deepCopy",t)
}

object lib {
  const val MILLIS_IN_SECOND = 1000.0
  val time get() = Timestamp(Common.timeMs)
  val sinceStart get() = time-createTime
  fun pillarTimeMs(max:Long) = Fun.pillar(time.ms, max)
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
    fun arg0toInf(y:Double,middle:Double) = y/middle/(1+y/middle)
    fun arg0toInf(y:Long,middle:Long) = arg0toInf(y.toDouble(), middle.toDouble())
    fun arg0toInf(y:Int,middle:Int) = arg0toInf(y.toDouble(), middle.toDouble())
    fun pillar(value:Long, max:Long) = if((value/max)%2==0L) { value%max } else { max-value%max }//Имеет график /\/\/\/\
  }
}

fun <T> MutableList<T>.copy() = this.toMutableList()
fun <E> MutableList<E>.removeFirst() = removeAt(0)
