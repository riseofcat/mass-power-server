package com.riseofcat.share.ping

import kotlinx.serialization.*
//todo val
@Serializable class ClientSay<T>(
  @Optional var pong:Boolean = false,
  @Optional var payload:T? = null
)

@Serializable class ServerSay<T>(
  @Optional val payload:T? = null,
  @Optional val latency:Int? = null,
  @Optional val ping:Boolean = false
)