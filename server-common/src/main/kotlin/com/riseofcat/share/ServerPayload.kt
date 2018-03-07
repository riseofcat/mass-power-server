package com.riseofcat.share

import com.riseofcat.share.data.*

class TickActions(var tick:Int,var list:MutableList<BigAction>/*Порядок важен*/)

class ServerPayload {
  var tick:Float = 0.toFloat()
  var welcome:Welcome? = null
  var stable:Stable? = null
  var actions:MutableList<TickActions>? = null
  var canceled:MutableSet<Int>? = null
  var apply:MutableList<AppliedActions>? = null
  var error:ServerError? = null

  class Welcome {
    var id:PlayerId? = null
  }

  class AppliedActions(var aid:Int,var delay:Int)

  class Stable {
    var tick:Int = 0//все actions уже пришли и новых больше не будет. Если кто-то кого-то убил, то в этом кадре засчитывается фраг. Но само убийство и набор очков мог произойти в прошлом
    var state:State? = null
  }

  class ServerError {
    var code:Int = 0
    var message:String? = null
  }

}
