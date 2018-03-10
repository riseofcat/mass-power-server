package com.riseofcat.share

import com.riseofcat.share.data.*
import kotlinx.serialization.*

@Serializable class TickActions(var tick:Int,var list:MutableList<BigAction>/*Порядок важен*/)

@Serializable class ServerPayload {
  var tick:Float = 0.toFloat()
  @Optional var welcome:Welcome? = null
  @Optional var stable:Stable? = null
  @Optional var actions:MutableList<TickActions>? = null
  @Optional var canceled:MutableSet<Int>? = null
  @Optional var apply:MutableList<AppliedActions>? = null
  @Optional var error:ServerError? = null

  @Serializable class Welcome(var id:PlayerId)
  @Serializable class AppliedActions(var aid:Int,var delay:Int)
  @Serializable class Stable {
    var tick:Int = 0//все actions уже пришли и новых больше не будет. Если кто-то кого-то убил, то в этом кадре засчитывается фраг. Но само убийство и набор очков мог произойти в прошлом
    @Optional var state:State? = null
  }
  @Serializable class ServerError {
    var code:Int = 0
    @Optional var message:String? = null
  }
}
