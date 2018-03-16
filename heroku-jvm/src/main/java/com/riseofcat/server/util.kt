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
    fun fromJsonClientSay(reader:Reader):ClientSay<ClientPayload> {
      return Lib.objStrSer.parse(SerializeHelp.clientSayClientPayloadSerializer, reader.readText())
    }

    fun toServerSayJson(ss:ServerSay<ServerPayload>):String {
      return Lib.objStrSer.stringify(SerializeHelp.serverSayServerPayloadSerializer, ss)
    }

  }
}