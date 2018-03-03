package com.n8cats.share

import com.n8cats.share.data.*
import java.util.ArrayList

class ClientPayload {
  var tick:Int = 0
  var actions:ArrayList<ClientAction>? = null

  class ClientAction {
    var aid:Int = 0//Если действия будут отложены или не применимы то сервер сообщит по id-шнику. Рецеркулировать от 0 до 255
    var wait:Int = 0
    var tick:Int = 0//tick = payload.tick + wait//todo redundant
    var action:Action? = null
  }

}