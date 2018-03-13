package app

import com.riseofcat.common.*
import com.riseofcat.lib.*
import spark.Spark
import spark.Spark.*


object ServerMain {
  @JvmStatic fun main(args: Array<String>) {
    System.getenv("PORT")?.let {
      Spark.port(it.toInt())
    }
    get("/") { req, res -> "heroku-jvm: ${LibJava.test()} ${ServerCommon.test()}" }
  }
}
