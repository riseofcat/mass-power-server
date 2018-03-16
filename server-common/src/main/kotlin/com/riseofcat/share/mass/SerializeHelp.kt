package com.riseofcat.share.mass

import com.riseofcat.share.ping.*
import kotlinx.serialization.*

object SerializeHelp {
  val serverPayloadSerializer:KSerializer<ServerPayload> = ServerPayload.serializer()
  val serverSayServerPayloadSerializer:KSerializer<ServerSay<ServerPayload>> = ServerSay.serializer(serverPayloadSerializer)

  val clientPayloadSerializer:KSerializer<ClientPayload> = ClientPayload.serializer()
  val clientSayClientPayloadSerializer:KSerializer<ClientSay<ClientPayload>> = ClientSay.serializer(clientPayloadSerializer)
}