package com.riseofcat.share

import kotlinx.serialization.*

@Serializable class ClientSay<T> {
   @Optional var pong:Boolean = false
   @Optional var payload:T? = null
}
