plugins {
  base
  kotlin("jvm") version "1.2.30" apply false
  if(false)java
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
