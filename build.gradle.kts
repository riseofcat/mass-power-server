plugins {
  base
  kotlin("jvm") version "1.2.21" apply false
}

allprojects {
  group = "io.mass-power"
  version = "1.0"
  if(false) {
    repositories {
      jcenter()
    }
  }
}
//buildscript {
//  repositories {
//    mavenCentral()
//  }
//  dependencies {
//    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.21")
//  }
//}
//allprojects {
//  group = "io.mass-power"
//  version = "1.0"
//  repositories {
//    mavenCentral()
//    jcenter()
//  }
//}

dependencies {
  // Make the root project archives configuration depend on every sub-project
  subprojects.forEach {
    archives(it)
  }
}


task("copyToLib") {
//  dependsOn("heroku-jvm:build")
//  println(project(":heroku-jvm").tasks)
//  dependsOn(project(":heroku-jvm").tasks["build"])
//  dependsOn.add()
  dependsOn("heroku-jvm:build")
//  mustRunAfter("heroku-jvm:build")
  doLast {
    copy {
      into("$buildDir/libs")
      from(project(":heroku-jvm").configurations["compile"])
    }
    copy {
      from("heroku-jvm/build/libs")
      into("$buildDir/libs")
    }
  }
}

task("stage") {
//  dependsOn("heroku-jvm:clean")
//  dependsOn("heroku-jvm:build")
  dependsOn("clean")
  mustRunAfter("clean")
  dependsOn("copyToLib")
}
//tasks["copyToLib"].dependsOn("heroku-jvm:build")

//task("copyToLib").dependsOn("heroku-jvm:clean")
//tasks["copyToLib"].mustRunAfter("heroku-jvm:build")//in groovy: build.mustRunAfter clean
//tasks["heroku-jvm:build"].mustRunAfter("clean")

task("myTask") {
//  dependsOn("build")
  dependsOn("heroku-jvm:build")
}