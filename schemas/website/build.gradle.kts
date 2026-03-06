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

val website = tasks.register<DocBookPipeline>("buildWebsite") {
  inputs.dir(layout.projectDirectory.file("src/main/website"))
  inputs.dir(layout.projectDirectory.file("../../tools"))
  outputs.dir(layout.buildDirectory.dir("tmp/website"))

  pipeline = layout.projectDirectory.file("../../tools/build-schema.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "website",
      "release" to "DocBook Website V${docbookVersion}"
  )
}

val websitefull = tasks.register<DocBookPipeline>("buildWebsiteFull") {
  inputs.dir(layout.projectDirectory.file("src/main/website-full"))
  inputs.dir(layout.projectDirectory.file("../../tools"))
  outputs.dir(layout.buildDirectory.dir("tmp/website-full"))

  pipeline = layout.projectDirectory.file("../../tools/build-schema.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "website-full",
      "release" to "DocBook Website Full V${docbookVersion}"
  )
}

val schemas = tasks.register("schemas") {
  dependsOn(website, websitefull)
}

val copyBuildArtifacts = tasks.register("copyBuildArtifacts") {
  dependsOn(schemas)
  inputs.files(website)
  inputs.files(websitefull)
  outputs.dir(layout.buildDirectory.dir("website-${docbookVersion}"))

  doLast {
    copy {
      into(layout.buildDirectory.dir("website-${docbookVersion}"))
      from(website) { include("rng/*") }
      from(websitefull) { include("rng/*") }
    }

    copy {
      into(layout.buildDirectory.dir("website-${docbookVersion}"))
      from(website) { include("sch/*") }
      from(websitefull) { include("sch/*") }
    }

    copy {
      into(layout.buildDirectory.dir("website-${docbookVersion}"))
      from(layout.projectDirectory.dir("src/main/nvdl")) {
        include("*.nvdl")
      }
    }
  }
}

val stage = tasks.register<DocBookPipeline>("stage") {
  dependsOn(copyBuildArtifacts)
  inputs.dir(layout.projectDirectory.file("../../tools"))
  inputs.dir(layout.buildDirectory.dir("website-${docbookVersion}"))
  outputs.file(layout.buildDirectory.dir("website-${docbookVersion}/catalog.xml"))

  pipeline = layout.projectDirectory.file("../../tools/build-catalog.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  result = layout.buildDirectory.file("website-${docbookVersion}/catalog.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "website",
      "prefix" to "website",
      "version" to docbookVersion,
      "title" to "XML Catalog for DocBook Website V${docbookVersion}"
  )
}

val testWebsite = tasks.register<DocBookPipeline>("testWebsite") {
  dependsOn(stage)
  outputs.file(layout.buildDirectory.file("test-reports/website.xml"))
  inputs.dir(layout.projectDirectory.dir("src/test/website"))

  doFirst {
    mkdir(layout.buildDirectory.file("test-reports"))
  }

  pipeline = layout.projectDirectory.file("../../tools/test-suite.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory.file("src/test/website")}",
      "rngschema" to "${layout.buildDirectory.file("website-${docbookVersion}/rng/website.rng").get().asFile}",
      "schschema" to "${layout.buildDirectory.file("website-${docbookVersion}/sch/website.sch").get().asFile}",
      "report" to "${layout.buildDirectory.file("test-reports/website.xml").get().asFile}"
  )
}

val testWebsiteFull = tasks.register<DocBookPipeline>("testWebsiteFull") {
  dependsOn(stage)
  outputs.file(layout.buildDirectory.file("test-reports/website-full.xml"))
  inputs.dir(layout.projectDirectory.dir("src/test/website-full"))

  doFirst {
    mkdir(layout.buildDirectory.file("test-reports"))
  }

  pipeline = layout.projectDirectory.file("../../tools/test-suite.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory.file("src/test/website-full")}",
      "rngschema" to "${layout.buildDirectory.file("website-${docbookVersion}/rng/website-full.rng").get().asFile}",
      "schschema" to "${layout.buildDirectory.file("website-${docbookVersion}/sch/website-full.sch").get().asFile}",
      "report" to "${layout.buildDirectory.file("test-reports/website-full.xml").get().asFile}"
  )
}

val test = tasks.register("schematest") {
  dependsOn(testWebsite, testWebsiteFull)
}

val release = tasks.register<Zip>("release") {
  dependsOn(stage, test)
  from(layout.buildDirectory.dir("website-${docbookVersion}"))
  into("website-${docbookVersion}/")
  archiveFileName = "website-${docbookVersion}.zip"
}

tasks.register<DocBookPipeline>("defguide") {
  dependsOn(website)

  pipeline = layout.projectDirectory.file("../../tools/defguide.xpl")
  source = layout.buildDirectory.file("tmp/src/website/website.rng")
  result = layout.buildDirectory.file("website.rnd")
  params = mapOf(
      "buildhash" to docbookBuild.gitHash()
  )
}
