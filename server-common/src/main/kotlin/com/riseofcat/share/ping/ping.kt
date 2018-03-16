package com.riseofcat.share.ping

import kotlinx.serialization.*
//todo val
@Serializable class ClientSay<T>(
  @Optional var pong:Boolean = false,
  @Optional var payload:T? = null
)

@Serializable class ServerSay<T>(
  var payload:T,
  @Optional var latency:Int? = null,
  @Optional var ping:Boolean = false
)