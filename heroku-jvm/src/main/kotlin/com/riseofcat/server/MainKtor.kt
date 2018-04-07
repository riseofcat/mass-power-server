package com.riseofcat.server

//import io.ktor.host.*
//import io.ktor.jetty.*
import com.riseofcat.lib.*
import com.riseofcat.lib_gwt.*
import com.riseofcat.share.mass.*
import com.riseofcat.share.ping.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import java.time.Duration

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

var mal:MutableMap<Session,Ses<String>> = mutableMapOf()
var lastId = 0

fun Application.main() {
  install(DefaultHeaders)
  install(CallLogging)
  install(WebSockets) {
    pingPeriod = Duration.ofSeconds(60) // Disabled (null) by default
    timeout = Duration.ofSeconds(15)
    maxFrameSize = Long.MAX_VALUE // Disabled (max value). The connection will be closed if surpassed this length.
    masking = false
  }
  install(Routing) {
    get("/") {call.respondText("ktor: " + LibJvm.info().toString())}
    webSocket("/socket") {
      val s = object:Ses<String>() {
        override val typeMap:TypeMap = TypeMap()
        override val id = ++lastId
        override fun stop() {
          async {close(CloseReason(CloseReason.Codes.NORMAL,"Good bye"))}
        }
        override fun send(message:String) {
          async {outgoing.send(Frame.Text(message))}
        }
      }
      serverModel.start(s)

      //todo Frame.Binary
      incoming.mapNotNull { it as? Frame.Text }.consumeEach { frame ->
        serverModel.message(s, frame.readText())
      }

    }
  }
}

class Session(val chanel:SendChannel<Frame>) {

}

fun oldSparkMain(args:Array<String>) {
  System.getenv("PORT")?.let {
    spark.Spark.port(it.toInt())
  }
  spark.Spark.get("/") {req,res-> "Hello from Kotlin"}
}