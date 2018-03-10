package com.riseofcat.server

import com.riseofcat.share.*
import kotlinx.serialization.json.*
import java.io.*

class Util{
  companion object {
    fun fromJsonClientSay(reader:Reader):ClientSay<ClientPayload> {
      return JSON.parse(Share.clientSayClientPayloadSerializer, reader.readText())
    }

    fun toServerSayJson(ss:ServerSay<ServerPayload>):String {
      return JSON.stringify(Share.serverSayServerPayloadSerializer, ss)
    }

  }
}