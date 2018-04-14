package com.riseofcat.lib

import com.riseofcat.common.*
import com.riseofcat.share.mass.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import kotlin.reflect.*

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

fun <E> Collection<E>.averageSqrt(selector:(E) -> Double?):Double {
  var result = 0.0
  forEach {
    val v = selector(it)
    if(v != null) {
      result+=v*v
    }
  }
  return kotlin.math.sqrt(result)/size
}

fun <E> Collection<E>.sumByDuration(selector:(E) -> Duration):Duration {
  var result = Duration(0)
  forEach {result+=selector(it)}
  return result
}

val Time.s get():Long = ms / 1000
val Time.sf get():Float = ms / 1000f
val Time.sd get():Double = ms / 1000.0

val createTime = libObj.time
val lib = libObj
inline fun <reified /*@Serializable*/T:Any> T.deepCopy():T = lib.measure("deepCopy"){
  try {
    CBOR.load<T>(CBOR.dump(this))
  } catch(t:Throwable) {
    lib.log.fatalError("deepCopy",t)
  }
}

object libObj {
  const val MILLIS_IN_SECOND = 1000.0
  val time get() = TimeStamp(Common.timeMs)
  val sinceStart get() = time-createTime
  fun pillarTimeMs(max:Long) = Fun.pillar(time.ms, max)
  fun pillarTimeS(max:Float) = pillarTimeMs((max*1000).toLong())/MILLIS_IN_SECOND
  val json = JSON(unquoted = true, nonstrict = true)
  val objStrSer = json
  inline fun <reified T:Any>getKClass() = T::class
  fun rnd(min:Int,max:Int) = (min+Common.random()*(max-min+1)).toInt()
  fun inLimits(value:Int, min:Int, max:Int) = kotlin.math.max(min, kotlin.math.min(max, value))

  object cbor {
    fun <T:Any> stringify(saver:KSerialSaver<T>, obj: T): String {
      return HexConverter.printHexBinary(CBOR.dump(saver, obj), lowerCase = true)
    }
    fun <T:Any> parse(loader: KSerialLoader<T>, str: String): T {
      return CBOR.load(loader, HexConverter.parseHexBinary(str))
    }
  }

  val log = logObj

  object logObj {
    enum class LogMode { TODO, FATAL_ERROR, ERROR, INFO, MEASURE, DEBUG, BREAKPOINT}

    private fun handleThrowable(t:Throwable?) {
      if(t!=null) Common.getStackTraceString(t)?.let {_println(it)}
    }
    fun todo(str:CharSequence):Nothing {
      _log(str,LogMode.TODO)
      throw Throwable("${LogMode.TODO}: $str")
    }
    fun fatalError(message:CharSequence,t:Throwable? = null):Nothing {
      _log(message,LogMode.FATAL_ERROR, 3)
      handleThrowable(t)
      throw Throwable("${LogMode.FATAL_ERROR}: $message")
    }

    fun error(message:CharSequence,t:Throwable? = null) {
      _log(message,LogMode.ERROR, 3)
      handleThrowable(t)
    }

    fun info(s:CharSequence) = _log(s,LogMode.INFO)
    fun debug(s:CharSequence) = _log(s,LogMode.DEBUG)
    fun breakpoint(s:CharSequence = "") = _log(s,LogMode.BREAKPOINT)
    inline fun _log(str:CharSequence,mode:LogMode, codeDepth:Int = 2) {
      if(mode.ordinal<LogMode.DEBUG.ordinal) {//todo сделать конфигурацию
        _println("$mode: $str | in ${Common.getCodeLineInfo(codeDepth)}")
      }
    }
    inline fun _println(str:CharSequence) = println(str)
  }

  inline fun debug(block:()->Unit) {
//    block()
  }
  inline fun release(block:()->Unit) {
    block()
  }
  inline fun <T>releaseOrDebug(rel:()->T, deb:()->T):T {
    release {
      return@releaseOrDebug rel()
    }
    debug {
      return@releaseOrDebug deb()
    }
    lib.log.fatalError("specify release or debug")
  }

  var measurementsBegin:Time? = null
  fun <T>measure(hashTag:String, block:()->T):T {
    return releaseOrDebug({
      block()
    }) {
      if(measurementsBegin == null) {
        measurementsBegin = lib.time
      }
      var result:T? = null
      Common.getCodeLineInfo(2)
      val t = Common.measureNanoTime {
        result = block()
      }/1e9
      measurements.getOrPut(hashTag) {Measure()}.add(t)

      if(time > previousMeasurePrint + Duration(10_000)) {
        previousMeasurePrint = time
        log._println("measure: ")
        measurements.entries.forEach {
          log._println("#${it.key}: ${it.value}")
        }

      }
      result as T
    }
  }

  var previousMeasurePrint = time
  val measurements:MutableMap<String, Measure> = mutableMapOf()

  class Measure{
    var average100s:Double? = null
    var average20s:Double? = null
    var sum:Double = 0.0
    var count:Int = 0

    fun add(value:Double) {
      fun averageN(prev:Double?,n:Int) = if(prev==null) value else (prev*n+value)/(n+1)
      ++count
      sum+=value
      average20s = averageN(average20s,20)
      average100s = averageN(average100s,100)
    }

    override fun toString():String {
      var result = ""
      val beginTime = measurementsBegin
      if(beginTime != null) {
        result += "sum%: ${lib.formatDouble(sum*100/(lib.time.s - beginTime.s), 9)} %    count:$count"
        average100s?.let{
          result += "\navrg100: ${lib.formatDouble(it*1000, 9)} ms"
        }
      }
      return result
    }
  }

  private fun formatDouble(value:Double,afterComa:Int):String {
    var digits = 1
    repeat(afterComa) {
      digits = digits*10
    }
    return "${value.toInt()}.${(value*digits).toLong()%digits}"
  }

  inline fun saveInvoke(lambda:()->Unit) {
    saveInvoke<Unit>(lambda)
  }
  inline fun <T> saveInvoke(lambda:()->T):T? = try {
    lambda()
  } catch(e:Throwable) {
    log.error("save invoke fail",e); null
  }

  val Fun = FunObj

  object FunObj {
    fun arg0toInf(y:Double,middle:Double) = y/middle/(1+y/middle)
    fun arg0toInf(y:Float,middle:Float) = y/middle/(1+y/middle)
    fun arg0toInf(y:Long,middle:Long) = arg0toInf(y.toDouble(), middle.toDouble())
    fun arg0toInf(y:Int,middle:Int) = arg0toInf(y.toDouble(), middle.toDouble())
    fun arg0toInf(y:Time,middle:Time) = arg0toInf(y.ms, middle.ms)
    fun pillar(value:Long, max:Long) = if((value/max)%2==0L) { value%max } else { max-value%max }//Имеет график /\/\/\/\
  }
  fun <T>smoothByTime(lambda:()->Double) = SmoothByTime<T>(lambda)
}

fun <T> MutableList<T>.copy() = toMutableList()
inline infix fun <T> MutableList<T>.rm(del:T) = remove(del)
fun <E> MutableList<E>.removeFirst() = removeAt(0)

class SmoothByTime<T>(val lambda:()->Double) {
  operator fun getValue(t:T,property:KProperty<*>):Double {
    return lambda()//todo time
  }
}
