package com.riseofcat.lib

class LibJava {
  companion object {
    @JvmStatic
    val log = Lib.Log

    @JvmStatic
    fun test() = "jvm-lib ${System.getProperties()["java.runtime.version"]}"
  }
}