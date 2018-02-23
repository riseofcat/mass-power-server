import org.gradle.kotlin.dsl.*

plugins {
//  java
//  kotlin("jvm") version "1.2.21"// apply false
  id("kotlin-platform-jvm")
}
//version = "1.0-SNAPSHOT"
//repositories {
//  jcenter()
//}
//buildscript {
//  repositories {
//    mavenCentral()
//  }
//  dependencies {
//    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.21")
//  }
//}
repositories {
  mavenCentral()
  jcenter()
}
//allprojects {
//  group = "io.mass-power"
//  version = "1.0"
//}
dependencies {
  if(true) compile(kotlin("stdlib")) else compile("org.jetbrains.kotlin:kotlin-stdlib:1.2.21")
  compile("com.sparkjava:spark-core:2.7.1")
  compile("org.slf4j:slf4j-simple:1.8.0-beta1")//todo update to stable
  compile(project(":core"))
  expectedBy(project(":server-common"))
}
