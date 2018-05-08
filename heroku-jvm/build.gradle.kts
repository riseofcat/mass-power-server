import com.github.jengelman.gradle.plugins.shadow.tasks.*
import com.github.jengelman.gradle.plugins.shadow.transformers.*
import org.gradle.kotlin.dsl.*

plugins {
  id("kotlin-platform-jvm")
  application //needs only for local launch ./gradlew heroku-jvm:run
  id("com.github.johnrengelman.shadow").version("2.0.2")
  id("kotlinx-serialization") version "0.5.0" apply true
}
application { //needs only for local launch
  mainClassName = "com.riseofcat.server.MainKtorKt"
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

version = "1.0" //Если изменить версию, то поменяется имя jar-ника

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
  maven { url = uri("http://dl.bintray.com/kotlin/ktor") }
  maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
}
dependencies {
  compile(kotlin("stdlib"))
  if(false) compile("org.jetbrains.kotlin:kotlin-stdlib:SPECIFY_KOTLIN_VERSION")
  compile("com.sparkjava:spark-core:2.7.1")//http://sparkjava.com/documentation//todo delete
  compile("org.slf4j:slf4j-simple:1.8.0-beta1")//todo update to stable

  val ktorVersion = "0.9.2"

  compile("io.ktor:ktor-server-core:$ktorVersion")
  compile("io.ktor:ktor-websockets:$ktorVersion")
  compile("io.ktor:ktor-server-cio:$ktorVersion")
  compile("io.ktor:ktor-server-netty:$ktorVersion")

//    compile("io.ktor:ktor-server-jetty:$ktorVersion")

  compile("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.3")
  compile(project(":lib-jvm"))
  if(false)expectedBy(project(":server-common"))//эта зависимост идёт через lib-jvm

//  kotlin {//todo groovy to kts
//    experimental {
//      coroutines "enable"
//    }
//  }
}
