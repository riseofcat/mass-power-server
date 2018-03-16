package com.riseofcat.share.ping

import kotlinx.serialization.*

@Serializable class ClientSay<T>(
  @Optional var pong:Boolean = false,
  @Optional var payload:T? = null
)

@Serializable class ServerSay<T>(
  var payload:T,
  @Optional var latency:Int? = null,
  @Optional var ping:Boolean = false
)