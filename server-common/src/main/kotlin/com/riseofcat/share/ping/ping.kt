package com.riseofcat.share.ping

import com.riseofcat.lib.*
import kotlinx.serialization.*

@Serializable class ClientSay<T>(
  @Optional val pong:Boolean = false,
  @Optional val payload:T? = null
)

@Serializable class ServerSay<T>(
  @Optional val payload:T? = null,
  @Optional val latency:Duration? = null,
  @Optional val ping:Boolean = false,
  @Optional val welcome:Welcome? = null
)

@Serializable data class Welcome(
  val serverTime:TimeStamp
)