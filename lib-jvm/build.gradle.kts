properties["userServerCommon"]
plugins {
  if(false)kotlin("jvm")
  id("kotlin-platform-jvm")
}

repositories {
  jcenter()
}

dependencies {
  compile(kotlin("stdlib"))
  expectedBy(project(":server-common"))
}
