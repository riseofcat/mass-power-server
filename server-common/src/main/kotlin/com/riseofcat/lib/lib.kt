package com.riseofcat.lib

import com.riseofcat.common.*
import com.riseofcat.share.mass.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*

interface Time {
  val ms:Long
}

@Serializable data class TimeStamp(override val ms:Long):Time
@Serializable data class Duration(override val ms:Long):Time

operator fun TimeStamp.minus(t:TimeStamp):Duration = Duration(ms-t.ms)
operator fun TimeStamp.minus(t:Duration):TimeStamp = TimeStamp(ms-t.ms)
operator fun TimeStamp.plus(t:Duration):TimeStamp = TimeStamp(ms+t.ms)
operator fun Duration.plus(t:TimeStamp):TimeStamp = TimeStamp(ms+t.ms)

operator fun Duration.plus(t:Duration):Duration = Duration(ms+t.ms)
operator fun Duration.minus(t:Duration):Duration = Duration(ms-t.ms)
infix fun Duration.diffAbs(d:Duration):Duration = (this - d).abs
val Duration.abs:Duration get() = Duration(kotlin.math.abs(ms))
operator fun Duration.div(int:Int):Duration = Duration(ms/int)
operator fun Duration.div(f:Float):Duration = Duration((ms/f).toLong())
operator fun Duration.div(double:Double):Duration = Duration((ms/double).toLong())
operator fun Duration.times(d:Double):Duration = Duration((ms*d).toLong())
operator fun Duration.times(int:Int):Duration = Duration(ms*int)
operator fun Duration.times(tick:Tick):Duration = Duration(ms*tick.tick)
operator fun Tick.times(d:Duration):Duration = d * this

operator fun Time.compareTo(time:Time):Int = ms.compareTo(time.ms)
operator fun Time.div(t:Time) = ms/t.ms

fun <E> Collection<E>.sumByDuration(selector:(E) -> Duration):Duration {
  var result = Duration(0)
  forEach {result+=selector(it)}
  return result
}

val Time.s get():Long = ms / 1000
val Time.sf get():Float = ms / 1000f
val Time.sd get():Double = ms / 1000.0

val createTime = lib.time
inline fun <reified /*@Serializable*/T:Any> T.deepCopy():T = lib.measure("deepCopy"){
  try {
    CBOR.load<T>(CBOR.dump(this))
  } catch(t:Throwable) {
    lib.log.fatalError("deepCopy",t)
  }
}

object lib {
  const val MILLIS_IN_SECOND = 1000.0
  val time get() = TimeStamp(Common.timeMs)
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
    enum class LogMode { TODO, FATAL_ERROR, ERROR, INFO, MEASURE, DEBUG, BREAKPOINT}

    private inline fun handleThrowable(t:Throwable?) {
      if(t!=null) Common.getStackTraceString(t)?.let {_println(it)}
    }
    fun todo(str:String):Nothing {
      _log(str,LogMode.TODO)
      throw Throwable("${LogMode.TODO}: $str")
    }
    fun fatalError(message:String,t:Throwable? = null):Nothing {
      _log(message,LogMode.FATAL_ERROR, 2)
      handleThrowable(t)
      throw Throwable("${LogMode.FATAL_ERROR}: $message")
    }

    fun error(message:String,t:Throwable? = null) {
      _log(message,LogMode.ERROR)
      handleThrowable(t)
    }

    fun info(s:CharSequence) = _log(s,LogMode.INFO)
    fun debug(s:CharSequence) = _log(s,LogMode.DEBUG)
    fun breakpoint(s:CharSequence = "") = _log(s,LogMode.BREAKPOINT)
    inline fun _log(str:CharSequence,mode:LogMode, codeDepth:Int = 2) {
      if(mode.ordinal<LogMode.DEBUG.ordinal) {
        _println("$mode: $str | in ${Common.getCodeLineInfo(codeDepth)}")
      }
    }
    inline fun _println(str:CharSequence) = println(str)
  }

  inline fun debug(block:()->Unit) {
    block()
  }

  inline fun releae(block:()->Unit) {
//    block()
  }

  fun <T>measure(hashTag:String, block:()->T):T {
    var result:T? = null
    Common.getCodeLineInfo(2)
    val value = Common.measureNanoTime {
      result = block()
    }/1e9
    measurements.getOrPut(hashTag) {Measure()}.add(value)

    if(lib.time > previousMeasurePrint + Duration(10_000)) {
      previousMeasurePrint = lib.time
      lib.log._println("measure: ")
      measurements.entries.forEach {
        lib.log._println("#${it.key}: ${it.value}")
      }

    }
    return result!!
  }

  var previousMeasurePrint = lib.time
  val measurements:MutableMap<String, Measure> = mutableMapOf()

  class Measure{
    var average100s:Double? = null
    var average20s:Double? = null
    var sum:Double = 0.0

    fun add(value:Double) {
      fun averageN(prev:Double?,n:Int) = if(prev==null) value else (prev*n+value)/(n+1)
      sum+=value
      average20s = averageN(average20s,20)
      average100s = averageN(average100s,100)
    }

    override fun toString():String {
      var result = ""
      result += "sum: ${sum.toInt()}.${(sum*1e6).toLong()%1_000_000}s\n"
      average100s?.let{
        result += "avrg100: ${(it*1_000).toInt()}.${(it*1e9).toLong()%1_000_000}ms"
      }
      return result
    }
  }

  object Fun {
    fun arg0toInf(y:Double,middle:Double) = y/middle/(1+y/middle)
    fun arg0toInf(y:Long,middle:Long) = arg0toInf(y.toDouble(), middle.toDouble())
    fun arg0toInf(y:Int,middle:Int) = arg0toInf(y.toDouble(), middle.toDouble())
    fun arg0toInf(y:Time,middle:Time) = arg0toInf(y.ms, middle.ms)
    fun pillar(value:Long, max:Long) = if((value/max)%2==0L) { value%max } else { max-value%max }//Имеет график /\/\/\/\
  }
}

fun <T> MutableList<T>.copy() = toMutableList()
inline infix fun <T> MutableList<T>.rm(del:T) = remove(del)
fun <E> MutableList<E>.removeFirst() = removeAt(0)
