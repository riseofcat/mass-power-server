package com.riseofcat.share.mass

import com.riseofcat.lib.*
import kotlinx.serialization.*

@Serializable data class Tick(val tick:Int):Comparable<Tick> {
  constructor(longTick:Long):this(longTick.toInt())
  override fun compareTo(other:Tick) = tick.compareTo(other.tick)
}

operator fun Tick.times(multiply:Int) = Tick(this.tick * multiply)
operator fun Tick.plus(i:Int) = Tick(tick+i)
operator fun Tick.plus(l:Long) = Tick(tick+l)
operator fun Tick.plus(other:Tick) = Tick(tick + other.tick)
operator fun Tick.minus(other:Tick) = Tick(tick - other.tick)
operator fun Tick.times(scl:Double) = Tick((tick * scl).toInt())

@Serializable class TickAction(
  val tick:Tick,
  val pid:PlayerId,
  @Optional val n:NewCarAction? = null,
  @Optional val p:PlayerAction? = null
):InStateAction {
  override fun act(state:State) {
    n?.act(state)
    p?.act(state)
  }
}

@Serializable class ServerPayload(
  @Optional val welcome:Welcome? = null,
  @Optional val stable:Stable? = null,
  @Optional var actions:List<TickAction> = mutableListOf(),
  @Optional val error:ServerError? = null
)
@Serializable class Welcome(
  val id:PlayerId,
  val roomCreate:TimeStamp
)
@Serializable data class Stable(
  @Deprecated("tick есть в state.tick") val tick:Tick,//все actions уже пришли и новых больше не будет. Если кто-то кого-то убил, то в этом кадре засчитывается фраг. Но само убийство и набор очков могло произойти в прошлом
  val state:State//? = null todo nullable  and @Optional
)

@Serializable class ClientPayload(val actions:MutableList<ClientAction>) {
  @Serializable class ClientAction(
    val tick:Tick,
    val action:Action
  )
}

@Serializable class ServerError(val code:Int = 0,@Optional val message:String? = null)