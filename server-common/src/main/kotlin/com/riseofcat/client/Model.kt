package com.riseofcat.client

import com.riseofcat.common.*
import com.riseofcat.lib.*
import com.riseofcat.share.mass.*
import kotlin.system.*


class Model(conf:Conf) {
  val client:PingClient<ServerPayload,ClientPayload>
  var playerId:PlayerId? = null
  private val actions:MutableList<TickAction> = Common.createConcurrentList()
  private val myActions:MutableList<TickAction> = mutableListOf()
  private var stable:StateWrapper? = null
  private var sync:Sync? = null
  val playerName:String get() = playerId?.let {"Player $it"} ?: "Wait connection..."
  private var cache:StateWrapper? = null

  init {
    client = PingClient(conf.host,conf.port,"socket",SerializeHelp.serverSayServerPayloadSerializer,SerializeHelp.clientSayClientPayloadSerializer)
    client.connect {s:ServerPayload->
      synchronized(this) {
        sync = Sync(s.tick.double+client.smartLatencyS/GameConst.UPDATE_S,sync)
        if(s.welcome!=null) playerId = s.welcome.id
        if(s.stable!=null) {
          stable = StateWrapper(s.stable.state,s.stable.tick)
          clearCache(s.stable.tick)
        }
        actions.addAll(s.actions)
        actions.sortBy {it.tick}

        val currentUserActions = s.actions.filter {it.pid==playerId}.map {it.tick}
        val serverMaxApplyTick:Tick = currentUserActions.max()?:Tick(-1)//Последний tick который принял сервер от этого игрока
        myActions.removeAll {it.tick <= serverMaxApplyTick}
        val serverMinApplyTick:Tick = currentUserActions.min()?:Tick(-1)//Последний tick который принял сервер от этого игрока
        if(false) clearCache(serverMinApplyTick+1)
      }
    }
  }

  fun calcDisplayState():State? = sync?.let {getState(Tick(it.calcClientTck().toInt()))}

  fun ready():Boolean {
    return playerId!=null
  }

  fun action(action:com.riseofcat.share.mass.Action) {
    synchronized(this) {
      val clientTick = Tick(sync!!.calcClientTck().toInt())//todo +0.5f?
      if(!ready()) return
      val w = (client.smartLatencyS/GameConst.UPDATE_S+1).toInt()//todo delta serverTick-clientTick
      val a = ClientPayload.ClientAction(
        tick = clientTick+w,//todo serverTick?
        action = action
      )
      synchronized(myActions) {
        playerId?.let {myActions.add(TickAction(clientTick+w, it, p = PlayerAction(it, a.action)))}
        myActions.sortBy {it.tick}
      }
      val payload = ClientPayload(
        tick = clientTick.toDbl(),
        actions = mutableListOf(a)
      )
      client.say(payload)
    }
  }

  fun touch(pos:XY) {//todo move out?
    val displayState = calcDisplayState()
    if(displayState==null||playerId==null) return
    for((owner,_,_,pos1) in displayState.cars) {
      if(playerId==owner) {
        val direction = (pos - pos1).calcAngle() + degreesAngle(0*180)
        action(Action(direction))
        break
      }
    }
  }

  private fun clearCache(tick:Tick) {
    if(cache!=null&&tick<cache!!.tick) cache = null
  }

  private fun getNearestCache(tick:Tick) = if(cache!=null&&cache!!.tick<=tick) cache else null

  private fun saveCache(value:StateWrapper) {
    cache = value
  }

  private fun getState(tick:Tick):State? {
    var result = getNearestCache(tick)
    if(result==null) {
      if(stable==null) return null
      synchronized(this) {
        result = StateWrapper(stable!!)
        saveCache(result!!)
      }
    }
    result!!.tick(tick)
    return result!!.state
  }

  fun dispose() {
    client.close()
  }

  var averageTickNanos = 0f
  private inner class StateWrapper(var state:State,var tick:Tick) {
    constructor(obj:StateWrapper):this(obj.state.copy(),obj.tick)
    fun tick(targetTick:Tick) {
      while(tick<targetTick) {
        val iterator = (actions+myActions)
          .filter {it.tick==tick}
          .iterator()
        state.act(iterator)

        val FRAMES = 20
        measureNanoTime{state.tick()}.let{averageTickNanos = (averageTickNanos*FRAMES + it) / (FRAMES+1)}
        tick+=1
      }
    }
  }
}

private class Sync(internal val serverTick:Double,oldSync:Sync?) {
  internal val clientTick:Double
  internal val time:Long
  init {
    time = Common.timeMs
    clientTick = if(oldSync==null) serverTick else oldSync.calcClientTck()
  }
  private fun calcSrvTck(t:Long) = serverTick+(t-time)/GameConst.UPDATE_MS
  private fun calcSrvTck() = calcSrvTck(Common.timeMs)
  fun calcClientTck() = Common.timeMs.let {calcSrvTck(it)+(clientTick-serverTick)*(1f-Lib.Fun.arg0toInf((it-time).toDouble(),600f))}
}
