package com.riseofcat.share.ping

import com.riseofcat.lib.*
import kotlinx.serialization.*

@Serializable class ClientSay<T>(
  @Optional val pong:Boolean = false,
  @Optional val payload:T? = null,
  @Optional var index:Int? = 0//todo написать проверку последовательности принятия сообщений в PingDecorator
)

@Serializable class ServerSay<T>(
  @Optional val payload:T? = null,
  @Optional val pingDelay:Duration? = null,
  @Optional val serverTime:TimeStamp? = null,
  @Optional val ping:Boolean = false
)
