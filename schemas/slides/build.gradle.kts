import org.docbook.build.*
import org.docbook.gradle.*

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("org.docbook.build.docbook-build")
  id("java-library")
  id("org.docbook.xsltng-gradle") version "0.6.0"
}

val docbookVersion = project.properties["docbookVersion"].toString() 
val docbookBuild = the<DocBookBuildExtension>()

configure<DocBookExtension> {
  classpath = configurations["transform"]
}

val slides = tasks.register<DocBookPipeline>("buildSlides") {
  inputs.dir(layout.projectDirectory.file("src/main/slides"))
  inputs.dir(layout.projectDirectory.file("../../tools"))
  outputs.dir(layout.buildDirectory.dir("tmp/slides"))

  pipeline = layout.projectDirectory.file("../../tools/build-schema.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "slides",
      "release" to "DocBook Slides V${docbookVersion}"
  )
}

val slidesfull = tasks.register<DocBookPipeline>("buildSlidesFull") {
  inputs.dir(layout.projectDirectory.file("src/main/slides-full"))
  inputs.dir(layout.projectDirectory.file("../../tools"))
  outputs.dir(layout.buildDirectory.dir("tmp/slides-full"))

  pipeline = layout.projectDirectory.file("../../tools/build-schema.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "slides-full",
      "release" to "DocBook Slides Full V${docbookVersion}"
  )
}

val schemas = tasks.register("schemas") {
  dependsOn(slides, slidesfull)
}

val copyBuildArtifacts = tasks.register("copyBuildArtifacts") {
  dependsOn(schemas)
  inputs.files(slides)
  inputs.files(slidesfull)
  outputs.dir(layout.buildDirectory.dir("slides-${docbookVersion}"))

  doLast {
    copy {
      into(layout.buildDirectory.dir("slides-${docbookVersion}"))
      from(slides) { include("rng/*") }
      from(slidesfull) { include("rng/*") }
    }

    copy {
      into(layout.buildDirectory.dir("slides-${docbookVersion}"))
      from(slides) { include("sch/*") }
      from(slidesfull) { include("sch/*") }
    }

    copy {
      into(layout.buildDirectory.dir("slides-${docbookVersion}"))
      from(layout.projectDirectory.dir("src/main/nvdl")) {
        include("*.nvdl")
      }
    }
  }
}

val stage = tasks.register<DocBookPipeline>("stage") {
  dependsOn(copyBuildArtifacts)
  inputs.dir(layout.projectDirectory.file("../../tools"))
  inputs.dir(layout.buildDirectory.dir("slides-${docbookVersion}"))
  outputs.file(layout.buildDirectory.dir("slides-${docbookVersion}/catalog.xml"))

  pipeline = layout.projectDirectory.file("../../tools/build-catalog.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  result = layout.buildDirectory.file("slides-${docbookVersion}/catalog.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "slides",
      "prefix" to "slides",
      "version" to docbookVersion,
      "title" to "XML Catalog for DocBook Slides V${docbookVersion}"
  )
}

val testSlides = tasks.register<DocBookPipeline>("testSlides") {
  dependsOn(stage)
  outputs.file(layout.buildDirectory.file("test-reports/slides.xml"))
  inputs.dir(layout.projectDirectory.dir("src/test/slides"))

  doFirst {
    mkdir(layout.buildDirectory.file("test-reports"))
  }

  pipeline = layout.projectDirectory.file("../../tools/test-suite.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory.file("src/test/slides")}",
      "rngschema" to "${layout.buildDirectory.file("slides-${docbookVersion}/rng/slides.rng").get().asFile}",
      "schschema" to "${layout.buildDirectory.file("slides-${docbookVersion}/sch/slides.sch").get().asFile}",
      "report" to "${layout.buildDirectory.file("test-reports/slides.xml").get().asFile}"
  )
}

val testSlidesFull = tasks.register<DocBookPipeline>("testSlidesFull") {
  dependsOn(stage)
  outputs.file(layout.buildDirectory.file("test-reports/slides-full.xml"))
  inputs.dir(layout.projectDirectory.dir("src/test/slides-full"))

  doFirst {
    mkdir(layout.buildDirectory.file("test-reports"))
  }

  pipeline = layout.projectDirectory.file("../../tools/test-suite.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory.file("src/test/slides-full")}",
      "rngschema" to "${layout.buildDirectory.file("slides-${docbookVersion}/rng/slides-full.rng").get().asFile}",
      "schschema" to "${layout.buildDirectory.file("slides-${docbookVersion}/sch/slides-full.sch").get().asFile}",
      "report" to "${layout.buildDirectory.file("test-reports/slides-full.xml").get().asFile}"
  )
}

val test = tasks.register("schematest") {
  dependsOn(testSlides, testSlidesFull)
}

val release = tasks.register<Zip>("release") {
  dependsOn(stage, test)
  from(layout.buildDirectory.dir("slides-${docbookVersion}"))
  into("slides-${docbookVersion}/")
  archiveFileName = "slides-${docbookVersion}.zip"
}

tasks.register<DocBookPipeline>("defguide") {
  dependsOn(slides)

  pipeline = layout.projectDirectory.file("../../tools/defguide.xpl")
  source = layout.buildDirectory.file("tmp/src/slides/slides.rng")
  result = layout.buildDirectory.file("slides.rnd")
  params = mapOf(
      "buildhash" to docbookBuild.gitHash()
  )
}
