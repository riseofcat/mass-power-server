package com.riseofcat.share

import kotlinx.serialization.*

@Serializable class ServerSay<T>(
   var payload:T) {
   @Optional var latency:Int? = null
   @Optional var ping:Boolean = false
}

