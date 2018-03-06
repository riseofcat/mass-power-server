package com.riseofcat.client

import com.riseofcat.common.*
import com.riseofcat.lib.*
import com.riseofcat.share.*
import kotlin.reflect.*

class PingClient<S:Any,C>(host:String,port:Int,path:String,typeS:KClass<ServerSay<S>>) {
  private val incoming = Signal<S>()
  private val socket:LibWebSocket
  private val queue:MutableList<ClientSay<C>> = mutableListOf()//todo test
  var smartLatencyS = Params.DEFAULT_LATENCY_S
  var latencyS = Params.DEFAULT_LATENCY_S
  private val latencies:MutableList<LatencyTime> = mutableListOf()

  init {
    latencies.add(LatencyTime(Params.DEFAULT_LATENCY_MS,Common.timeMs))
    socket = Common.createWebSocket(host,port,path)
    socket.addListener(object:LibWebSocket.Listener {
      override fun onOpen() {
        while(queue.isNotEmpty()) sayNow(queue.removeFirst())
      }

      override fun onClose() {

      }

      override fun onMessage(packet:String) {
        val serverSay = Common.fromJson(packet,typeS)
        if(serverSay.latency!=null) {
          latencyS = serverSay.latency!!/Lib.Const.MILLIS_IN_SECOND

          latencies.add(LatencyTime(serverSay.latency!!,Common.timeMs))
          while(latencies.size>100) latencies.removeFirst()
          var sum = 0f
          var weights = 0f
          val time = Common.timeMs
          for(l in latencies) {
            var w = (1-Lib.Fun.arg0toInf((time-l.time).toDouble(),10000f)).toDouble()
            w *= (1-Lib.Fun.arg0toInf(l.latency.toDouble(),Params.DEFAULT_LATENCY_MS.toFloat())).toDouble()
            sum += (w*l.latency).toFloat()
            weights += w.toFloat()
          }
          if(weights>Float.MIN_VALUE*1E10) smartLatencyS = sum/weights/Lib.Const.MILLIS_IN_SECOND
        }
        if(serverSay.ping) {
          val answer = ClientSay<C>()
          answer.pong = true
          say(answer)
        }
        if(serverSay.payload!=null) incoming.dispatch(serverSay.payload!!)
      }

    })
  }

  fun connect(incomeListener:Signal.Listener<S>) {
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
      socket.send(Common.toJson(say))
      return
    } catch(t:Throwable) {
      t.printStackTrace()
    }
  }

  private class LatencyTime(val latency:Int,val time:Long)
}
