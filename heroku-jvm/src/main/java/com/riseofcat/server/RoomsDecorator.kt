package com.riseofcat.server

import com.riseofcat.lib.*
import com.riseofcat.share.mass.PlayerId
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap

open class RoomsDecorator<TClientPayload,TServerPayload>:SesServ<TClientPayload,TServerPayload>() {
  val MAXIMUM_ROOM_PLAYERS = 50//todo если увеличивать количество игроков, то сильно лагает сервер на updateGame

  val onRoomCreated = Signal<RoomsDecorator<TClientPayload,TServerPayload>.Room>()
  //todo onRoomDestroyed
  private val rooms = HashSet<RoomsDecorator<TClientPayload,TServerPayload>.Room>()
  private val map = ConcurrentHashMap<Ses<TServerPayload>,Room>()

  override fun start(session:Ses<TServerPayload>) {
    var room:RoomsDecorator<TClientPayload,TServerPayload>.Room? = null
    synchronized(this) {
      for(r in rooms) {
        if(r.playersCount<MAXIMUM_ROOM_PLAYERS) {
          room = r
          break
        }
      }
      if(room==null) {
        lib.log.info("new room created")
        room = Room()
        rooms.add(room!!)
        onRoomCreated.dispatch(room!!)
      }
      room!!.add(session)
    }
    map[session] = room!!
  }

  override fun close(session:Ses<TServerPayload>) {
    val room = map.remove(session)
    room!!.remove(session)
  }

  override fun message(session:Ses<TServerPayload>,payload:TClientPayload) {
    val room = map[session]
    room!!.message(session,payload)
  }

  inner class Room {
    val createTime = lib.time
    val onPlayerAdded = Signal<Player>()
    val onPlayerRemoved = Signal<Player>()
    val onMessage = Signal<PlayerMessage>()
    private val players = ConcurrentHashMap<Ses<TServerPayload>,Player>()
    val playersCount:Int
      get() = players.size

    fun getPlayers():Collection<Player> {
      return players.values
    }

    fun add(session:Ses<TServerPayload>) {//todo private
      val player = Player(session)
      players[session] = player
      synchronized(this) {
        onPlayerAdded.dispatch(player)
      }
    }

    fun message(session:Ses<TServerPayload>,payload:TClientPayload) {//todo private
      onMessage.dispatch(PlayerMessage(players[session]!!,payload))
    }

    fun remove(session:Ses<TServerPayload>) {//todo private
      val remove = players.remove(session)
      synchronized(this) {
        onPlayerRemoved.dispatch(remove!!)
      }
    }

    inner class Player(val session:Ses<TServerPayload>) {
      val id:PlayerId
        get() = PlayerId(session.id)
    }
  }

  inner class PlayerMessage(val player:Room.Player,val payload:TClientPayload)

  class RoomHandler<TClientPayload,TServerPayload,E> {
    fun handleRoomCreated(room:RoomsDecorator<TClientPayload,TServerPayload>.Room) {

    }
  }
}
