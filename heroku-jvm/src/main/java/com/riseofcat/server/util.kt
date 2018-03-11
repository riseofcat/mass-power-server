package com.riseofcat.server

import com.riseofcat.lib.*
import com.riseofcat.share.*
import java.io.*

class Util{
  companion object {
    fun fromJsonClientSay(reader:Reader):ClientSay<ClientPayload> {
      return Lib.objStrSer.parse(Share.clientSayClientPayloadSerializer, reader.readText())
    }

    fun toServerSayJson(ss:ServerSay<ServerPayload>):String {
      return Lib.objStrSer.stringify(Share.serverSayServerPayloadSerializer, ss)
    }

  }
}