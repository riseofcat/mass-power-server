package com.riseofcat.share.mass

import com.riseofcat.lib.*
import kotlinx.serialization.*

@Serializable data class Tick(val tick:Int):Comparable<Tick> {
  constructor(longTick:Long):this(longTick.toInt())
  override fun compareTo(other:Tick) = this.tick.compareTo(other.tick)
}

operator fun Tick.times(multiply:Int) = Tick(this.tick * multiply)
fun Tick.toDbl() = TickDbl(tick.toDouble())

@Deprecated("use serverTime")
@Serializable data class TickDbl(val double:Double)

fun TickDbl.intTick() = Tick(double.toInt())
operator fun TickDbl.plus(l:Long) = TickDbl(double + l)
operator fun TickDbl.plus(d:Double) = TickDbl(double + d)
operator fun TickDbl.plus(other:TickDbl) = TickDbl(double + other.double)
operator fun TickDbl.minus(other:TickDbl) = TickDbl(double - other.double)
operator fun TickDbl.times(scl:Double) = TickDbl(double * scl)

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

@Serializable class ClientPayload(
  val tick:TickDbl,
  val actions:MutableList<ClientAction>
) {
  @Serializable class ClientAction(
    val tick:TickDbl,
    val action:Action
  )
}

@Serializable class ServerError(val code:Int = 0,@Optional val message:String? = null)

operator fun Tick.plus(i:Int) = Tick(tick+i)
operator fun Tick.plus(t:Tick) = Tick(tick+t.tick)
operator fun Tick.minus(t:Tick) = Tick(tick - t.tick)
operator fun Tick.compareTo(other:Tick) = tick - other.tick