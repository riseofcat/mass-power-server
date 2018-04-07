package com.riseofcat.server

import com.riseofcat.lib.*
import com.riseofcat.share.ping.ClientSay
import com.riseofcat.share.ping.ServerSay
import com.riseofcat.share.mass.ClientPayload
import com.riseofcat.share.mass.ServerPayload
import com.riseofcat.share.mass.SerializeHelp
import java.io.*

class Util{
  companion object {
    fun fromJsonClientSay(str:String):ClientSay<ClientPayload> {
      return lib.objStrSer.parse(SerializeHelp.clientSayClientPayloadSerializer, str)
    }

    fun toServerSayJson(ss:ServerSay<ServerPayload>):String {
      return lib.objStrSer.stringify(SerializeHelp.serverSayServerPayloadSerializer, ss)
    }

  }
}