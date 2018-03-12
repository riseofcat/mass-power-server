package com.riseofcat.server

import com.riseofcat.lib.*
import com.riseofcat.lib_gwt.*
import java.util.concurrent.*

class ConvertDecorator<CSay,SSay,CCode,SCode>(
  private val server:SesServ<CSay,SSay>,
  private val cConv:IConverter<CCode,CSay>,
  private val sConv:IConverter<SSay,SCode>):SesServ<CCode,SCode>() {
  private val map = ConcurrentHashMap<Ses<SCode>,Ses<SSay>>()
  override fun start(session:Ses<SCode>) {
    val s = object:Ses<SSay>() {
      override val id:Int
        get() = session.id
      override val typeMap:TypeMap
        get() = session.typeMap

      override fun stop() {
        session.stop()
      }

      override fun send(data:SSay) {
        session.send(sConv.convert(data))
      }
    }
    map[session] = s
    server.start(s)
  }

  override fun close(sess:Ses<SCode>) {
    server.close(map[sess]!!)
    map.remove(sess)
  }

  override fun message(ses:Ses<SCode>,code:CCode) {
    server.message(map[ses]!!,cConv.convert(code))
  }

}
