package com.riseofcat.client

import com.riseofcat.common.*
import com.riseofcat.lib.*
import com.riseofcat.share.ping.*
import kotlinx.serialization.*

interface IPingClient<S:Any,C> {
  val serverTime:TimeStamp
  val smartPingDelay:Duration
  fun connect(incomeListener:SignalListener<S>)
  fun close()
  fun say(payload:C)
  val clientMessages:Int
}

class FakePingClient<S:Any,C>(val connectData:S):IPingClient<S,C> {
  override val serverTime:TimeStamp get() = lib.time
  override val smartPingDelay:Duration = Duration(100)
  override fun connect(incomeListener:SignalListener<S>) {
    incomeListener(connectData)
  }
  override fun close() {}
  override fun say(payload:C) {}
  override val clientMessages:Int=0
}

class PingClient<S:Any,C>(host:String,port:Int,path:String,typeS:KSerializer<ServerSay<S>>, val typeC:KSerializer<ClientSay<C>>):IPingClient<S,C> {
  private val incoming = Signal<S>()
  private val socket:LibWebSocket
  private val queue:MutableList<ClientSay<C>> = mutableListOf()//todo test
  private val pingDelays:MutableList<PingDelay> = Common.createConcurrentList()//todo queue
  private val timeSync:MutableList<TimeSync> = Common.createConcurrentList()//todo queue
  override var clientMessages:Int = 0
  val lastPingDelay get() = pingDelays.lastOrNull()?.pingDelay
  override val smartPingDelay get():Duration {
    if(pingDelays.size == 0) return Duration(0)

    var sum = Duration(0)
    var weights = 0.0

    val average = pingDelays.sumByDuration{it.pingDelay}/pingDelays.size

    for(l in pingDelays) {
      var w:Double = 1E5//todo перенести логику точности в классы Time
      w *= 1.0-libObj.Fun.arg0toInf(lib.time-l.clientTime,Duration(10_000))
      w *= 1.0-libObj.Fun.arg0toInf(average diffAbs l.pingDelay,Duration(100))
      sum += l.pingDelay*w
      weights += w
    }
    return sum/weights
  }

  override val serverTime:TimeStamp get() {//todo потестировать перевод времени
    var result = lib.time
    timeSync.lastOrNull()?.run {
      result += server - client + smartPingDelay
    }
    return result
  }

  init {
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

        if(serverSay.serverTime!=null) timeSync.add(TimeSync(serverSay.serverTime,lib.time))
        if(serverSay.pingDelay!=null) {
          pingDelays.add(PingDelay(serverSay.pingDelay,lib.time))
          while(pingDelays.size>20) pingDelays.removeFirst()//todo queue
        }
        if(serverSay.ping) say(ClientSay<C>(pong=true))
        if(serverSay.payload!=null) incoming.dispatch(serverSay.payload)
      }

    })
  }

  override fun connect(incomeListener:SignalListener<S>) {
    incoming.add(incomeListener)
    try {
      socket.connect()
    } catch(e:Throwable) {
      lib.log.todo("handle offline")//todo
    }
  }

  override fun close() = socket.close()
  override fun say(payload:C) = say(ClientSay<C>(payload = payload))

  private fun say(say:ClientSay<C>) {
    if(socket.state == LibWebSocket.State.OPEN)
      sayNow(say)
    else
      queue.add(say)
  }

  private fun sayNow(say:ClientSay<C>) {
    try {
      clientMessages++
      socket.send(lib.objStrSer.stringify(typeC, say))
      return
    } catch(t:Throwable) {
      lib.log.error("socket.send error", t)
    }
  }

  private class PingDelay(val pingDelay:Duration, val clientTime:TimeStamp)
}
data class TimeSync(val server:TimeStamp, val client:TimeStamp)