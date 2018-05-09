package com.riseofcat.server

import com.riseofcat.lib.*
import com.riseofcat.share.ping.ClientSay
import com.riseofcat.share.ping.ServerSay
import com.riseofcat.share.mass.ClientPayload
import com.riseofcat.share.mass.ServerPayload
import com.riseofcat.share.mass.SerializeHelp

class Util{
  companion object {
    fun fromJsonClientSay(str:String):ClientSay<ClientPayload> {
      try{
        return lib.strSer.parse(SerializeHelp.clientSayClientPayloadSerializer, str)
      } catch(e:Exception) {
        lib.log.fatalError("json parse,\njson:\n $str", e)
      }
    }

    fun toServerSayJson(ss:ServerSay<ServerPayload>):String {
      return lib.strSer.stringify(SerializeHelp.serverSayServerPayloadSerializer, ss)
    }

    fun fromBinClientSay(str:ByteArray):ClientSay<ClientPayload> {
      return lib.binnarySer.parse(SerializeHelp.clientSayClientPayloadSerializer, str)
    }

    fun toServerSayBin(ss:ServerSay<ServerPayload>):ByteArray {
      return lib.binnarySer.stringify(SerializeHelp.serverSayServerPayloadSerializer, ss)
    }

  }
}