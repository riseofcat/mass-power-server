package com.riseofcat.client

import com.riseofcat.common.*
import com.riseofcat.lib.*
import com.riseofcat.share.mass.*

class Model(conf:Conf) {
  val CACHE = true
  val client:PingClient<ServerPayload,ClientPayload> = PingClient(conf.host,conf.port,"socket",SerializeHelp.serverSayServerPayloadSerializer,SerializeHelp.clientSayClientPayloadSerializer)
  var playerId:PlayerId? = null
  private val actions:MutableList<TickAction> = Common.createConcurrentList()
  private val myLocal:MutableList<TickAction> = mutableListOf()
  private var stable:StateWrapper? = null//todo StateWrapper(State(), Tick(0))//Сделать default state чтобы играть offline до появления интернета или пока сервак не отвечает
  private var sync:Sync? = null
  val playerName get() = playerId?.let {"Player $it"} ?: "Wait connection..."

  init {
    client.connect {s:ServerPayload->
      synchronized(this) {
        sync = Sync(s.tick.double+client.smartLatencyS/GameConst.UPDATE_S,sync)
        if(s.welcome!=null) playerId = s.welcome.id
        if(s.stable!=null) {
          stable = StateWrapper(s.stable.state)
          clearCache()//todo ? clearCache(s.stable.tick)
        }
        actions.addAll(s.actions)
        actions.sortBy {it.tick}

        val myMaxApplyTick:Tick = s.actions.filter {it.pid==playerId}.map {it.tick}.max()?:Tick(0)//Последний tick который принял сервер от этого игрока
        myLocal.removeAll {it.tick <= myMaxApplyTick}
        val serverMinApplyTick:Tick = s.actions.map{it.tick}.min()?:Tick(0)
        clearCache(serverMinApplyTick)
      }
    }
  }

  fun calcDisplayState():State? = sync?.let {getState(Tick(it.calcClientTck().toInt()))}
  fun ready() = playerId!=null

  fun action(action:com.riseofcat.share.mass.Action) {
    synchronized(this) {
      val clientTick = Tick(sync!!.calcClientTck().toInt())//todo +0.5f?
      if(!ready()) return
      val w = (client.smartLatencyS/GameConst.UPDATE_S+1).toInt()//todo delta serverTick-clientTick
      val a = ClientPayload.ClientAction(
        tick = clientTick+w,//todo serverTick?
        action = action
      )
      synchronized(myLocal) {
        playerId?.let {myLocal.add(TickAction(clientTick+w, it, p = PlayerAction(it, a.action)))}
        myLocal.sortBy {it.tick}
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
      if(stable==null) return null
      synchronized(this) {
        result = StateWrapper(stable!!)
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
          Lib.Log.breakpoint("size > 0")
          if(filtered.any{it.p != null}) {
            Lib.Log.breakpoint("p != null")
          }
        }
        _state act filtered.iterator()
        _state.tick()
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
