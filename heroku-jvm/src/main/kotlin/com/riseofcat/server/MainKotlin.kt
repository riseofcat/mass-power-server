package com.riseofcat.server

import org.jetbrains.ktor.application.Application
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.application.install
import org.jetbrains.ktor.features.ConditionalHeaders
import org.jetbrains.ktor.features.DefaultHeaders
import org.jetbrains.ktor.features.PartialContentSupport
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.jetty.Jetty
import org.jetbrains.ktor.routing.Routing
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.sessions.sessionOrNull
import org.jetbrains.ktor.websocket.Frame
import org.jetbrains.ktor.websocket.readText
import org.jetbrains.ktor.websocket.webSocket
import spark.Spark
import spark.Spark.get

object MainKotlin {
    @JvmStatic fun main(args: Array<String>) {
        if(true) {
            var port: Int = 5000
            try {
                port = Integer.valueOf(System.getenv("PORT"))
            } catch(e: Exception) {

            }
            embeddedServer(Jetty, port, reloadPackages = listOf("heroku"), module = Application::module/*, host = "localhost"*/).start()
        } else {
            System.getenv("PORT")?.let {
                Spark.port(it.toInt())
            }
            get("/") { req, res -> "Hello from Kotlin" }
        }
    }
}

fun Application.module() {
    install(DefaultHeaders)
    install(ConditionalHeaders)
    install(PartialContentSupport)

    install(Routing) {
        webSocket("/socket") {
            //https://github.com/Kotlin/ktor/blob/master/ktor-samples/ktor-samples-websocket/src/org/jetbrains/ktor/samples/chat/ChatApplication.kt
            this.send(Frame.Text("hello from ktor websocket"))
            JavaHandeKotlin.handle(this)
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


