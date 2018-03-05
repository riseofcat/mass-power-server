package com.riseofcat.share.data

class BigAction:InStateAction {
  //todo redundant because Json serialization
  var n:NewCarAction? = null
  var p:PlayerAction? = null
  override fun act(state:State,getCar:GetCarById) {
    if(n!=null) n!!.act(state,getCar)
    if(p!=null) p!!.act(state,getCar)
  }
}
