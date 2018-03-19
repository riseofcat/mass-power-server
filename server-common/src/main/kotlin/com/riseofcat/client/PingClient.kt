package com.riseofcat.client

import com.riseofcat.common.*
import com.riseofcat.lib.*
import com.riseofcat.share.ping.*
import kotlinx.serialization.*

val PingClient.Companion.DEFAULT_LATENCY get() = Duration(250)

class PingClient<S:Any,C>(host:String,port:Int,path:String,typeS:KSerializer<ServerSay<S>>, val typeC:KSerializer<ClientSay<C>>) {
  private val incoming = Signal<S>()
  private val socket:LibWebSocket
  private val queue:MutableList<ClientSay<C>> = mutableListOf()//todo test
  var smartLatency = DEFAULT_LATENCY
  var latency:Duration = DEFAULT_LATENCY
  private val latencies:MutableList<LatencyTime> = mutableListOf()

  init {
    latencies.add(LatencyTime(DEFAULT_LATENCY,Common.timeMs))
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

        if(serverSay.sync!=null) {
          latency = serverSay.sync.latency
          serverSay.sync.serverTime

          latencies.add(LatencyTime(serverSay.sync.latency,Common.timeMs))
          while(latencies.size>100) latencies.removeFirst()
          var sum = Duration(0)
          var weights = 0f
          val time = Common.timeMs
          for(l in latencies) {
            var w = 1.0-lib.Fun.arg0toInf(time-l.time,10_000)
            w *= (1-lib.Fun.arg0toInf(l.latency.ms,DEFAULT_LATENCY.ms))
            sum = (sum + l.latency * w).toDuration()
            weights += w.toFloat()
          }
          if(weights>Float.MIN_VALUE*1E10) smartLatency = (sum/weights).toDuration()
        }
        if(serverSay.ping) {
          say(ClientSay<C>(pong=true))
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
      lib.log.todo("handle offline")
    }

  }

  fun close() {
    socket.close()
  }

  fun say(payload:C) {
    say(ClientSay<C>(payload = payload))
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

  private class LatencyTime(val latency:Duration,val time:Long)

  companion object
}
