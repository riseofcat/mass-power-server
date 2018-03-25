package com.riseofcat.server

import com.riseofcat.lib.*
import com.riseofcat.lib_gwt.IConverter
import com.riseofcat.share.ping.*
import com.riseofcat.share.mass.ClientPayload
import com.riseofcat.share.mass.ServerPayload

import java.io.*

object ServerMain {
  @JvmStatic fun main(args:Array<String>) {
    val port = java.lang.System.getenv("PORT")
    spark.Spark.port(if(port!=null) Integer.parseInt(port) else 5000)
    if(false) {
      spark.Spark.threadPool(30,2,30000)
      spark.Spark.webSocketIdleTimeoutMillis(30000)
    }
    spark.Spark.staticFiles.location("/public")
    spark.Spark.staticFiles.expireTime(600)
    spark.Spark.webSocket("/socket",SparkWebSocket(
      UsageMonitorDecorator<Reader,String>(
        ConvertDecorator<ClientSay<ClientPayload>,ServerSay<ServerPayload>,Reader,String>(
          PingDecorator(
            RoomsDecorator<ClientPayload,ServerPayload>().apply {onRoomCreated.add({ room -> ServerModel(room) })},Duration(1000)),
          IConverter {obj ->
            Util.fromJsonClientSay(obj)
          },
          IConverter {ss->
            Util.toServerSayJson(ss)
          }
          ))))
    spark.Spark.get("/") {request,response-> LibJvm.info()}
    spark.Spark.init()//Spark.stop();
  }
}
