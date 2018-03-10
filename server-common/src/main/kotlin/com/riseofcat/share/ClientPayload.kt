package com.riseofcat.share

import com.riseofcat.share.data.*
import kotlinx.serialization.*

@Serializable class ClientPayload {
  var tick:Int = 0
  @Optional var actions:MutableList<ClientAction>? = null

  @Serializable class ClientAction {
    @Optional var aid:Int = 0//Если действия будут отложены или не применимы то сервер сообщит по id-шнику. Рецеркулировать от 0 до 255
    @Optional var wait:Int = 0
    @Optional var tick:Int = 0//tick = payload.tick + wait//todo redundant
    @Optional var action:Action? = null
  }

}
