package com.riseofcat.server

import com.google.gson.*
import com.riseofcat.client.*
import com.riseofcat.lib.*
import com.riseofcat.lib_gwt.*
import com.riseofcat.server.telegram.*
import com.riseofcat.share.mass.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.netty.*
import io.ktor.server.engine.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import kotlinx.serialization.json.*
import java.time.Duration
import java.util.concurrent.atomic.*

val rooms:MutableList<ServerModel> = mutableListOf()
val incomeMessages = AtomicInteger()
var lastId = AtomicInteger()
//val server = UsageMonitorDecorator<ByteArray,ByteArray>(
val server = UsageMonitorDecorator<String,String>(
ConvertDecorator(
PingDecorator(
  RoomsDecorator<ClientPayload,ServerPayload>().apply {
    onRoomCreated.add{rooms.add(ServerModel(it))}
  },
  pingInterval = Duration(1000)
),
IConverter {obj ->
  Util.fromJsonClientSay(obj)
},
IConverter {ss->
  Util.toServerSayJson(ss)
}
))

fun main(args:Array<String>) {
  val port:Int = try {
    Integer.valueOf(System.getenv("PORT"))
  } catch(e:Exception) {
    5000
  }
  startBots()
  embeddedServer(if(true) CIO else Netty, port, module = Application::main).start(wait = true)
  //После этого код не выполняется, такак как embeddedServer блокирует этот поток
}

fun startBots() {
  launch {
    while(true) {
      for(room in rooms) {
        if(room.bots.size<30) {
          room.bots.add(Bot(lastId.incrementAndGet()))
        }
        room.updateBots()
        yield()
      }
      delay(650)
    }
  }
}

fun Application.main() {
  install(DefaultHeaders)
  lib.debug {
    install(CallLogging)
  }
  install(WebSockets) {
    pingPeriod = Duration.ofSeconds(60) // Disabled (null) by default
    timeout = Duration.ofSeconds(15)
    maxFrameSize = Long.MAX_VALUE // Disabled (max value). The connection will be closed if surpassed this length.
    masking = false
  }

  val bodyKey = AttributeKey<String>("body")
  intercept(ApplicationCallPipeline.Infrastructure) {
    call.attributes.put(bodyKey, call.receiveText())//todo второй раз вызвать receiveText - приходит пустая строка
    call.request.document()
    lib.log.info("""
      ${call.request.origin.uri}
      headers: ${call.request.headers.toMap().map {"${it.key}: ${it.value}"}}
      recieve: ${call.receiveText()}
    """.trimIndent())
    call.request.queryParameters
  }

  routing {
    get("/") {
      call.respondText(
        """
        ktor: ${LibJvm.info()}
        incomeMessages: ${incomeMessages}
        """.trimIndent()
      )
    }
    post("/telegram") {
      val bodyStr = call.attributes.get(bodyKey)
      val fromJson = if(true) {
        Gson().fromJson(bodyStr, TgMessage::class.java)
      } else {
        JSON(unquoted = false, nonstrict = true).parse<TgMessage>(bodyStr)//todo вываливается ошибка
      }
      lib.log.info("message_id: ${fromJson.message?.message_id}")

      call.respondText("True")
    }
    webSocket("/socket") {
      val ktorSes:DefaultWebSocketSession = this
//      val s = object:Ses<ByteArray>() {
      val s = object:Ses<String>() {
        override val typeMap:TypeMap = TypeMap()
        override val id = lastId.incrementAndGet()
        override fun stop() {
          async {close(CloseReason(CloseReason.Codes.NORMAL,"Good bye"))}
        }
//        override fun send(message:ByteArray) {
        override fun send(message:String) {
          if(confs.serverSayBinary) {
            async {outgoing.send(Frame.Binary(true, java.nio.ByteBuffer.wrap(message as ByteArray)))}
          } else {
            async {outgoing.send(Frame.Text(message as String))}
          }
        }
      }
      server.start(s)

      incoming.mapNotNull { it as? Frame.Text }.consumeEach { frame ->
        incomeMessages.incrementAndGet()
        if(false) {
          lib.log.info("thread: " + Thread.currentThread().name)
          lib.log.info("incomeMessages: "+ incomeMessages)
        }
        server.message(s, frame.readText() as String)
      }

      incoming.mapNotNull { it as? Frame.Binary }.consumeEach { frame ->
        incomeMessages.incrementAndGet()
        if(false) {
          lib.log.info("thread: " + Thread.currentThread().name)
          lib.log.info("incomeMessages: "+ incomeMessages)
        }
        server.message(s, frame.buffer.moveToByteArray() as String)
      }

    }
  }
}
