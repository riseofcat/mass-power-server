package com.riseofcat.share

import com.riseofcat.share.data.*
import kotlinx.serialization.Serializable

@Serializable class ClientPayload {
  var tick:Int = 0
  var actions:MutableList<ClientAction>? = null

  @Serializable class ClientAction {
    var aid:Int = 0//Если действия будут отложены или не применимы то сервер сообщит по id-шнику. Рецеркулировать от 0 до 255
    var wait:Int = 0
    var tick:Int = 0//tick = payload.tick + wait//todo redundant
    var action:Action? = null
  }

}
