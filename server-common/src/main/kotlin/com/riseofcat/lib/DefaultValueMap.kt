package com.riseofcat.lib

class DefaultValueMap<K,V>(val map:MutableMap<K,V>,private val createNew:()->V) {
  fun getExistsOrPutDefault(key:K):V {
    if(map.containsKey(key)) {
      return map[key]!!
    } else {
      val v = createNew()
      map[key] = v
      return v
    }
  }

}

