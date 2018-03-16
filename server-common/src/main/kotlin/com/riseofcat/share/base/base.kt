package com.riseofcat.share.base

import kotlinx.serialization.*

@Serializable class ClientSay<T>(
  @Optional var pong:Boolean = false,
  @Optional var payload:T? = null
)

@Serializable class ServerSay<T>(
  var payload:T,
  @Optional var latency:Int? = null,
  @Optional var ping:Boolean = false
)

@Serializable data class Tick(val tick:Int)

operator fun Tick.plus(i:Int) = Tick(tick+i)
operator fun Tick.plus(t:Tick) = Tick(tick+t.tick)
operator fun Tick.minus(t:Tick) = Tick(tick - t.tick)
operator fun Tick.compareTo(other:Tick) = tick - other.tick