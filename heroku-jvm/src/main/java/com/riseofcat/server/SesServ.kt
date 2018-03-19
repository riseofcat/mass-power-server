package com.riseofcat.server

import com.riseofcat.lib.TypeMap

abstract class SesServ<C,S> {
  abstract fun start(session:Ses<S>)
  abstract fun close(session:Ses<S>)
  abstract fun message(ses:Ses<S>,code:C)
}

abstract class Ses<S> {
  private var typeMapCache:TypeMap? = null

  abstract val id:Int
  abstract val typeMap:TypeMap
  abstract fun stop()
  infix abstract fun send(message:S)

  fun <T:TypeMap.Marker> put(value:T) {
    if(typeMapCache==null) {
      typeMapCache = typeMap
    }
    typeMapCache!!.put(value)
  }

  operator fun <T:TypeMap.Marker> get(type:Class<T>):T? {
    return typeMap.get(type)
  }
}

