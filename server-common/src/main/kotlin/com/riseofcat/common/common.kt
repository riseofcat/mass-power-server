package com.riseofcat.common

object ServerCommon {
  fun test():String {
    val k = "server"
    val m = "common"
    return "$k $m"
  }
}

expect class Common {
  companion object {
    fun <T> createConcurrentList():MutableList<T>
    fun <K,V> createConcurrentHashMap():MutableMap<K,V>
    fun toJson(obj:Any):String
    inline fun <reified T> fromJson(str:String):T
  }
}
