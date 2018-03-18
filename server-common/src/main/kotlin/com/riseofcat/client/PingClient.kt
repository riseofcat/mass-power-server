package com.riseofcat.client

import com.riseofcat.common.*
import com.riseofcat.lib.*
import com.riseofcat.share.ping.*
import kotlinx.serialization.*

val PingClient.Companion.DEFAULT_LATENCY_MS get() = 250
val PingClient.Companion.DEFAULT_LATENCY_S get() = DEFAULT_LATENCY_MS/lib.MILLIS_IN_SECOND

class PingClient<S:Any,C>(host:String,port:Int,path:String,typeS:KSerializer<ServerSay<S>>, val typeC:KSerializer<ClientSay<C>>) {
  private val incoming = Signal<S>()
  private val socket:LibWebSocket
  private val queue:MutableList<ClientSay<C>> = mutableListOf()//todo test
  var smartLatencyS = DEFAULT_LATENCY_S
  var latencyS = DEFAULT_LATENCY_S
  private val latencies:MutableList<LatencyTime> = mutableListOf()

  init {
    latencies.add(LatencyTime(DEFAULT_LATENCY_MS,Common.timeMs))
    socket = Common.createWebSocket(host,port,path)
    socket.addListener(object:LibWebSocket.Listener {
      override fun onOpen() {
        while(queue.isNotEmpty()) sayNow(queue.removeFirst())
      }

      override fun onClose() {

      }

      override fun onMessage(packet:String) {
        val serverSay:ServerSay<S> = try {
          lib.log.debug(packet)
          lib.objStrSer.parse(typeS, packet)
        } catch(t:Throwable) {
          lib.log.error("serverSay parse", t)
          TODO("")
        }

        if(serverSay.latency!=null) {
          latencyS = serverSay.latency/lib.MILLIS_IN_SECOND

          latencies.add(LatencyTime(serverSay.latency,Common.timeMs))
          while(latencies.size>100) latencies.removeFirst()
          var sum = 0f
          var weights = 0f
          val time = Common.timeMs
          for(l in latencies) {
            var w = 1.0-lib.Fun.arg0toInf(time-l.time,10_000)
            w *= (1-lib.Fun.arg0toInf(l.latency,DEFAULT_LATENCY_MS))
            sum += (w*l.latency).toFloat()
            weights += w.toFloat()
          }
          if(weights>Float.MIN_VALUE*1E10) smartLatencyS = sum/weights/lib.MILLIS_IN_SECOND
        }
        if(serverSay.ping) {
          val answer = ClientSay<C>()
          answer.pong = true
          say(answer)
        }
        if(serverSay.payload!=null) incoming.dispatch(serverSay.payload)
      }

    })
  }

  fun connect(incomeListener:SignalListener<S>) {
    incoming.add(incomeListener)
    try {
      socket.connect()
    } catch(e:Throwable) {
      //todo handle offline
    }

  }

  fun close() {
    socket.close()
  }

  fun say(payload:C) {
    val answer = ClientSay<C>()
    answer.payload = payload
    say(answer)
  }

  private fun say(say:ClientSay<C>) {
    if(socket.state == LibWebSocket.State.OPEN)
      sayNow(say)
    else
      queue.add(say)
  }

  private fun sayNow(say:ClientSay<C>) {
    try {
      socket.send(lib.objStrSer.stringify(typeC, say))
      return
    } catch(t:Throwable) {
      lib.log.error("socket.send error", t)
    }
  }

  private class LatencyTime(val latency:Int,val time:Long)

  companion object
}
