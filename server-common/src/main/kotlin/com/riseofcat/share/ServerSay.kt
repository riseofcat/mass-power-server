package com.riseofcat.share

open class ServerSay<T>(
  var ping:Boolean = false,
  var latency:Int? = null,
  var payload:T? = null) {

}
