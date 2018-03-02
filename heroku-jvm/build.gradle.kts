import com.github.jengelman.gradle.plugins.shadow.tasks.*
import com.github.jengelman.gradle.plugins.shadow.transformers.*
import org.gradle.kotlin.dsl.*
import java.net.URI

plugins {
  id("kotlin-platform-jvm")
  application //needs only for local launch ./gradlew heroku-jvm:run
  id("com.github.johnrengelman.shadow").version("2.0.2")
}
application { //needs only for local launch
  mainClassName = "com.riseofcat.server.MainJava"
}
tasks.withType<Jar> {
  manifest {
//    attributes["Implementation-Version"] = "version_from_jar_task"
    attributes["Main-Class"] = application.mainClassName
  }
}

tasks.withType<ShadowJar> {
  mergeServiceFiles {//equivalent transform(ServiceFileTransformer::class.java) { ... }
    setPath("META-INF/services")
    include("org.eclipse.jetty.http.HttpFieldPreEncoder")
  }
}

//create a single Jar with all dependencies
//task fatJar(type: Jar) {
//  manifest {
//    attributes 'Implementation-Title': 'Gradle Jar File Example',
//    'Implementation-Version': version,
//    'Main-Class': 'com.mkyong.DateUtils'
//  }
//  baseName = project.name + '-all'
//  from { configurations.compile.collect { it.isDirectory() ? it : zipTree(it) } }
//  with jar
//}

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

  val old=true
  val oldKtorVersion = "0.3.1"
  val ktorVersion = "0.9.1"
//  val gdxVersion by project
  if(!old) {
    compile("io.ktor:ktor-server-core:$ktorVersion")
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.ktor:ktor-server-jetty:$ktorVersion")
  } else {
    compile("org.jetbrains.ktor:ktor-websockets:$oldKtorVersion")
    compile("org.jetbrains.ktor:ktor-netty:$oldKtorVersion")
    compile("org.jetbrains.ktor:ktor-jetty:$oldKtorVersion")
    compile("org.jetbrains.ktor:ktor-freemarker:$oldKtorVersion")
  }

  compile("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.3")
//  compile("com.badlogicgames.gdx:gdx:$gdxVersion")
  compile("com.google.code.gson:gson:2.7")


  compile(project(":lib-jvm"))
  expectedBy(project(":server-common"))

//  kotlin {//todo groovy to kts
//    experimental {
//      coroutines "enable"
//    }
//  }
}
