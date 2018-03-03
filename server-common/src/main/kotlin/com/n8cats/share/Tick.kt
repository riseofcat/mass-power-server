package com.n8cats.share

//todo redundant. Replace with Integer
data class Tick(//do not use in JSON
  val tick:Int) {
  fun add(t:Int):Tick {
    return Tick(tick+t)
  }

}