import org.gradle.kotlin.dsl.*

plugins {
  id("kotlin-platform-jvm")
}
repositories {
  mavenCentral()
  jcenter()
}
dependencies {
  compile(kotlin("stdlib"))
  if(false) compile("org.jetbrains.kotlin:kotlin-stdlib:1.2.21")
  compile("com.sparkjava:spark-core:2.7.1")
  compile("org.slf4j:slf4j-simple:1.8.0-beta1")//todo update to stable
  compile(project(":jvm-lib"))
  expectedBy(project(":server-common"))
}
