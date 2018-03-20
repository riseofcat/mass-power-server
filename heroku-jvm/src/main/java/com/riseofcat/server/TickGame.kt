package com.riseofcat.server

import com.riseofcat.lib.*
import com.riseofcat.share.mass.*
import java.util.*
import java.util.concurrent.*

class TickGame(room:RoomsDecorator<ClientPayload,ServerPayload>.Room) {
  val STABLE_STATE_SYNC_TICKS:Int? = 500
  private val startTime = lib.time
  private var previousActionsVersion = 0
  private val tick get() = state.tick + GameConst.DELAY_TICKS
  private val state = State()
  private var actions:MutableList<Action> = mutableListOf()
  private val mapPlayerVersion = ConcurrentHashMap<PlayerId,Int>()

  init {
    room.onPlayerAdded.add {player->
      synchronized(this@TickGame) {
        actions.add(Action(++previousActionsVersion,TickAction(tick+GameConst.NEW_CAR_DELAY,player.id,n = NewCarAction(player.id))))
        actions.sortBy {it.ta.tick}
        val payload = createStablePayload(Welcome(player.id, startTime))
        payload.actions = actions.map{it.ta}
        player.session.send(payload)
        mapPlayerVersion.put(player.id,previousActionsVersion)
      }
      for(p in room.getPlayers()) if(p!=player) updatePlayer(p)//Говорим другим, что пришёл новый игрок
    }
    room.onMessage.add {message->
      synchronized(this@TickGame) {
        for(a in message.payload.actions) {
          val payload = ServerPayload(tick.toDbl())
          var delay = Tick(0)//todo redundant
          if(a.tick.intTick()<state.tick) {
            if(a.tick.intTick()>state.tick-GameConst.REMOVE_TICKS) {
              delay = state.tick-a.tick.intTick()
            }
          }
          actions.add(Action(++previousActionsVersion,TickAction(a.tick.intTick()+delay, message.player.id, p = PlayerAction(message.player.id,a.action))))
          updatePlayerInPayload(payload,message.player)
          message.player.session.send(payload)//todo move out of for
        }
      }
      for(p in room.getPlayers()) if(p!=message.player) updatePlayer(p)
    }
    val timer = Timer()
    timer.schedule(object:TimerTask() {//todo можно обойтись без таймера. Делать тики только при запросах с клиента. Тогда и проще будет с синхронизацией
      override fun run() {
        while(lib.time-startTime>GameConst.UPDATE * tick.tick) {
          synchronized(this@TickGame) {
            state act actions.map{it.ta}.filter {it.tick == state.tick}.iterator()
            actions.removeAll{it.ta.tick == state.tick}
            state.tick()
            if(STABLE_STATE_SYNC_TICKS != null && tick.tick%STABLE_STATE_SYNC_TICKS==0) for(player in room.getPlayers()) player.session.send(createStablePayload())
          }
        }
      }
    },0,GameConst.UPDATE.ms/2)
  }

  private fun updatePlayer(p:RoomsDecorator<ClientPayload,ServerPayload>.Room.Player) {
    val payload = ServerPayload(tick.toDbl())
    updatePlayerInPayload(payload,p)
    p.session.send(payload)
  }

  private fun updatePlayerInPayload(payload:ServerPayload,p:RoomsDecorator<ClientPayload,ServerPayload>.Room.Player) {
    synchronized(this) {
      payload.tick = tick.toDbl()//todo redundant? but synchronized
      payload.actions = actions
        .filter {it.actionVersion>mapPlayerVersion[p.id]?:lib.log.fatalError("unknown id")}
        .map {it.ta}
      mapPlayerVersion.put(p.id,previousActionsVersion)
    }
  }

  internal fun createStablePayload(welcome:Welcome?=null):ServerPayload {
    val result = ServerPayload(
      tick = tick.toDbl(),
      welcome =  welcome,
      stable = Stable(state.tick,state)
    )
    return result
  }

  private class Action(val actionVersion:Int, val ta:TickAction)

  private fun todo() {//todo move to service
    val player:RoomsDecorator<ClientPayload,ServerPayload>.Room.Player? = null
    val startTime = player!!.session.get(UsageMonitorDecorator.Extra::class.java)!!.startTime
    val latency = player!!.session.get(PingDecorator.Extra::class.java)!!.lastLatency
  }
}
