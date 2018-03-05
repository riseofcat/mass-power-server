package com.riseofcat.common

import kotlin.reflect.*

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
    fun <T:Any> fromJson(str:String,clazz:KClass<T>):T
  }
}
