package com.riseofcat.share

import kotlinx.serialization.*

@Serializable class ServerSay<T>(
   var ping:Boolean = false,
   var latency:Int? = null,
   var payload:T? = null)

