package com.riseofcat.server

import com.riseofcat.lib.*
import com.riseofcat.share.mass.PlayerId
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap

open class RoomsDecorator<TClientPayload,TServerPayload>:SesServ<TClientPayload,TServerPayload> {
  val onRoomCreated = Signal<RoomsDecorator<TClientPayload,TServerPayload>.Room>()
  //todo onRoomDestroyed
  private val rooms = HashSet<RoomsDecorator<TClientPayload,TServerPayload>.Room>()
  private val map = ConcurrentHashMap<SesServ<TClientPayload,TServerPayload>.Ses,Room>()

  constructor() {}
  constructor(roomCreatedListener:Signal.Listener<RoomsDecorator<TClientPayload,TServerPayload>.Room>) {
    onRoomCreated.add(roomCreatedListener)
  }

  override fun start(session:SesServ<TClientPayload,TServerPayload>.Ses) {
    var room:RoomsDecorator<TClientPayload,TServerPayload>.Room? = null
    synchronized(this) {
      for(r in rooms) {
        if(r.playersCount<MAXIMUM_ROOM_PLAYERS) {
          room = r
          break
        }
      }
      if(room==null) {
        Lib.Log.info("new room created")
        room = Room()
        rooms.add(room!!)
        onRoomCreated.dispatch(room!!)
      }
      room!!.add(session)
    }
    map[session] = room!!
  }

  override fun close(session:SesServ<TClientPayload,TServerPayload>.Ses) {
    val room = map.remove(session)
    room!!.remove(session)
  }

  override fun message(session:SesServ<TClientPayload,TServerPayload>.Ses,payload:TClientPayload) {
    val room = map[session]
    room!!.message(session,payload)
  }

  inner class Room {
    val onPlayerAdded = Signal<Player>()
    val onPlayerRemoved = Signal<Player>()
    val onMessage = Signal<PlayerMessage>()
    private val players = ConcurrentHashMap<SesServ<TClientPayload,TServerPayload>.Ses,Player>()
    val playersCount:Int
      get() = players.size

    fun getPlayers():Collection<Player> {
      return players.values
    }

    fun add(session:SesServ<TClientPayload,TServerPayload>.Ses) {//todo private
      val player = Player(session)
      players[session] = player
      synchronized(this) {
        onPlayerAdded.dispatch(player)
      }
    }

    fun message(session:SesServ<TClientPayload,TServerPayload>.Ses,payload:TClientPayload) {//todo private
      onMessage.dispatch(PlayerMessage(players[session]!!,payload))
    }

    fun remove(session:SesServ<TClientPayload,TServerPayload>.Ses) {//todo private
      val remove = players.remove(session)
      synchronized(this) {
        onPlayerRemoved.dispatch(remove!!)
      }
    }

    inner class Player(val session:SesServ<TClientPayload,TServerPayload>.Ses) {
      val id:PlayerId
        get() = PlayerId(session.id)
    }
  }

  inner class PlayerMessage(val player:Room.Player,val payload:TClientPayload)

  class RoomHandler<TClientPayload,TServerPayload,E> {
    fun handleRoomCreated(room:RoomsDecorator<TClientPayload,TServerPayload>.Room) {

    }
  }

  companion object {
    val MAXIMUM_ROOM_PLAYERS = 5
  }
}
