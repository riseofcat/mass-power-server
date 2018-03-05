package com.riseofcat.lib_gwt

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

  fun getOrNew(key:K,or:()->V):V {
    return if(map.containsKey(key)) {
      map[key]!!
    } else {
      or()
    }
  }

}

