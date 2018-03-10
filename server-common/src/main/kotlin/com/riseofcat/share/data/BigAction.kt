package com.riseofcat.share.data

import kotlinx.serialization.*

@Serializable class BigAction(
  ):InStateAction {
   var n:NewCarAction? = null
   var p:PlayerAction? = null

  override fun act(state:State,getCar:GetCarById) {
    if(n!=null) n!!.act(state,getCar)
    if(p!=null) p!!.act(state,getCar)
  }
}
