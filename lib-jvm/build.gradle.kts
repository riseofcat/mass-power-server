properties["userServerCommon"]
val serialization_version = "0.5.0"
buildscript {
  println("hello buildscript")
  repositories {
    maven {url = uri("https://kotlin.bintray.com/kotlinx")}
  }
  val kotlin_version = "1.2.41"
  val serialization_version = "0.5.0"
  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    classpath("org.jetbrains.kotlinx:kotlinx-gradle-serialization-plugin:$serialization_version")
  }
}

plugins {
  if(false) kotlin("jvm")
  id("kotlin-platform-jvm")
  id("kotlinx-serialization") version "0.5.0" apply true
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_7
  targetCompatibility = JavaVersion.VERSION_1_7
}

repositories {
  maven {url = uri("https://kotlin.bintray.com/kotlinx")}
  jcenter()
}

dependencies {
  compile(kotlin("stdlib"))
  expectedBy(project(":server-common"))

  compile("com.fasterxml.jackson.core:jackson-databind:2.8.5")
  compile("commons-codec:commons-codec:1.9")
  compile("commons-io:commons-io:2.4")
  compile("com.google.code.gson:gson:2.7")
  compile("com.beust:klaxon:2.1.6")
  val webSocketCryzbyVersion = "1.9.1.9.6"
  compile("com.github.czyzby:gdx-websocket:$webSocketCryzbyVersion")
  compile("com.github.czyzby:gdx-websocket-common:$webSocketCryzbyVersion")

  compile("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serialization_version")
}
