package com.riseofcat.share

import kotlinx.serialization.*

object Share{
  val serverPayloadSerializer:KSerializer<ServerPayload> = ServerPayload.serializer()
  val serverSayServerPayloadSerializer:KSerializer<ServerSay<ServerPayload>> = ServerSay.serializer(serverPayloadSerializer)

  val clientPayloadSerializer:KSerializer<ClientPayload> = ClientPayload.serializer()
  val clientSayClientPayloadSerializer:KSerializer<ClientSay<ClientPayload>> = ClientSay.serializer(clientPayloadSerializer)
}