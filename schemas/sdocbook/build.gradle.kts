import org.docbook.build.*
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
val docbookBuild = the<DocBookBuildExtension>()

configure<DocBookExtension> {
  classpath = configurations["transform"]
}

val sdocbook = tasks.register<DocBookPipeline>("buildSimplifiedDocBook") {
  inputs.dir(layout.projectDirectory.file("src/main/sdocbook"))
  inputs.dir(layout.projectDirectory.file("../../tools"))
  outputs.dir(layout.buildDirectory.dir("tmp/sdocbook"))

  pipeline = layout.projectDirectory.file("../../tools/build-schema.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "sdocbook",
      "release" to "Simplified DocBook V${docbookVersion}"
  )
}

tasks.register("schemas") {
  dependsOn(sdocbook)
}

val copyBuildArtifacts = tasks.register("copyBuildArtifacts") {
  dependsOn(sdocbook)
  inputs.files(sdocbook)
  outputs.dir(layout.buildDirectory.dir("sdocbook-${docbookVersion}"))

  doLast {
    copy {
      into(layout.buildDirectory.dir("sdocbook-${docbookVersion}"))
      from(sdocbook) { include("rng/*") }
    }

    copy {
      into(layout.buildDirectory.dir("sdocbook-${docbookVersion}"))
      from(sdocbook) { include("sch/*") }
    }

    copy {
      into(layout.buildDirectory.dir("sdocbook-${docbookVersion}"))
      from(layout.projectDirectory.dir("src/main/nvdl")) {
        include("*.nvdl")
      }
    }
  }
}

val stage = tasks.register<DocBookPipeline>("stage") {
  dependsOn(copyBuildArtifacts)
  inputs.dir(layout.projectDirectory.file("../../tools"))
  inputs.dir(layout.buildDirectory.dir("sdocbook-${docbookVersion}"))
  outputs.file(layout.buildDirectory.dir("sdocbook-${docbookVersion}/catalog.xml"))

  pipeline = layout.projectDirectory.file("../../tools/build-catalog.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  result = layout.buildDirectory.file("sdocbook-${docbookVersion}/catalog.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "sdocbook",
      "prefix" to "sdocbook",
      "version" to docbookVersion,
      "title" to "XML Catalog for DocBook Forms V${docbookVersion}"
  )
}

val testSimplified = tasks.register<DocBookPipeline>("testSimplified") {
  dependsOn(stage)
  outputs.file(layout.buildDirectory.file("test-reports/sdocbook.xml"))
  inputs.dir(layout.projectDirectory.dir("src/test/sdocbook"))

  doFirst {
    mkdir(layout.buildDirectory.file("test-reports"))
  }

  pipeline = layout.projectDirectory.file("../../tools/test-suite.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory.file("src/test/sdocbook")}",
      "rngschema" to "${layout.buildDirectory.file("sdocbook-${docbookVersion}/rng/sdocbook.rng").get().asFile}",
      "schschema" to "${layout.buildDirectory.file("sdocbook-${docbookVersion}/sch/sdocbook.sch").get().asFile}",
      "report" to "${layout.buildDirectory.file("test-reports/sdocbook.xml").get().asFile}"
  )
}

val test = tasks.register("schematest") {
  dependsOn(testSimplified)
}

val release = tasks.register<Zip>("release") {
  dependsOn(stage, test)
  from(layout.buildDirectory.dir("sdocbook-${docbookVersion}"))
  into("sdocbook-${docbookVersion}/")
  archiveFileName = "sdocbook-${docbookVersion}.zip"
}

tasks.register<DocBookPipeline>("defguide") {
  dependsOn(sdocbook)

  pipeline = layout.projectDirectory.file("../../tools/defguide.xpl")
  source = layout.buildDirectory.file("tmp/src/sdocbook/sdocbook.rng")
  result = layout.buildDirectory.file("sdocbook.rnd")
  params = mapOf(
      "buildhash" to docbookBuild.gitHash()
  )
}
