package com.riseofcat.server

import com.riseofcat.lib.*
import com.riseofcat.share.*
import java.io.*

class Util{
  companion object {
    fun fromJsonClientSay(reader:Reader):ClientSay<ClientPayload> {
      return Lib.json.parse(Share.clientSayClientPayloadSerializer, reader.readText())
    }

    fun toServerSayJson(ss:ServerSay<ServerPayload>):String {
      return Lib.json.stringify(Share.serverSayServerPayloadSerializer, ss)
    }

  }
}