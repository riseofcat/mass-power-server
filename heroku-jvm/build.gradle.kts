import org.gradle.kotlin.dsl.*

plugins {
//  base
//  java
//  kotlin("jvm") /*version "1.2.21"*/// apply false
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
allprojects {
  group = "io.mass-power"
  version = "1.0"
  repositories {
    mavenCentral()
    jcenter()
  }
}
dependencies {
  if(true) compile(kotlin("stdlib")) else compile("org.jetbrains.kotlin:kotlin-stdlib:1.2.21")
  compile("com.sparkjava:spark-core:+")
  compile("org.slf4j:slf4j-simple:+")
  compile(project(":core"))
  expectedBy(project(":server-common"))
}

//task("copyToLib") {
//  doLast {
//    copy {
//      into("${parent!!.buildDir}/libs")
//      from(configurations.compile)
//    }
//  }
//}
//
//task("stage") {
//  dependsOn.add("clean")
//  dependsOn.add("build")
//  dependsOn.add("copyToLib")
//}

//tasks["build"].mustRunAfter(tasks["clean"])//in groovy: build.mustRunAfter clean
