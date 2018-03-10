package com.riseofcat.share

import kotlinx.serialization.*

@Serializable open class ServerSay<T>(
  @Optional var ping:Boolean = false,
  @Optional var latency:Int? = null,
  @Optional var payload:T? = null)

