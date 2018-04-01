package com.riseofcat.share.ping

import com.riseofcat.lib.*
import kotlinx.serialization.*

@Serializable class ClientSay<T>(
  @Optional val pong:Boolean = false,
  @Optional val payload:T? = null
)

@Serializable class ServerSay<T>(
  @Optional val payload:T? = null,
  @Optional val pingDelay:Duration? = null,
  @Optional val serverTime:TimeStamp? = null,
  @Optional val ping:Boolean = false
)
