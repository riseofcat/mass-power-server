package com.riseofcat.common

import kotlin.reflect.*

object ServerCommon {
  fun test():String {
    val k = "server"
    val m = "common"
    return "$k $m"
  }
}

expect class Common {
  companion object {
    fun <T> createConcurrentList():MutableList<T>
    fun <K,V> createConcurrentHashMap():MutableMap<K,V>
    fun toJson(obj:Any):String
    fun <T:Any> fromJson(str:String,clazz:KClass<T>):T
    fun createWebSocket(host:String,port:Int,path:String):LibWebSocket
    fun getStackTraceString(t:Throwable):String?
    val timeMs:Long
    fun getCodeLineInfo(depth:Int):CharSequence
  }
}

abstract class LibWebSocket {
  abstract fun addListener(webSocketAdapter:Listener)
  abstract fun connect()
  abstract fun close()
  abstract fun send(message:String)
  abstract val state:State

  interface Listener {
    fun onOpen()
    fun onClose()
    fun onMessage(packet:String)
  }

  enum class State {
    OPEN,
    CLOSE
  }
}

