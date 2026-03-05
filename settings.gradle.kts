pluginManagement {
  repositories {
    gradlePluginPortal()
    maven {
      url = java.net.URI("https://nwalsh.com/maven")
    }
  }
}

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "DocBook"

include("schemas:docbook")
include("schemas:dbforms")
include("schemas:publishers")
include("schemas:sdocbook")
include("schemas:slides")
include("schemas:website")
