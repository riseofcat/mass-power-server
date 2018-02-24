object JvmLib {
  fun test():String {
    return "jvm-lib ${System.getProperties()["java.runtime.version"]}"
  }
}