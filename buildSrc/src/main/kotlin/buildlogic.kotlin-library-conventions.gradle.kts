plugins {
    id("buildlogic.kotlin-common-conventions")
    `java-library`
}

repositories {
  mavenLocal()
  mavenCentral()
  maven { url = uri("https://maven.saxonica.com/maven") }
}

val saxonVersion = project.properties["saxonVersion"].toString()
val xmlCalabashVersion = project.properties["xmlCalabashVersion"].toString()
val xslTNGversion = project.properties["xslTNGversion"].toString() 
val docbookPublishedVersion = project.properties["docbookPublishedVersion"].toString() 

val transform by configurations.creating {
  extendsFrom(configurations["implementation"])
}

dependencies {
  transform("net.sf.saxon:Saxon-HE:${saxonVersion}")
  transform("org.docbook:schemas-docbook:${docbookPublishedVersion}")
  transform("org.docbook:docbook-xslTNG:${xslTNGversion}")
  transform("com.xmlcalabash:xmlcalabash:${xmlCalabashVersion}")
  transform("com.xmlcalabash:app:${xmlCalabashVersion}")
  transform("io.pebbletemplates:pebble:4.1.1")
}
