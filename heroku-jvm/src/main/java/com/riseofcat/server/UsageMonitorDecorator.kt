package com.riseofcat.server

import com.riseofcat.common.*
import com.riseofcat.lib.TypeMap

class UsageMonitorDecorator<C,S>(private val server:SesServ<C,S>):SesServ<C,S>() {
  private val map:MutableMap<Ses, CountSes> = Common.createConcurrentHashMap()
  var sessionsCount = 0
    private set

  override fun start(session:Ses) {
    val s = CountSes(session)
    map[session] = s
    server.start(s)
    sessionsCount++
  }

  override fun close(session:Ses) {
    server.close(map[session]!!)
    map.remove(session)
    sessionsCount--
  }

  override fun message(session:Ses,code:C) {
    val s = map[session]
    server.message(s!!,code)
    s.incomeCalls++
  }

  inner class CountSes(private val sess:Ses):Ses() {
    var incomeCalls:Int = 0
    var outCalls:Int = 0
    val startTimeMs:Long
    override val id:Int get() = sess.id
    override val typeMap:TypeMap get() = sess.typeMap

    init {
      startTimeMs = System.currentTimeMillis()
      put<Extra>(Extra(this))
    }

    override fun stop() {
      sess.stop()
    }

    override fun send(message:S) {
      sess.send(message)
      outCalls++
    }
  }

  inner class Extra(private val countSes:UsageMonitorDecorator<C,S>.CountSes):TypeMap.Marker {
    val incomeCalls:Int get() = countSes.incomeCalls
    val outCalls:Int get() = countSes.outCalls
    val startTime:Long get() = countSes.startTimeMs
  }
}
