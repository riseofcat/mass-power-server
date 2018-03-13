package com.riseofcat.server

import com.riseofcat.lib.*
import com.riseofcat.lib_gwt.IConverter
import com.riseofcat.share.base.*
import com.riseofcat.share.mass.ClientPayload
import com.riseofcat.share.mass.ServerPayload

import spark.Spark
import java.io.*

//import static spark.Spark.*;
//http://sparkjava.com/documentation

object ServerMain {
  @JvmStatic fun main(args:Array<String>) {
    val port = java.lang.System.getenv("PORT")
    Spark.port(if(port!=null) Integer.parseInt(port) else 5000)
    if(false) {
      Spark.threadPool(30,2,30000)
      Spark.webSocketIdleTimeoutMillis(30000)
    }
    Spark.staticFiles.location("/public")
    Spark.staticFiles.expireTime(600)
    Spark.webSocket("/socket",SparkWebSocket(
      UsageMonitorDecorator<Reader,String>(
        ConvertDecorator<ClientSay<ClientPayload>,ServerSay<ServerPayload>,Reader,String>(
          PingDecorator(
            RoomsDecorator<ClientPayload,ServerPayload>(object: Signal.Listener<RoomsDecorator<ClientPayload, ServerPayload>.Room> {
              override fun onSignal(room:RoomsDecorator<ClientPayload,ServerPayload>.Room) {
                TickGame(room)
              }
            }),1000),
          IConverter {obj ->
            Util.fromJsonClientSay(obj)
          },
          IConverter {ss->
            Util.toServerSayJson(ss)
          }
          ))))
    Spark.get("/") {request,response-> LibJava.info()}
    Spark.init()//Spark.stop();
  }
}
