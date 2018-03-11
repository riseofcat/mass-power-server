package com.riseofcat.share

import com.riseofcat.share.data.*
import kotlinx.serialization.*

@Serializable class ClientPayload(
  var tick:Int,
  @Optional var actions:MutableList<ClientAction>? = null //todo может по умолчанию сделать пустой массив?
) {
  @Serializable class ClientAction (
    var aid:Int,//Если действия будут отложены или не применимы то сервер сообщит по id-шнику. Рецеркулировать от 0 до 255
    var wait:Int,
    var tick:Int,//tick = payload.tick + wait//todo redundant
    var action:Action
  )
}
