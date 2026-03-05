import org.docbook.gradle.*

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("org.docbook.build.docbook-build")
  id("org.docbook.xsltng-gradle") version "0.6.0"
}

val docbookVersion = project.properties["docbookVersion"].toString() 

configure<DocBookExtension> {
  classpath = configurations["transform"]
}
