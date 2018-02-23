package app

import core.*
import spark.Spark
import spark.Spark.*


object Main {
  @JvmStatic fun main(args: Array<String>) {
    System.getenv("PORT")?.let {
      Spark.port(it.toInt())
    }
    get("/") { req, res -> "Hello World! ${DeepThought.compute()}" }
  }
}
