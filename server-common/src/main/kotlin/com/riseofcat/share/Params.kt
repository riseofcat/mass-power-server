package com.riseofcat.share

import com.riseofcat.share.data.*

object Params {
  val TITLE = "mass-power.io"
  val DEFAULT_LATENCY_MS = 250
  val DEFAULT_LATENCY_S = 250/1000f
  val DELAY_TICKS = DEFAULT_LATENCY_MS*3/Logic.UPDATE_MS+1//количество тиков для хранения действий //bigger delayed
  val REMOVE_TICKS = DELAY_TICKS*3//bigger removed
  val FUTURE_TICKS = DELAY_TICKS*3

}