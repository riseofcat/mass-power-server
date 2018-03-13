package com.riseofcat.lib

class Signal<T> {
  //todo weak reference
  private val callbacks:MutableList<Callback> = mutableListOf()

  fun dispatch(value:T) {
    val currentCallbacks = callbacks.copy()
    var iterator:MutableIterator<Callback> = currentCallbacks.iterator()
    while(iterator.hasNext()) {
      val next = iterator.next()
      next.listener!!(value)
      if(next.once) {
        next.removed = true
      }
    }
    iterator = callbacks.iterator()
    while(iterator.hasNext()) {
      val next = iterator.next()
      if(next.removed) {
        iterator.remove()
      }
    }
  }

  fun add(listener:SignalListener<T>) {
    val c = Callback()
    c.listener = listener
    callbacks.add(c)
  }

  fun addOnce(listener:SignalListener<T>) {
    val c = Callback()
    c.listener = listener
    c.once = true
    callbacks.add(c)
  }

  fun remove(signalListener:SignalListener<T>) {
    val iterator = callbacks.iterator()
    while(iterator.hasNext()) {
      val next = iterator.next()
      if(next.listener===signalListener) {
        next.removed = true
        iterator.remove()
      }
    }
  }

  fun destroy() {
    callbacks.clear()
  }

  private inner class Callback {
    var listener:SignalListener<T>? = null
    var removed = false
    var once = false
  }
}

typealias SignalListener<T> = (arg:T)->Unit