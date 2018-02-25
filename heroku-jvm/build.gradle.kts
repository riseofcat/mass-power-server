import org.gradle.kotlin.dsl.*
import java.net.URI

plugins {
  id("kotlin-platform-jvm")
}
repositories {
  mavenCentral()
  jcenter()
  maven { url  = URI("http://dl.bintray.com/kotlin/ktor") }
  maven { url = URI("https://dl.bintray.com/kotlin/kotlinx") }
}
dependencies {
  compile(kotlin("stdlib"))
  if(false) compile("org.jetbrains.kotlin:kotlin-stdlib:SPECIFY_KOTLIN_VERSION")
  compile("com.sparkjava:spark-core:2.7.1")
  compile("org.slf4j:slf4j-simple:1.8.0-beta1")//todo update to stable

  val ktorVersion = "0.3.1"//todo обновить
  val gdxVersion = "1.9.6"
  compile("org.jetbrains.ktor:ktor-websockets:$ktorVersion")
  compile("org.jetbrains.ktor:ktor-netty:$ktorVersion")
  compile("org.jetbrains.ktor:ktor-jetty:$ktorVersion")
  compile("org.jetbrains.ktor:ktor-freemarker:$ktorVersion")
  compile("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.3")
  compile("com.badlogicgames.gdx:gdx:$gdxVersion")

  compile(project(":lib-jvm"))
  expectedBy(project(":server-common"))
}
