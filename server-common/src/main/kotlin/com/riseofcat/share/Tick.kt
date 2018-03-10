package com.riseofcat.share

import kotlinx.serialization.Serializable

@Serializable data class Tick(
  val tick:Int) {

  fun add(t:Int):Tick {//todo operator fun plus
    return Tick(tick+t)
  }

}