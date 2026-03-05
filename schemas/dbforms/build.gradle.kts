import org.docbook.gradle.*

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("org.docbook.build.docbook-build")
  id("java-library")
  id("maven-publish")
  id("signing")
  id("org.docbook.xsltng-gradle") version "0.6.0"
}

val docbookVersion = project.properties["docbookVersion"].toString() 

configure<DocBookExtension> {
  classpath = configurations["transform"]
}

val dbforms = tasks.register<DocBookPipeline>("builddbforms") {
  inputs.dir(layout.projectDirectory.file("src/main/relaxng"))
  inputs.dir(layout.projectDirectory.file("../../tools"))
  outputs.dir(layout.buildDirectory.dir("tmp/dbforms"))

  pipeline = layout.projectDirectory.file("../../tools/build-schema.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "dbforms",
      "release" to "DocBook Forms V${docbookVersion}"
  )
}

tasks.register("schemas") {
  dependsOn(dbforms)
}

val copyBuildArtifacts = tasks.register("copyBuildArtifacts") {
  dependsOn(dbforms)
  outputs.dir(layout.buildDirectory.dir("dbforms-${docbookVersion}"))

  doLast {
    copy {
      into(layout.buildDirectory.dir("dbforms-${docbookVersion}"))
      from(dbforms) { include("rng/*") }
    }

    copy {
      into(layout.buildDirectory.dir("dbforms-${docbookVersion}"))
      from(dbforms) { include("sch/*") }
    }

    copy {
      into(layout.buildDirectory.dir("dbforms-${docbookVersion}"))
      from(layout.projectDirectory.dir("src/main/nvdl")) {
        include("*.nvdl")
      }
    }
  }
}

val stage = tasks.register<DocBookPipeline>("stage") {
  dependsOn(copyBuildArtifacts)
  inputs.dir(layout.projectDirectory.file("../../tools"))
  inputs.dir(layout.buildDirectory.dir("dbforms-${docbookVersion}"))
  outputs.file(layout.buildDirectory.dir("dbforms-${docbookVersion}/catalog.xml"))

  pipeline = layout.projectDirectory.file("../../tools/build-catalog.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  result = layout.buildDirectory.file("dbforms-${docbookVersion}/catalog.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "dbforms",
      "prefix" to "dbforms",
      "version" to docbookVersion,
      "title" to "XML Catalog for DocBook Forms V${docbookVersion}",
      "oasisVersion" to docbookVersion,
      "oasisRelease" to "os"
  )
}

val testForms = tasks.register<DocBookPipeline>("testForms") {
  dependsOn("stage")
  outputs.file(layout.buildDirectory.file("test-reports/dbforms.xml"))
  inputs.dir(layout.projectDirectory.dir("src/test/dbforms"))

  doFirst {
    mkdir(layout.buildDirectory.file("test-reports"))
  }

  pipeline = layout.projectDirectory.file("../../tools/test-suite.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory.file("src/test/dbforms")}",
      "rngschema" to "${layout.buildDirectory.file("dbforms-${docbookVersion}/rng/dbforms.rng").get().asFile}",
      "schschema" to "${layout.buildDirectory.file("dbforms-${docbookVersion}/sch/dbforms.sch").get().asFile}",
      "report" to "${layout.buildDirectory.file("test-reports/dbforms.xml").get().asFile}"
  )
}

val test = tasks.register("schematest") {
  dependsOn(testForms)
}

val release = tasks.register<Zip>("release") {
  dependsOn(stage, test)
  from(layout.buildDirectory.dir("dbforms-${docbookVersion}"))
  into("dbforms-${docbookVersion}/")
  archiveFileName = "dbforms-${docbookVersion}.zip"
}
