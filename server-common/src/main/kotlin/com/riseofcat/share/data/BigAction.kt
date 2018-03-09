package com.riseofcat.share.data

import kotlinx.serialization.Serializable

/*@Serializable*/ class BigAction(
  var n:NewCarAction? = null,
  var p:PlayerAction? = null):InStateAction {
  //todo redundant because Json serialization
  override fun act(state:State,getCar:GetCarById) {
    if(n!=null) n!!.act(state,getCar)
    if(p!=null) p!!.act(state,getCar)
  }
}
