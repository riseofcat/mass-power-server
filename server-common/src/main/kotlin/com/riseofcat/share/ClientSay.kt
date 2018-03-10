package com.riseofcat.share

import kotlinx.serialization.*

@Serializable class ClientSay<T> {
   var pong:Boolean = false
   var payload:T? = null
}
