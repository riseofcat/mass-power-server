package com.riseofcat.client

import kotlinx.serialization.Serializable

@Serializable class Conf(
  var port:Int = 80,
  var host:String = "localhost")
