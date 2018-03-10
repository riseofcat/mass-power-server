package com.riseofcat.share

import com.riseofcat.share.data.*
import kotlinx.serialization.*

@Serializable class TickActions(var tick:Int,var list:MutableList<BigAction>/*Порядок важен*/)

@Serializable class ServerPayload {
  var tick:Float = 0.toFloat()
   var welcome:Welcome? = null
   var stable:Stable? = null
   var actions:MutableList<TickActions>? = null
   var canceled:MutableSet<Int>? = null
   var apply:MutableList<AppliedActions>? = null
   var error:ServerError? = null

  @Serializable class Welcome(var id:PlayerId)
  @Serializable class AppliedActions(var aid:Int,var delay:Int)
  @Serializable class Stable {
    var tick:Int = 0//все actions уже пришли и новых больше не будет. Если кто-то кого-то убил, то в этом кадре засчитывается фраг. Но само убийство и набор очков мог произойти в прошлом
     var state:State? = null
  }
  @Serializable class ServerError {
    var code:Int = 0
     var message:String? = null
  }
}
