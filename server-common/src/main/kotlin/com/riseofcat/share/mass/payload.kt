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

@Serializable class AllCommand(
  val tick:Tick,
  val pid:PlayerId,
  @Optional var newCarCmd:NewCarCommand? = null,//todo val
  @Optional var moveCmd:MoveCommand? = null//todo val
):ICommand {
  override fun act(state:State) {
    newCarCmd?.act(state)
    moveCmd?.act(state)
  }
}

@Serializable class ServerPayload(
  val stableTick:Tick,
  @Optional val welcome:Welcome? = null,
  @Optional val stable:State? = null,
  @Optional var actions:List<AllCommand> = mutableListOf(),
  @Optional val error:ServerError? = null,
  @Optional val recommendedLatency:Duration? = null
)
@Serializable class Welcome(
  val id:PlayerId,
  val roomCreate:TimeStamp
)
@Serializable class ClientPayload(val actions:MutableList<ClientAction>) {
  @Serializable class ClientAction(
    val tick:Tick,
    @Optional var moveDirection:Angle? = null,//todo val
    @Optional var newCar:Boolean = false//todo val
  )
}

@Serializable class ServerError(val code:Int = 0,@Optional val message:String? = null)