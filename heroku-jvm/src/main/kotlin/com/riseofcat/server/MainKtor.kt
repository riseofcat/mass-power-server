package com.riseofcat.server

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.features.*
import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.jetty.*
import org.jetbrains.ktor.routing.*
import org.jetbrains.ktor.websocket.*

fun main(args:Array<String>) {
  var port = 5000
  try {
    port = Integer.valueOf(System.getenv("PORT"))
  } catch(e:Exception) {

  }
  embeddedServer(Jetty,port,reloadPackages = listOf("heroku"),module = Application::module/*, host = "localhost"*/).start()
}

fun Application.module() {
  install(DefaultHeaders)
  install(ConditionalHeaders)
  install(PartialContentSupport)

  install(Routing) {
    webSocket("/socket") {
      //https://github.com/Kotlin/ktor/blob/master/ktor-samples/ktor-samples-websocket/src/org/jetbrains/ktor/samples/chat/ChatApplication.kt
      this.send(Frame.Text("hello from ktor websocket"))
      this.handle {
        if(it is Frame.Text) {
          val readText = it.readText()
          System.out.println(readText)
        }
      }

    }
    get("/") {
      call.respond("hi")
    }
  }
}

fun oldSparkMain(args:Array<String>) {
  System.getenv("PORT")?.let {
    spark.Spark.port(it.toInt())
  }
  spark.Spark.get("/") {req,res-> "Hello from Kotlin"}
}