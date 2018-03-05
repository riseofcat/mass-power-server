package com.riseofcat.common

import kotlin.reflect.*

object ServerCommon {
  fun test():String {
    val k = "server"
    val m = "common"
    return "$k $m"
  }
}

expect fun Any.toJson():String
expect inline fun <reified T:Any>String.fromJson():T
//expect fun <T>String.fromJson():T
expect fun <T> createConcurrentList():MutableList<T>
expect fun <K,V> createConcurrentHashMap():MutableMap<K,V>

