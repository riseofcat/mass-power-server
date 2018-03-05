//package com.riseofcat.lib
//
//import kotlin.reflect.*
//
//data class IntJson(var value:Int = 0):BaseJson {
//  override fun parseJson(jsonString:String) {
//    value = jsonString.toInt()
//  }
//  override fun toJson() = value.toString()
//}
//
//data class StringJson(var value:String = ""):BaseJson {
//  override fun parseJson(jsonString:String) {
//    value = jsonString.replace("\"", "")
//  }
//  override fun toJson() = "\"$value\""
//}
//
//data class TestComplexJson(
//  var i:IntJson,
//  var s:StringJson
//):ContainerJson {
//  override val jsonArgs:List<JsonArg<BaseJson>>
//    get() = listOf(
//      JsonArg("i", {i}, {i=it}, {IntJson()}),
//      JsonArg("s", {s}, {s=it}, {StringJson()})
//    )
//}
//
//interface BaseJson {
//  fun parseJson(jsonString:String)
//  fun toJson():String
//}
//
//class JsonArg<T:BaseJson>(val name:String, val getter:()->T, val setter:(T)->Unit, val factory:()->T)
//
//interface ContainerJson:BaseJson {
//  val jsonArgs:List<JsonArg<BaseJson>>
//  override fun parseJson(jsonString:String) {
//    for(arg in jsonArgs) {
//      arg.setter(arg.factory().apply {parseJson("todo")})
//    }
//  }
//  override fun toJson():String {
//
//  }
//}
//
//fun test() {
//  val constructors:Collection<KFunction<IntJson>> = IntJson::class.constructors
//  for(constructor in constructors) {
//    constructor.call()
//  }
//}
