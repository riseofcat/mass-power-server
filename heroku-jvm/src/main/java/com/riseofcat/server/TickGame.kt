package com.riseofcat.server

import com.riseofcat.lib.*
import com.riseofcat.share.mass.*
import java.util.*
import java.util.concurrent.*

class TickGame(room:RoomsDecorator<ClientPayload,ServerPayload>.Room) {
  private val startTime = System.currentTimeMillis()
  private var previousActionsVersion = 0
  @Volatile private var tick = Tick(0)//todo volatile redundant? //todo float
  private val state = State()
  private var actions:MutableList<Action> = mutableListOf()
  private val mapPlayerVersion = ConcurrentHashMap<PlayerId,Int>()
  private val stableTick get() = (tick-GameConst.DELAY_TICKS+1).let {if(it<Tick(0)) Tick(0) else it}
  private val removeBeforeTick:Tick get() = tick-GameConst.REMOVE_TICKS+1

  init {
    room.onPlayerAdded.add {player->
      synchronized(this@TickGame) {
        val d = Tick(1)
        actions.add(Action(++previousActionsVersion,tick+d,player.id,n = NewCarAction(player.id)))
        actions.sortBy {it.tick}
        val payload = createStablePayload(Welcome(player.id))
        for(a in actions) payload.actions.add(TickAction(a.tick,a.pid, n=a.n, p=a.pa))
        player.session.send(payload)
        mapPlayerVersion.put(player.id,previousActionsVersion)
      }
      for(p in room.getPlayers()) if(p!=player) updatePlayer(p)//Говорим другим, что пришёл новый игрок
    }
    room.onMessage.add {message->
      synchronized(this@TickGame) {
        for(a in message.payload.actions) {
          val payload = ServerPayload(tick.toDbl())
          var delay = Tick(0)
          if(a.tick<stableTick) {
            if(a.tick<removeBeforeTick) continue
            else delay = stableTick-a.tick
          }
          actions.add(Action(++previousActionsVersion,a.tick+delay, message.player.id, pa = PlayerAction(message.player.id,a.action)))
          updatePlayerInPayload(payload,message.player)
          message.player.session.send(payload)//todo move out of for
        }
      }
      for(p in room.getPlayers()) if(p!=message.player) updatePlayer(p)
    }
    val timer = Timer()
    timer.schedule(object:TimerTask() {
      override fun run() {
        while(System.currentTimeMillis()-startTime>tick.tick * GameConst.UPDATE_MS) {
          synchronized(this@TickGame) {
            state.act(actions.toList().filter {it.tick == stableTick}.iterator()).tick()//todo toList redundant
            actions.removeIf{it.tick == stableTick}//todo duplicate
            tick += 1
            if(true)if(tick.tick%200==0) for(player in room.getPlayers()) player.session.send(createStablePayload())
          }
        }
      }
    },0,(GameConst.UPDATE_MS/2).toLong())
  }

  private fun updatePlayer(p:RoomsDecorator<ClientPayload,ServerPayload>.Room.Player) {
    val payload = ServerPayload(tick.toDbl())
    updatePlayerInPayload(payload,p)
    p.session.send(payload)
  }

  private fun updatePlayerInPayload(payload:ServerPayload,p:RoomsDecorator<ClientPayload,ServerPayload>.Room.Player) {
    payload.actions = mutableListOf()
    synchronized(this) {
      payload.tick = tick.toDbl()//todo redundant? but synchronized
      for(a in actions) {
        if(a.actionVersion>mapPlayerVersion[p.id]?:Lib.Log.fatalError("unknowk id")) {
          payload.actions.add(TickAction(a.tick, a.pid, p = a.pa, n = a.n))
        }
      }
      mapPlayerVersion.put(p.id,previousActionsVersion)
    }
  }

  internal fun createStablePayload(welcome:Welcome?=null):ServerPayload {
    val result = ServerPayload(
      tick = tick.toDbl(),
      welcome =  welcome,
      stable = Stable(stableTick,state)
    )
    return result
  }

  private class ConcreteRoomsServer:RoomsDecorator<ClientPayload,ServerPayload>()
  private class Action(val actionVersion:Int, val tick:Tick, val pid:PlayerId, val pa:PlayerAction?=null, val n:NewCarAction?=null):InStateAction {
    override fun act(state:State) {
      pa?.act(state)
      n?.act(state)
    }

  }

  private fun todo() {
    val player:RoomsDecorator<ClientPayload,ServerPayload>.Room.Player? = null
    val startTime = player!!.session.get(UsageMonitorDecorator.Extra::class.java)!!.startTime
    val latency = player!!.session.get(PingDecorator.Extra::class.java)!!.latency
  }
}
