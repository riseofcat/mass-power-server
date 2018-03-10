package com.riseofcat.share

import kotlinx.serialization.Serializable

//todo redundant. Replace with Integer
@Serializable data class Tick(//do not use in JSON
  val tick:Int) {
  fun add(t:Int):Tick {
    return Tick(tick+t)
  }

}