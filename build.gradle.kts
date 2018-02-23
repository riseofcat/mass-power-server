plugins {
  base
//  java
  kotlin("jvm") version "1.2.21" apply false
}

if(false)allprojects {
  group = "io.mass-power"
  version = "1.0"
}

if(false)dependencies {
  // Make the root project archives configuration depend on every sub-project
  subprojects.forEach {
    archives(it)
  }
}

task("copyToLib") {
  dependsOn("heroku-jvm:build")
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
  dependsOn("clean")
  mustRunAfter("clean")
  dependsOn("copyToLib")
}
