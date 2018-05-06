package com.riseofcat.client

import com.riseofcat.common.*
import com.riseofcat.lib.*
import com.riseofcat.share.mass.*

class ClientModel(val ping:IPingClient<ServerPayload, ClientPayload>, val slowpoke:Boolean=false):IClientModel {
  val FREEZE_TICKS = Tick(Duration(1000)/GameConst.UPDATE+1)//todo сделать плавное ускорение времени после фриза?
  val CACHE = true
  private val actions:MutableList<AllCommand> = Common.createConcurrentList()
  private val myLocal:MutableList<AllCommand> = mutableListOf()//todo избавиться от myLocal
  private var stable:StateWrapper = StateWrapper(State())
  override val playerName get() = welcome?.id?.let {"Player $it"} ?: "Wait connection..."
  var welcome:Welcome?=null//todo lateinit?
  var recommendendLatency:Duration?=null

  init {
    ping.connect {s:ServerPayload->
      synchronized(this) {
        if(s.welcome!=null) welcome = s.welcome
        if(s.recommendedLatency != null) recommendendLatency = s.recommendedLatency
        if(s.stable!=null) {
          stable = StateWrapper(s.stable)
          clearCache()
        }
        actions.removeAll {it.tick<stable._state.tick}//todo оптимизировать
        actions.addAll(s.actions)
        actions.sortBy {it.tick}//todo оптимизировать

        val myMaxApplyTick:Tick = s.actions.filter {it.pid==welcome?.id}.map {it.tick}.max()?:Tick(0)//Последний tick который принял сервер от этого игрока
        myLocal.removeAll {it.tick <= myMaxApplyTick}
        val serverMinApplyTick:Tick? = s.actions.map{it.tick}.min()
        stable.tick(s.stableTick)
        if(serverMinApplyTick != null) {
          clearCache(serverMinApplyTick)
        }
        actions.removeAll {it.tick<stable._state.tick}//todo оптимизировать
      }
    }
  }

  val latency:Duration get() = recommendendLatency?: Duration(150)
  val realtimeTick get():Tick = welcome?.run{Tick((ping.serverTime-roomCreate)/GameConst.UPDATE)}?:Tick(0)

  val start = lib.time
  var moves:Int = 0
  fun calcDisplayState():State? {
    if(slowpoke) return stable._state
    return getState(realtimeTick)//todo можно рендерить с задержкой для слабых клиентов, чтобы кэш дольше жил
  }
  override fun ready() = welcome!=null
  val myCar:Car? get() = lib.measure("myCar"){calcDisplayState()?.cars?.firstOrNull {it.owner == welcome?.id}}
  override fun move(direction:Angle) = synchronized(this) {
    if(!ready()) return
    val t = realtimeTick + Tick(latency/GameConst.UPDATE+1)
    val a = ClientPayload.ClientAction(tick = t)
    a.moveDirection = direction
    synchronized(myLocal) {//todo может synchronized не надо...
      welcome?.run {myLocal.add(AllCommand(t, id, moveCmd = MoveCommand(id, direction)))}
      //actions.sortBy{it.tick}
    }
    ping.say(ClientPayload(mutableListOf(a))) //todo если предудыщее отправление было в этом же тике, то задержать текущий набор действий на следующий tick
  }
  override fun newCar() = synchronized(this) {//todo дублирование кода
    if(!ready()) return
    val t = realtimeTick + Tick(latency/GameConst.UPDATE+1)
    val a = ClientPayload.ClientAction(tick = t)
    a.newCar = true
    synchronized(myLocal) {//todo может synchronized не надо...
      welcome?.run {myLocal.add(AllCommand(t, id, newCarCmd = NewCarCommand(id)))}
      //actions.sortBy{it.tick}
    }
    ping.say(ClientPayload(mutableListOf(a))) //todo если предудыщее отправление было в этом же тике, то задержать текущий набор действий на следующий tick
  }
  override fun dispose() { ping.close() }
  private var cache:StateWrapper? = null//todo рендерить немного прошлое для лагающих клиентов тогда кэш реже надо будет сбрасывать
  private fun clearCache(tick:Tick = Tick(0)) {
    cache?.let {if(tick<=it._state.tick) cache = null}
  }
  private fun saveCache(value:StateWrapper) { cache = value }
  private fun getNearestCache(tick:Tick):StateWrapper? = if(CACHE) _getNearestCache(tick) else null
  private fun _getNearestCache(tick:Tick) = cache?.let {if(tick+FREEZE_TICKS>=it._state.tick) it else null}
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
        _state act filtered.iterator()
        _state.tick()
      }
    }
  }
}

fun ClientModel.touch(pos:XY) {
  val car = myCar
  _touch(car?.pos, pos)
}

fun ClientModel._touch(myCarPos:XY?, pos:XY) {
  if(myCarPos != null) {
    val direction = (pos - myCarPos).calcAngle() + degreesAngle(0*180)
    move(direction)
  } else {
    newCar()
  }
}

fun calcRenderXY(state:State,pos:XY, center:XY):XY {
  var x = pos.x
  val dx = center.x-x
  if(dx>state.width/2)
    x += state.width
  else if(dx<-state.width/2) x -= state.width
  var y = pos.y
  val dy = center.y-y
  if(dy>state.height/2)
    y += state.height
  else if(dy<-state.height/2) y -= state.height
  return XY(x,y)
}