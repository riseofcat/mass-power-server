package com.riseofcat.server

import com.riseofcat.lib.*
import com.riseofcat.share.ping.*
import java.util.concurrent.ConcurrentHashMap

class PingDecorator<C,S>(private val server:SesServ<C,S>,private val pingInterval:Duration):SesServ<ClientSay<C>,ServerSay<S>>() {
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
    val s = map[session] ?: lib.log.fatalError("session not found")
    if(say.pong) session send ServerSay(sync = TimeSync(Duration(1)+(lib.time-s.lastPing)/2,lib.time))
    if(say.payload!=null) server.message(s,say.payload!!)
  }

  inner class PingSes constructor(private val sess:Ses<ServerSay<S>>):Ses<S>() {
    var lastPing:TimeStamp = TimeStamp(0)
    override val id:Int get() = sess.id
    override val typeMap:TypeMap get() = sess.typeMap

    init {
      put(Extra(this))
    }

    override fun stop() = sess.stop()

    override fun send(payload:S) {
      val ping =
        if(lib.time>lastPing+pingInterval) {
          lastPing = lib.time
          true
        } else false
      sess.send(ServerSay(payload,null,ping))
    }
  }

  inner class Extra(private val pingSes:PingSes):TypeMap.Marker {
//    val latency:Int? get() = pingSes.latency
  }

}
