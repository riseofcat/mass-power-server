package com.riseofcat.share

import kotlinx.serialization.Serializable

@Serializable open class ServerSay<T>(
  var ping:Boolean = false,
  var latency:Int? = null,
  var payload:T? = null)

