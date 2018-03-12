package com.riseofcat.share.base

import kotlinx.serialization.*

@Serializable class ClientSay<T> {
   @Optional var pong:Boolean = false
   @Optional var payload:T? = null
}

@Serializable class ServerSay<T>(
   var payload:T) {
   @Optional var latency:Int? = null
   @Optional var ping:Boolean = false
}

@Serializable data class Tick(
  val tick:Int) {

  fun add(t:Int):Tick {//todo operator fun plus
    return Tick(tick+t)
  }

}