package com.n8cats.share.data

class PlayerId {
  var id:Int = 0

  constructor() {}
  constructor(id:Int) {
    this.id = id
  }

  override fun hashCode():Int {
    return id
  }

  override fun equals(o:Any?):Boolean {
    return o!=null&&(o===this||o.javaClass==PlayerId::class.java&&(o as PlayerId).id==id)
  }

  override fun toString():String {
    return id.toString()
  }
}
