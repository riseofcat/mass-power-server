package com.riseofcat.server

import com.riseofcat.lib.TypeMap
import com.riseofcat.share.base.ClientSay
import com.riseofcat.share.base.ServerSay
import java.util.concurrent.ConcurrentHashMap

class PingDecorator<C,S>(private val server:SesServ<C,S>,private val pingIntervalMs:Int):SesServ<ClientSay<C>,ServerSay<S>>() {
  private val map = ConcurrentHashMap<Ses<ServerSay<S>>,PingSes>()
  override fun start(session:Ses<ServerSay<S>>) {
    val s = PingSes(session)
    map[session] = s
    server.start(s)
  }

  override fun close(session:Ses<ServerSay<S>>) {
    server.close(map[session]!!)
    map.remove(session)
  }

  override fun message(session:Ses<ServerSay<S>>,say:ClientSay<C>) {
    val s = map[session]
    if(say.pong&&s!!.lastPingTime!=null) {
      val l = (System.currentTimeMillis()-s.lastPingTime!!+1)/2
      s.latency = l.toInt()
    }
    if(say.payload!=null) {
      server.message(s!!,say.payload!!)
    }
  }

  inner class PingSes constructor(private val sess:Ses<ServerSay<S>>):Ses<S>() {
    var lastPingTime:Long? = null
    var latency:Int? = null
    override val id:Int
      get() = sess.id
    override val typeMap:TypeMap
      get() = sess.typeMap

    init {
      put(Extra(this))
    }

    override fun stop() {
      sess.stop()
    }

    override fun send(payload:S) {
      val say = ServerSay(payload)
      say.latency = latency
      if(lastPingTime==null||System.currentTimeMillis()>lastPingTime!!+pingIntervalMs) {
        say.ping = true
        lastPingTime = System.currentTimeMillis()
      }
      sess.send(say)
    }
  }

  inner class Extra(private val pingSes:PingSes):TypeMap.Marker {
    val latency:Int? get() = pingSes.latency
  }

}
