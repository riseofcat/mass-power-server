package com.riseofcat.lib

import com.riseofcat.common.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import kotlin.jvm.*

val createMs = Lib.timeMs

class LibJava {
  companion object {
    @JvmStatic
    val log = Lib.Log
  }
}

object Lib {

  val timeMs get() = Common.timeMs
  val timeS get() = Common.timeMs/Const.MILLIS_IN_SECOND
  val sinceStartS get() = (timeMs-createMs)/Const.MILLIS_IN_SECOND
  fun pillarTimeMs(max:Long) = Fun.pillar(timeMs, max)
  fun pillarTimeS(max:Float) = pillarTimeMs((max*1000).toLong())/Lib.Const.MILLIS_IN_SECOND
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

  object Const {
    const val MILLIS_IN_SECOND = 1000f
  }

  object Log {
    enum class LogMode { INFO, ERROR, DEBUG }

    fun error(message:String,t:Throwable? = null) {
      _log(message,LogMode.ERROR)
      if(t!=null) Common.getStackTraceString(t)?.let {_println(it)}
    }

    fun info(s:String) = _log(s,LogMode.INFO)
    fun debug(s:String) = _log(s,LogMode.DEBUG)
    inline fun _log(str:CharSequence,mode:LogMode) {
      if(mode.ordinal<LogMode.DEBUG.ordinal) {
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
