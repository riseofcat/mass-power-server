package com.riseofcat.share.data

import kotlinx.serialization.*

@Serializable class BigAction:InStateAction {
  @Optional var n:NewCarAction? = null
  @Optional var p:PlayerAction? = null

  override fun act(state:State,getCar:GetCarById) {
    n?.act(state,getCar)
    p?.act(state,getCar)
  }
}
