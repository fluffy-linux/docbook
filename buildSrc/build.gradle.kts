// Read the configuration properties from the "main" project so that I don't have to
// maintain them in two places.
layout.projectDirectory.file("../gradle.properties").asFile.readLines().forEach { input ->
  val line = input.trim()
  if (!line.isBlank() && !line.startsWith("#") && !line.startsWith("//")) {
    val parts = line.split("=").map { it.trim() }
    val property = parts.first()
    val value = parts.last()

    // Ignore the build-related properties
    if (!property.startsWith("kotlin.") && !property.startsWith("org.gradle.")) {
      project.extra[property] = value
      //println("${property}=${value}")
    }
  }
}

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://maven.saxonica.com/maven") }
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
  implementation(libs.kotlin.gradle.plugin)
}

gradlePlugin {
  plugins {
    create("docbook-build") {
      id = "org.docbook.build.docbook-build"
      implementationClass = "org.docbook.build.DocBookBuildPlugin"
    }
  }
}
