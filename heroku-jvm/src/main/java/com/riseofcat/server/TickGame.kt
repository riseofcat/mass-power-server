package com.riseofcat.server

import com.riseofcat.client.*
import com.riseofcat.common.*
import com.riseofcat.lib.*
import com.riseofcat.share.mass.*

class TickGame(val room:RoomsDecorator<ClientPayload,ServerPayload>.Room) {
  val STABLE_STATE_UPDATE:Duration? = Duration(10_000)
  @Volatile private var previousActionsVersion = 0//todo почитать про volatile в Kotlin
  private val state = State()
  private var actions:MutableList<Action> = Common.createConcurrentList()
  private val mapPlayerVersion = Common.createConcurrentHashMap<PlayerId,Int>()
  private var previousStableUpdate:TimeStamp = lib.time

  val realtimeTick get() = Tick((lib.time - room.createTime)/GameConst.UPDATE)
  //todo averageLatency считать умнее
  val averageLatency get() = room.getPlayers().sumByDuration{it.session.get(PingDecorator.Extra::class.java)!!.lastPingDelay?: DEFAULT_LATENCY}/room.getPlayers().size
  val recommendedLatency get() = averageLatency*2
  val recommendedDelay get() = Tick(recommendedLatency/GameConst.UPDATE + 1)
  val maxDelay get() = recommendedDelay*2
  val newCarDelay get() = recommendedDelay*2
  val removeAfterDelay get() = recommendedDelay*3//0 - значит всё что позже stable - удаляется

  init {
    room.onPlayerAdded.add {player->
      updateGame()
      redundantSynchronize(this@TickGame) {
        actions.add(Action(++previousActionsVersion,TickAction(realtimeTick+newCarDelay,player.id,n = NewCarAction(player.id))))
        val payload = createStablePayload(Welcome(player.id, room.createTime))
        player.session.send(payload)
      }
      for(p in room.getPlayers()) updatePlayer(p)//Говорим другим, что пришёл новый игрок
    }
    room.onMessage.add {message->
      updateGame()
      redundantSynchronize(this@TickGame) {
        for(a in message.payload.actions) {
          val payload = ServerPayload(recommendedLatency = recommendedLatency)
          var delay = Tick(0)
          if(a.tick<state.tick) {
            if(a.tick <= state.tick-removeAfterDelay) continue
            delay = state.tick-a.tick//задерживаем
          }
          actions.add(Action(++previousActionsVersion,TickAction(a.tick+delay, message.player.id, p = PlayerAction(message.player.id,a.action))))
          updatePlayerInPayload(payload,message.player)
          message.player.session.send(payload)//todo move out of for
        }
      }
      for(p in room.getPlayers()) if(p!=message.player) updatePlayer(p)
    }
  }

  private fun updateGame() {
    fun condition() = state.tick<realtimeTick-maxDelay
    if(!condition()) return

    val test = true//todo test performance
    synchronized(this@TickGame) {
      while(condition()) {
        state act actions.map {it.ta}.filter {it.tick==state.tick}.iterator()
        if(!test)actions.removeAll {it.ta.tick==state.tick}
        state.tick()
      }
      if(test)actions.removeAll {it.ta.tick<state.tick}

      if(STABLE_STATE_UPDATE!=null&&lib.time>previousStableUpdate+STABLE_STATE_UPDATE) {
        previousStableUpdate = lib.time
        for(player in room.getPlayers()) player.session.send(createStablePayload())
      }
    }
  }

  private fun updatePlayer(p:RoomsDecorator<ClientPayload,ServerPayload>.Room.Player) {
    val payload = ServerPayload()
    updatePlayerInPayload(payload,p)
    p.session.send(payload)
  }

  private fun updatePlayerInPayload(payload:ServerPayload,p:RoomsDecorator<ClientPayload,ServerPayload>.Room.Player) {
    redundantSynchronize(this) {
      val filtered = actions.filter {it.actionVersion>mapPlayerVersion[p.id] ?: 0}
      payload.actions = filtered.map {it.ta}
      val maxActionVer = filtered.map {it.actionVersion}.max()
      if(maxActionVer != null) mapPlayerVersion.put(p.id,maxActionVer)
    }
  }

  internal fun createStablePayload(welcome:Welcome?=null):ServerPayload = ServerPayload(
    welcome =  welcome,
    stable = state,
    recommendedLatency = recommendedLatency
  )
}

inline fun <R> redundantSynchronize(lock:Any,block:()->R) = if(true) block() else synchronized(lock,block) //todo test performance
private class Action(val actionVersion:Int, val ta:TickAction)

