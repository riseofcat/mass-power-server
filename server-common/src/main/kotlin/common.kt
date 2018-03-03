object ServerCommon {
  fun test():String {
    val k = "server"
    val m = "common"
    return "$k $m"
  }
}

expect fun Any.toJson():String
expect fun <T>String.fromJson():T