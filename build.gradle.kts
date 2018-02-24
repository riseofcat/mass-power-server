plugins {
  base
  kotlin("jvm") version "1.2.21" apply false
  if(false)java
}

if(false)allprojects {
  group = "io.mass-power"
  version = "1.0"
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
