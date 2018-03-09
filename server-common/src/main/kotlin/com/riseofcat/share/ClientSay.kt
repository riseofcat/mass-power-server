package com.riseofcat.share

import kotlinx.serialization.Serializable

@Serializable open class ClientSay<T> {
  var pong:Boolean = false
  var payload:T? = null
}
