package com.riseofcat.common

object ServerCommon {
  fun test() = "server common"
}

expect class Common {
  companion object {
    fun <T> createConcurrentList():MutableList<T>
    fun <K,V> createConcurrentHashMap():MutableMap<K,V>
    fun createWebSocket(host:String,port:Int,path:String):LibWebSocket
    fun getStackTraceString(t:Throwable):String?
    val timeMs:Long
    fun getCodeLineInfo(depth:Int):CharSequence
    fun measureNanoTime(block:()->Unit):Long
    fun urlGet(url:String):String
    fun random():Double
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

  enum class State(val message:String, val good:Boolean) {
    OPEN("Успешное соединение с сервером", true),
    CLOSE("Закрываю соединение", false),
    CONNECTING("Соединение...", true),
    CLOSING("Закрытие соединения", false),
    CLOSED("Соединение закрыто", false),
    TIMEOUT("Время ожидания истекло", false);

    override fun toString() = message
  }
}
