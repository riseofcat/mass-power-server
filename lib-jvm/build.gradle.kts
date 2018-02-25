properties["userServerCommon"]
plugins {
  if(false) kotlin("jvm")
  id("kotlin-platform-jvm")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
  jcenter()
}

dependencies {
  compile(kotlin("stdlib"))
  expectedBy(project(":server-common"))

  compile("com.fasterxml.jackson.core:jackson-databind:2.8.5")
  compile("commons-codec:commons-codec:1.9")
  compile("commons-io:commons-io:2.4")
}
