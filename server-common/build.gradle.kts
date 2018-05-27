import org.gradle.kotlin.dsl.*
//import org.jetbrains.kotlin.gradle.dsl.Coroutines

val serialization_version = project.property("serialization_version")

plugins {
    id("kotlin-platform-common")
    id("kotlinx-serialization") version "0.4.2" apply true
}

repositories {
    maven {url = uri("https://kotlin.bintray.com/kotlinx")}
    jcenter()
}

dependencies {
    compile(kotlin("stdlib"))
    compile("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serialization_version")
}

//dependencies {
//    compile "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlin_version"
//    testCompile "org.jetbrains.kotlin:kotlin-test-annotations-common:$kotlin_version"
//    testCompile "org.jetbrains.kotlin:kotlin-test-common:$kotlin_version"
//}
//kotlin {
//  experimental.coroutines = Coroutines.ENABLE
//}