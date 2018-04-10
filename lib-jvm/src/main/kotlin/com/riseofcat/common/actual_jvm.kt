package com.riseofcat.common

import com.github.czyzby.websocket.*
import com.github.czyzby.websocket.data.*
import java.util.concurrent.*

actual class Common {
  actual companion object {
    actual fun <T> createConcurrentList():MutableList<T> {
      return CopyOnWriteArrayList()
    }

    actual fun <K,V> createConcurrentHashMap():MutableMap<K,V> {
      return ConcurrentHashMap()
    }

    var firstCreateWebSocket = true
    actual fun createWebSocket(host:String,port:Int,path:String):LibWebSocket {
      if(firstCreateWebSocket) {
        CommonWebSockets.initiate()
        firstCreateWebSocket = false
      }
      val socket = WebSockets.newSocket(WebSockets.toWebSocketUrl(host,port,path))
//      import com.github.czyzby.websocket.net.Extended
//      ExtendedNet.getNet().newWebSocket(host,port,path)
      return object:LibWebSocket() {

        override fun send(message:String) {
          socket.send(message)
        }

        override val state:State
          get() = if(socket.state==WebSocketState.OPEN) State.OPEN else State.CLOSE

        override fun connect() {
          socket.connect()
        }

        override fun close() {
          WebSockets.closeGracefully(socket) // Null-safe closing method that catches and logs any exceptions.
          if(false) socket.close()
        }

        override fun addListener(listener:Listener) {
          socket.addListener(object:WebSocketAdapter() {
            override fun onOpen(webSocket:WebSocket?):Boolean {
              listener.onOpen()
              return WebSocketListener.FULLY_HANDLED
            }

            override fun onMessage(webSocket:WebSocket?,packet:String?):Boolean {
              listener.onMessage(packet!!)
              return WebSocketListener.FULLY_HANDLED
            }

            override fun onClose(webSocket:WebSocket?,code:WebSocketCloseCode?,reason:String?):Boolean {
              listener.onClose()
              return WebSocketListener.FULLY_HANDLED
            }

            override fun onMessage(webSocket:WebSocket?,packet:ByteArray?):Boolean {
              return super.onMessage(webSocket,packet)
            }

            override fun onError(webSocket:WebSocket?,error:Throwable?):Boolean {
              return super.onError(webSocket,error)
            }

          })
        }

      }
    }

    actual val timeMs:Long get() = System.currentTimeMillis()
    actual fun getStackTraceString(t:Throwable):String? {
      return buildString {
        appendln("Custom EXCEPTION log in actual.jvm:")
        appendln("${t.javaClass.name}: ${t.message}")
        t.localizedMessage
        for(e in t.stackTrace) {
          appendln(e.prettyString)
        }
      }
    }

    actual fun getCodeLineInfo(depth:Int):CharSequence = try {
      throw Exception()
    } catch(e:Throwable) {
      e.stackTrace.getOrNull(depth)?.prettyString ?: ""
    }

    actual fun measureNanoTime(block:()->Unit):Long = kotlin.system.measureNanoTime(block)
    actual fun urlGet(url:String):String {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    actual fun random() = Math.random()
  }
}

val StackTraceElement.prettyString:CharSequence get() = "\t$className-$methodName ($fileName:$lineNumber)"
@Deprecated("use lib.rnd() instead")
fun rnd(min:Int,max:Int) = (min+Math.random()*(max-min+1)).toInt()