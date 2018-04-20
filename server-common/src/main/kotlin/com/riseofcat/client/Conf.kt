package com.riseofcat.client

import com.riseofcat.share.mass.*
import kotlinx.serialization.Serializable

@Serializable class Conf(
  var port:Int,
  var host:String) {
  val path = "socket"
  fun pingClient() = PingClient(host,port,path,SerializeHelp.serverSayServerPayloadSerializer,SerializeHelp.clientSayClientPayloadSerializer)
}

object confs {
  val local = Conf(5000, "localhost")
  val heroku = Conf(80, "mass-power.herokuapp.com")
  val ramenki = Conf(5000, "192.168.100.5")
  val tutu = Conf(5000,"192.168.43.176")
  val current = heroku
}