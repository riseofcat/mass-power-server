package com.riseofcat.client

import com.riseofcat.common.*
import com.riseofcat.lib.*
import com.riseofcat.share.mass.*

class ClientModel(conf:Conf) {
  val CACHE = true
  val client:PingClient<ServerPayload,ClientPayload> = PingClient(conf.host,conf.port,"socket",SerializeHelp.serverSayServerPayloadSerializer,SerializeHelp.clientSayClientPayloadSerializer)
  private val actions:MutableList<AllCommand> = Common.createConcurrentList()
  private val myLocal:MutableList<AllCommand> = mutableListOf()//todo избавиться от myLocal
  private var stable:StateWrapper = StateWrapper(State())
  val playerName get() = welcome?.id?.let {"Player $it"} ?: "Wait connection..."
  var welcome:Welcome?=null//todo lateinit? //todo Может сделать что если приходит новый Welcome, то игрока перевели в другую комнату
  var recommendendLatency:Duration?=null

  init {
    client.connect {s:ServerPayload->
      synchronized(this) {
        if(s.welcome!=null) welcome = s.welcome
        if(s.recommendedLatency != null) recommendendLatency = s.recommendedLatency
        if(s.stable!=null) {
          stable = StateWrapper(s.stable)
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

  val latency:Duration get() = recommendendLatency?: Duration(150)
  val realtimeTick get():Tick = welcome?.run{Tick((client.serverTime-roomCreate)/GameConst.UPDATE)}?:Tick(0)

  val start = lib.time
  var moves:Int = 0
  fun calcDisplayState():State? {
    if(false) if(Duration(300)*moves < lib.time - start) {//Временный код для тестирования производительности
      move(degreesAngle(45))
      moves++
    }
    return getState(realtimeTick)
  }
  fun ready() = welcome!=null
  val meAlive get() = lib.measure("meAlive"){getState(realtimeTick)?.cars?.any {it.owner == welcome?.id}?:false}
  fun move(direction:Angle) = synchronized(this) {
    if(!ready()) return
    val t = realtimeTick + Tick(latency/GameConst.UPDATE+1)
    val a = ClientPayload.ClientAction(tick = t)
    a.moveDirection = direction
    synchronized(myLocal) {//todo может synchronized не надо...
      welcome?.run {myLocal.add(AllCommand(t, id, moveCmd = MoveCommand(id, direction)))}
      //actions.sortBy{it.tick}
    }
    client.say(ClientPayload(mutableListOf(a))) //todo если предудыщее отправление было в этом же тике, то задержать текущий набор действий на следующий tick
  }
  fun newCar() = synchronized(this) {//todo дублирование кода
    if(!ready()) return
    val t = realtimeTick + Tick(latency/GameConst.UPDATE+1)
    val a = ClientPayload.ClientAction(tick = t)
    a.newCar = true
    synchronized(myLocal) {//todo может synchronized не надо...
      welcome?.run {myLocal.add(AllCommand(t, id, newCarCmd = NewCarCommand(id)))}
      //actions.sortBy{it.tick}
    }
    client.say(ClientPayload(mutableListOf(a))) //todo если предудыщее отправление было в этом же тике, то задержать текущий набор действий на следующий tick
  }
  fun dispose() { client.close() }

  private var cache:StateWrapper? = null
  private fun clearCache(tick:Tick = Tick(0)) = cache?.let {if(tick<=it._state.tick) cache = null}
  private fun saveCache(value:StateWrapper) { cache = value }
  private fun getNearestCache(tick:Tick):StateWrapper? = if(CACHE) _getNearestCache(tick) else null
  private fun _getNearestCache(tick:Tick) = cache?.let {if(tick>=it._state.tick) it else null}
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
      lib.measure("StateWrapper.tick") {
        while(_state.tick<targetTick) {
          val filtered = (actions+myLocal).filter {it.tick==_state.tick}
          _state act filtered.iterator()
          _state.tick()
        }
      }
    }
  }
}

fun ClientModel.touch(pos:XY) {
  val displayState = calcDisplayState()
  if(displayState==null||welcome==null) return
  if(meAlive) {
    for((owner,_,_,pos1) in displayState.cars) {
      if(welcome?.id==owner) {
        val direction = (pos - pos1).calcAngle() + degreesAngle(0*180)
        move(direction)
        break
      }
    }
  } else {
    newCar()
  }
}
