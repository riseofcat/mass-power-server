package com.riseofcat.share.data

class CompositeIterator<T>(vararg lists:MutableList<out T>):MutableIterator<T> {
  private val lists:Array<out MutableList<out T>>
  private var index = 0
  private var iterator:MutableIterator<T>? = null

  init {
    this.lists = lists
    iterator = lists[0].iterator()
  }

  override fun hasNext():Boolean {
    if(iterator!!.hasNext()) {
      return true
    }
    var add = 1
    while(index+add<lists.size) {
      if(lists[index+add].size>0) {
        return true
      }
      add++
    }
    return false
  }

  override fun next():T {
    while(index<lists.size) {
      if(iterator!!.hasNext()) {
        return iterator!!.next()
      }
      index++
      if(index<lists.size) {
        iterator = lists[index].iterator()
      }
    }
    error("next null")
  }

  override fun remove() {
    iterator!!.remove()//todo test
  }
}
