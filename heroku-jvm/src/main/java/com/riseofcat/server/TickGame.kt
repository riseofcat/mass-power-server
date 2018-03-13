package com.riseofcat.server

import com.riseofcat.common.*
import com.riseofcat.lib.*
import com.riseofcat.share.ShareTodo
import com.riseofcat.share.base.Tick
import com.riseofcat.share.mass.*

import java.util.ArrayList
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap

class TickGame(room:RoomsDecorator<ClientPayload,ServerPayload>.Room) {
  private val startTime = System.currentTimeMillis()
  private var previousActionsVersion = 0
  @Volatile private var tick = 0//todo volatile redundant? //todo float
  private val state = State()
  private val actions:DefaultValueMap<Tick,MutableList<Action>> = DefaultValueMap(Common.createConcurrentHashMap<Tick,MutableList<Action>>()) {mutableListOf()}
  private val mapPlayerVersion = ConcurrentHashMap<PlayerId,Int>()
  private val stableTick:Tick
    get() {
      val result = tick-GameConst.DELAY_TICKS+1
      return if(result<0) Tick(0) else Tick(result)
    }
  private val removeBeforeTick:Int
    get() = tick-GameConst.REMOVE_TICKS+1
  private val futureTick:Int
    get() = tick+GameConst.FUTURE_TICKS

  init {
    room.onPlayerAdded.add {player->
      synchronized(this@TickGame) {
        val d = 1
        actions.getExistsOrPutDefault(Tick(tick+d)).add(Action(++previousActionsVersion,NewCarAction(player.id).toBig()))
        val payload = createStablePayload()
        payload.welcome = Welcome(player.id)
        payload.actions = mutableListOf()
        for(entry in actions.map.entries) {
          val temp = ArrayList<BigAction>()
          for(a in entry.value) temp.add(a.pa)
          payload.actions!!.add(TickActions(entry.key.tick,temp))
        }
        player.session.send(payload)
        mapPlayerVersion.put(player.id,previousActionsVersion)
      }
      for(p in room.getPlayers()) if(p!=player) updatePlayer(p)//Говорим другим, что пришёл новый игрок
    }
    room.onMessage.add {message->
      synchronized(this@TickGame) {
        if(message.payload.actions!=null) {
          for(a in message.payload.actions!!) {
            val payload = ServerPayload(tick.toFloat())
            var delay = 0
            if(a.tick<stableTick.tick) {
              if(a.tick<removeBeforeTick) {
                payload.canceled = hashSetOf()
                payload.canceled!!.add(a.aid)
                message.player.session.send(payload)//todo move out of for
                continue
              } else
                delay = stableTick.tick-a.tick
            } else if(a.tick>futureTick) {
              payload.canceled = hashSetOf()
              payload.canceled!!.add(a.aid)
              message.player.session.send(payload)//todo move out of for
              continue
            }
            payload.apply = mutableListOf()
            payload.apply!!.add(AppliedActions(a.aid,delay))
            actions.getExistsOrPutDefault(Tick(a.tick+delay)).add(Action(++previousActionsVersion,PlayerAction(message.player.id,a.action).toBig()))
            if(ShareTodo.SIMPLIFY) updatePlayerInPayload(payload,message.player)
            message.player.session.send(payload)//todo move out of for
          }
        }
      }
      for(p in room.getPlayers()) if(p!=message.player) updatePlayer(p)

    }
    val timer = Timer()
    timer.schedule(object:TimerTask() {
      override fun run() {
        class Adapter(arr:List<Action>?):Iterator<InStateAction> {
          private var iterator:Iterator<Action>? = null

          init {
            if(arr!=null) iterator = arr.iterator()
          }

          override fun hasNext():Boolean {
            return iterator!=null&&iterator!!.hasNext()
          }

          override fun next():InStateAction {
            return iterator!!.next().pa
          }
        }
        while(System.currentTimeMillis()-startTime>tick*GameConst.UPDATE_MS) {
          synchronized(this@TickGame) {
            state.act(Adapter(actions.map[stableTick])).tick()
            this@TickGame.actions.map.remove(stableTick)
            ++tick
            if(tick%200==0) /*todo %*/ for(player in room.getPlayers()) player.session.send(createStablePayload())
          }
        }
      }
    },0,(GameConst.UPDATE_MS/2).toLong())
  }

  private fun updatePlayer(p:RoomsDecorator<ClientPayload,ServerPayload>.Room.Player) {
    val payload = ServerPayload(tick.toFloat())
    updatePlayerInPayload(payload,p)
    p.session.send(payload)
  }

  private fun updatePlayerInPayload(payload:ServerPayload,p:RoomsDecorator<ClientPayload,ServerPayload>.Room.Player) {
    payload.actions = mutableListOf()
    synchronized(this) {
      payload.tick = tick.toFloat()//todo redundant? but synchronized
      for(entry in actions.map.entries) {
        val temp = ArrayList<BigAction>()
        for(a in entry.value) if(ShareTodo.SIMPLIFY||a.pa.p==null||a.pa.p!!.id!=p.id) if(a.actionVersion>mapPlayerVersion[p.id]!!) temp.add(a.pa)
        if(temp.size>0) payload.actions!!.add(TickActions(entry.key.tick,temp))
      }
      mapPlayerVersion.put(p.id,previousActionsVersion)
    }
  }

  internal fun createStablePayload():ServerPayload {
    val result = ServerPayload(tick.toFloat())
    result.stable = Stable(stableTick.tick,state)
    return result
  }

  private class ConcreteRoomsServer:RoomsDecorator<ClientPayload,ServerPayload>()
  private inner class Action(var actionVersion:Int,var pa:BigAction)

  private fun todo() {
    val player:RoomsDecorator<ClientPayload,ServerPayload>.Room.Player? = null
    val startTime = player!!.session.get(UsageMonitorDecorator.Extra::class.java)!!.startTime
    val latency = player!!.session.get(PingDecorator.Extra::class.java)!!.latency
  }
}
