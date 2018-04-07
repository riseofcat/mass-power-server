package com.riseofcat.server

import com.riseofcat.lib_gwt.IConverter
import com.riseofcat.share.mass.*
import com.riseofcat.share.ping.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
//import io.ktor.host.*
//import io.ktor.jetty.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.experimental.channels.*
import java.time.*

//import io.ktor.websocket.*

val serverModel = UsageMonitorDecorator<String,String>(
ConvertDecorator<ClientSay<ClientPayload>,ServerSay<ServerPayload>,String,String>(
PingDecorator(
RoomsDecorator<ClientPayload,ServerPayload>().apply {onRoomCreated.add({ room -> ServerModel(room) })},com.riseofcat.lib.Duration(1000)),
IConverter {obj ->
  Util.fromJsonClientSay(obj)
},
IConverter {ss->
  Util.toServerSayJson(ss)
}
))

fun main(args:Array<String>) {
  var port = 5000
  try {
    port = Integer.valueOf(System.getenv("PORT"))
  } catch(e:Exception) {

  }

  val server = embeddedServer(Netty, port, module = Application::main).start(wait = true)
//  {
//    routing {
//      get("/") {
//        call.respondText("hi, ktor!", ContentType.Text.Html)
//      }
//    }
//  }.start(wait = true)

}

fun Application.main() {
  install(DefaultHeaders)
  install(CallLogging)
//  install(ConditionalHeaders)
//  install(PartialContentSupport)

//  install(WebSocket)
  install(WebSockets) {
    pingPeriod = Duration.ofSeconds(60) // Disabled (null) by default
    timeout = Duration.ofSeconds(15)
    maxFrameSize = Long.MAX_VALUE // Disabled (max value). The connection will be closed if surpassed this length.
    masking = false
  }
  install(Routing) {
    get("/") {
      call.respondText("Hello, ktor!")
    }
    webSocket("/socket") {
      incoming.mapNotNull { it as? Frame.Text }.consumeEach { frame ->
        val text = frame.readText()
        serverModel.message()
        outgoing.send(Frame.Text("YOU SAID $text"))
        if (text.equals("bye", ignoreCase = true)) {
          close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
        }
      }

//      //https://github.com/Kotlin/ktor/blob/master/ktor-samples/ktor-samples-websocket/src/org/jetbrains/ktor/samples/chat/ChatApplication.kt
//      this.send(Frame.Text("hello from ktor websocket"))
//      this.handle {
//        if(it is Frame.Text) {
//          val readText = it.readText()
//          System.out.println(readText)
//        }
//        if(it is Frame.Binary) {
//
//        }
//      }
//
    }
  }
//  install(WebSockets)
}

class Session(val chanel:SendChannel<Frame>) {

}

fun oldSparkMain(args:Array<String>) {
  System.getenv("PORT")?.let {
    spark.Spark.port(it.toInt())
  }
  spark.Spark.get("/") {req,res-> "Hello from Kotlin"}
}