package com.riseofcat.common

import com.beust.klaxon.*
import com.google.gson.*
import java.util.concurrent.*
import kotlin.reflect.*

val gson = Gson()
val klaxon = Klaxon()

actual fun Any.toJson():String {
//  val toJsonString = klaxon.toJsonString(this)
//  return toJsonString
  return gson.toJson(this)
}
actual inline fun <reified T:Any>String.fromJson():T {
//  return klaxon.parse(this)!!
  return gson.fromJson(this, T::class.java)
}
actual fun <T> createConcurrentList():MutableList<T> {
  return CopyOnWriteArrayList()
}
actual fun <K,V> createConcurrentHashMap():MutableMap<K,V> {
  return ConcurrentHashMap()
}

