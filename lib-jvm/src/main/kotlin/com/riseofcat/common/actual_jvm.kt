package com.riseofcat.common

import com.google.gson.*
import java.util.concurrent.*
import kotlin.reflect.*

val gson = Gson()

actual class Common {
  actual companion object {
    actual fun <T> createConcurrentList():MutableList<T> {
      return CopyOnWriteArrayList()
    }

    actual fun <K,V> createConcurrentHashMap():MutableMap<K,V> {
      return ConcurrentHashMap()
    }

    actual fun toJson(obj:Any):String {
      return gson.toJson(obj)
    }

    actual fun <T:Any> fromJson(str:String,clazz:KClass<T>):T {
      return gson.fromJson(str,clazz.java)
    }
  }
}
