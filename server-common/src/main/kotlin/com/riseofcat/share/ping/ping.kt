package com.riseofcat.share.ping

import com.riseofcat.lib.*
import kotlinx.serialization.*
//todo val
@Serializable class ClientSay<T>(
  @Optional var pong:Boolean = false,
  @Optional var payload:T? = null
)

@Serializable class ServerSay<T>(
  @Optional val payload:T? = null,
  @Optional val sync:TimeSync? = null,
  @Optional val ping:Boolean = false
)

@Serializable class TimeSync(
  val latency:Duration,
  val serverTime:Timestamp
)