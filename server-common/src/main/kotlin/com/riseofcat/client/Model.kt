package com.riseofcat.client

import com.riseofcat.common.*
import com.riseofcat.lib.*
import com.riseofcat.share.mass.*

class Model(conf:Conf) {
  val CACHE = true
  val client:PingClient<ServerPayload,ClientPayload> = PingClient(conf.host,conf.port,"socket",SerializeHelp.serverSayServerPayloadSerializer,SerializeHelp.clientSayClientPayloadSerializer)
  private val actions:MutableList<TickAction> = Common.createConcurrentList()
  private val myLocal:MutableList<TickAction> = mutableListOf()
  private var stable:StateWrapper = StateWrapper(State())
  private var sync:Sync? = null
  val playerName get() = welcome?.id?.let {"Player $it"} ?: "Wait connection..."
  var welcome:Welcome?=null//todo lateinit?  Может сделать что если приходит новый Welcome, то игрока перевели в другую комнату

  init {
    client.connect {s:ServerPayload->
      synchronized(this) {
        if(s.welcome!=null) welcome = s.welcome
        welcome?.run {
          val tick = Tick((client.serverTime-roomCreate)/GameConst.UPDATE)
          sync = Sync(tick,sync)
        }
        if(s.stable!=null) {
          stable = StateWrapper(s.stable.state)
          clearCache()
        }
        actions.addAll(s.actions)
        actions.sortBy {it.tick}

        val myMaxApplyTick:Tick = s.actions.filter {it.pid==welcome?.id}.map {it.tick}.max()?:Tick(0)//Последний tick который принял сервер от этого игрока
        myLocal.removeAll {it.tick <= myMaxApplyTick}
        val serverMinApplyTick:Tick = s.actions.map{it.tick}.min()?:Tick(0)
        clearCache(serverMinApplyTick)
      }
    }
  }

  fun calcDisplayState():State? = sync?.let {getState(it.calcClientTck())}
  fun ready() = welcome!=null

  fun action(action:com.riseofcat.share.mass.Action) {
    synchronized(this) {
      val clientTick = sync!!.calcClientTck()
      if(!ready()) return
      val wait = Tick((client.smartLatency.s/GameConst.UPDATE_S+1).toInt())//todo delta serverTick-clientTick
      val a = ClientPayload.ClientAction(
        tick = clientTick+wait,//todo serverTick?
        action = action
      )
      synchronized(myLocal) {
        welcome?.run {myLocal.add(TickAction(clientTick+wait, id, p = PlayerAction(id, a.action)))}
        myLocal.sortBy {it.tick}
      }
      client.say(ClientPayload(mutableListOf(a)))
    }
  }

  fun touch(pos:XY) {//todo move out?
    val displayState = calcDisplayState()
    if(displayState==null||welcome==null) return
    for((owner,_,_,pos1) in displayState.cars) {
      if(welcome?.id==owner) {
        val direction = (pos - pos1).calcAngle() + degreesAngle(0*180)
        action(Action(direction))
        break
      }
    }
  }
  fun dispose() { client.close() }

  private var cache:StateWrapper? = null
  private fun clearCache(tick:Tick = Tick(0)) = cache?.let {if(tick<=it._state.tick) cache = null}
  private fun saveCache(value:StateWrapper) { cache = value }
  private fun getNearestCache(tick:Tick):StateWrapper? =
    if(CACHE) {
      cache?.let {if(tick>=it._state.tick) it else null}
    }
    else {
      null
    }
  private fun getState(tick:Tick):State? {
    var result = getNearestCache(tick)
    if(result==null) {
      synchronized(this) {
        result = StateWrapper(stable)
        saveCache(result!!)
      }
    }
    result!!.tick(tick)
    return result!!._state
  }

  private inner class StateWrapper(state:State) {
    val _state = state.deepCopy()
    constructor(obj:StateWrapper):this(obj._state)
    fun tick(targetTick:Tick) {
      while(_state.tick<targetTick) {
        val filtered = (actions+myLocal).filter {it.tick==_state.tick}
        val size = filtered.size
        if(size > 0) {
          lib.log.breakpoint("size > 0")
          if(filtered.any{it.p != null}) {
            lib.log.breakpoint("p != null")
          }
        }
        _state act filtered.iterator()
        _state.tick()
      }
    }
  }
}

private class Sync(internal val serverTick:Tick,oldSync:Sync?) {
  internal val clientTick:Tick
  internal val time:TimeStamp
  init {
    time = lib.time
    clientTick = if(oldSync==null) serverTick else oldSync.calcClientTck()
  }
  private fun calcSrvTck():Tick {
    val duration:Duration = lib.time-time
    return serverTick+duration/GameConst.UPDATE
  }
  fun calcClientTck():Tick {
    return calcSrvTck()+(clientTick-serverTick)*(1.0-lib.Fun.arg0toInf(lib.time-time,Duration(600)))
  }
}
