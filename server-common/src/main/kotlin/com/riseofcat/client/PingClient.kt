package com.riseofcat.client

import com.riseofcat.common.*
import com.riseofcat.lib.*
import com.riseofcat.share.ping.*
import kotlinx.serialization.*

val PingClient.Companion.DEFAULT_LATENCY get() = Duration(150)

class PingClient<S:Any,C>(host:String,port:Int,path:String,typeS:KSerializer<ServerSay<S>>, val typeC:KSerializer<ClientSay<C>>) {
  private val incoming = Signal<S>()
  private val socket:LibWebSocket
  private val queue:MutableList<ClientSay<C>> = mutableListOf()//todo test
  private val latencies:MutableList<LatencyTime> = Common.createConcurrentList()//todo queue

  val smartLatency get():Duration {
    if(latencies.size == 0) return DEFAULT_LATENCY

    var sum = Duration(0)
    var weights = 0.0
    for(l in latencies) {
      var w:Double = 1E5//todo перенести логику точности в классы Time
      w *= 1.0-lib.Fun.arg0toInf(lib.time-l.clientTime,Duration(10_000))
      w *= 1.0-lib.Fun.arg0toInf(DEFAULT_LATENCY diffAbs l.sync.latency,DEFAULT_LATENCY)
      sum += l.sync.latency*w
      weights += w
    }
    return sum/weights
  }
  val latency get() = latencies.lastOrNull()?.sync?.latency?:DEFAULT_LATENCY

  init {
//    latencies.add(LatencyTime(DEFAULT_LATENCY,Common.timeMs))//todo delete
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
          lib.log.fatalError("serverSay parse", t)
        }

        if(serverSay.sync!=null) {
          latencies.add(LatencyTime(serverSay.sync,lib.time))
          while(latencies.size>20) latencies.removeFirst()//todo queue
        }
        if(serverSay.ping) say(ClientSay<C>(pong=true))
        if(serverSay.payload!=null) incoming.dispatch(serverSay.payload)
      }

    })
  }

  fun connect(incomeListener:SignalListener<S>) {
    incoming.add(incomeListener)
    try {
      socket.connect()
    } catch(e:Throwable) {
      lib.log.todo("handle offline")//todo
    }
  }

  fun close() = socket.close()
  fun say(payload:C) = say(ClientSay<C>(payload = payload))

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

  private class LatencyTime(val sync:TimeSync, val clientTime:TimeStamp)

  companion object
}
