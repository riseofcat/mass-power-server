package com.riseofcat.share.mass

import kotlinx.serialization.*

@Serializable data class Tick(val tick:Int):Comparable<Tick> {
  override fun compareTo(other:Tick) = this.tick.compareTo(other.tick)
}

operator fun Tick.times(multiply:Int) = Tick(this.tick * multiply)
fun Tick.toDbl() = TickDbl(tick.toDouble())

@Serializable data class TickDbl(val double:Double)
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
  var tick:TickDbl,
  @Optional val welcome:Welcome? = null,
  @Optional val stable:Stable? = null,
  @Optional var actions:MutableList<TickAction> = mutableListOf(),
  @Optional val error:ServerError? = null
)
@Serializable class Welcome(val id:PlayerId)
@Serializable data class Stable(
  val tick:Tick,//все actions уже пришли и новых больше не будет. Если кто-то кого-то убил, то в этом кадре засчитывается фраг. Но само убийство и набор очков могло произойти в прошлом
  val state:State//? = null todo nullable  and @Optional
)

@Serializable class ClientPayload(
  val tick:TickDbl,
  val actions:MutableList<ClientAction>
) {
  @Serializable class ClientAction(
    val tick:Tick,
    val action:Action
  )
}

@Serializable class ServerError(val code:Int = 0,@Optional val message:String? = null)

operator fun Tick.plus(i:Int) = Tick(tick+i)
operator fun Tick.plus(t:Tick) = Tick(tick+t.tick)
operator fun Tick.minus(t:Tick) = Tick(tick - t.tick)
operator fun Tick.compareTo(other:Tick) = tick - other.tick