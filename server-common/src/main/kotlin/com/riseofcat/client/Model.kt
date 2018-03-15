package com.riseofcat.client

import com.riseofcat.common.*
import com.riseofcat.lib.*
import com.riseofcat.share.*
import com.riseofcat.share.base.*
import com.riseofcat.share.mass.*
import kotlin.system.*

class Model(conf:Conf) {
  val client:PingClient<ServerPayload,ClientPayload>
  var playerId:PlayerId? = null
  private val actions = DefaultValueMap<Tick,MutableList<BigAction>>(mutableMapOf(),{Common.createConcurrentList()})
  private val myActions = DefaultValueMap<Tick,MutableList<Action>>(mutableMapOf(),{mutableListOf()})
  private var stable:StateWrapper? = null
  private var sync:Sync? = null
  val playerName:String get() = playerId?.let {"Player $it"} ?: "Wait connection..."
  private var previousActionId = 0
  fun calcDisplayState():State? = sync?.let {getState(it.calcClientTck().toInt())}
  private var cache:StateWrapper? = null

  class Sync(internal val serverTick:Float,oldSync:Sync?) {
    internal val clientTick:Float
    internal val time:Long

    init {
      time = Common.timeMs
      if(oldSync==null)
        this.clientTick = serverTick
      else
        this.clientTick = oldSync.calcClientTck()
    }

    private fun calcSrvTck(t:Long):Float {
      return serverTick+(t-time)/GameConst.UPDATE_MS.toFloat()
    }

    fun calcSrvTck():Float {
      return calcSrvTck(Common.timeMs)
    }

    fun calcClientTck():Float {
      val t = Common.timeMs
      return calcSrvTck(t)+(clientTick-serverTick)*(1f-Lib.Fun.arg0toInf((t-time).toDouble(),600f))
    }
  }

  init {
    client = PingClient(conf.host,conf.port,"socket",SerializeHelp.serverSayServerPayloadSerializer,SerializeHelp.clientSayClientPayloadSerializer)
    client.connect {s:ServerPayload->
      synchronized(this) {
        sync = Sync(s.tick+client.smartLatencyS/GameConst.UPDATE_S,sync)
        if(s.welcome!=null) {
          playerId = s.welcome!!.id
        }
        if(s.stable!=null) {
          stable = StateWrapper(s.stable!!.state,s.stable!!.tick)
          clearCache(s.stable!!.tick)
        }
        if(s.actions!=null&&s.actions!!.size>0) {
          for(t:TickActions in s.actions!!) {
            actions.getExistsOrPutDefault(Tick(t.tick)).addAll(t.list)
            clearCache(t.tick+1)
          }
        }
        for(t in myActions.map.keys) {
          val iterator = myActions.map[t]!!.iterator()
          whl@ while(iterator.hasNext()) {
            val next = iterator.next()
            if(s.canceled!=null) {
              if(s.canceled!!.contains(next.aid)) {
                iterator.remove()
                clearCache(t.tick+1)
                continue
              }
            }
            if(s.apply!=null) {
              for(apply in s.apply!!) {
                if(apply.aid==next.aid) {
                  if(!ShareTodo.SIMPLIFY) actions.getExistsOrPutDefault(t+apply.delay).add(PlayerAction(playerId!!,next.pa.action).toBig())
                  iterator.remove()
                  clearCache(t.tick+1)
                  continue@whl
                }
              }
            }
          }
        }
      }
    }
  }

  fun ready():Boolean {
    return playerId!=null
  }

  fun action(action:com.riseofcat.share.mass.Action) {
    synchronized(this) {
      val clientTick:Int = sync!!.calcClientTck().toInt()//todo +0.5f?
      if(!ready()) return
      if(false) if(sync!!.calcSrvTck()-sync!!.calcClientTck()>GameConst.DELAY_TICKS*1.5||sync!!.calcClientTck()-sync!!.calcSrvTck()>GameConst.FUTURE_TICKS*1.5) return
      val w = (client.smartLatencyS/GameConst.UPDATE_S+1).toInt()//todo delta serverTick-clientTick
      val a = ClientPayload.ClientAction(
        aid = ++previousActionId,
        wait = w,
        tick = clientTick+w,//todo serverTick?
        action = action
      )
      synchronized(myActions) {
        myActions.getExistsOrPutDefault(Tick(clientTick+w)).add(Action(a.aid,a.action))
      }
      val payload = ClientPayload(
        tick = clientTick,
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

  private fun clearCache(tick:Int) {
    if(cache!=null&&tick<cache!!.tick) cache = null
  }

  private fun getNearestCache(tick:Int):StateWrapper? {
    return if(cache!=null&&cache!!.tick<=tick) cache else null
  }

  private fun saveCache(value:StateWrapper) {
    cache = value
  }

  private fun getState(tick:Int):State? {
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

  private inner class Action(
    val aid:Int,
    action:com.riseofcat.share.mass.Action,
    val pa:PlayerAction = PlayerAction(playerId!!,action)):InStateAction by pa

  private inner class StateWrapper(
    var state:State,
    var tick:Int) {
    constructor(obj:StateWrapper):this(obj.state.copy(),obj.tick)

    fun tick(targetTick:Int) {
      while(tick<targetTick) {
        val other = actions.map[Tick(tick)]
        if(other!=null) state.act(other.iterator())
        val my = myActions.map[Tick(tick)]
        if(my!=null) {
          synchronized(myActions) {
            state.act(my.iterator())
          }
        }
        measureNanoTime{
          state.tick()
        }.let{averageTickNanos = (averageTickNanos*frames + it) / (frames+1)}
        tick++
      }
    }
  }
}

val frames = 20
var averageTickNanos = 0f
