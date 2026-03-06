import org.docbook.build.*
import org.docbook.gradle.*

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("org.docbook.build.docbook-build")
  id("java-library")
  id("maven-publish")
  id("signing")
  id("com.github.gmazzo.buildconfig") version "5.5.0"
  id("org.docbook.xsltng-gradle") version "0.6.0"
}

val docbookVersion = project.properties["docbookVersion"].toString() 
val docbookBuild = the<DocBookBuildExtension>()

configure<DocBookExtension> {
  classpath = configurations["transform"]
}

val publishers = tasks.register<DocBookPipeline>("buildPublishers") {
  inputs.dir(layout.projectDirectory.file("src/main/publishers"))
  inputs.dir(layout.projectDirectory.file("../../tools"))
  outputs.dir(layout.buildDirectory.dir("tmp/publishers"))

  pipeline = layout.projectDirectory.file("../../tools/build-schema.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "publishers",
      "release" to "DocBook Publishers V${docbookVersion}"
  )
}

tasks.register("schemas") {
  dependsOn(publishers)
}

val copyBuildArtifacts = tasks.register("copyBuildArtifacts") {
  dependsOn(publishers)
  inputs.files(publishers)
  outputs.dir(layout.buildDirectory.dir("publishers-${docbookVersion}"))

  doLast {
    copy {
      into(layout.buildDirectory.dir("publishers-${docbookVersion}"))
      from(publishers) { include("rng/*") }
    }

    copy {
      into(layout.buildDirectory.dir("publishers-${docbookVersion}"))
      from(publishers) { include("sch/*") }
    }

    copy {
      into(layout.buildDirectory.dir("publishers-${docbookVersion}"))
      from(layout.projectDirectory.dir("src/main/nvdl")) {
        include("*.nvdl")
      }
    }
  }
}

val stage = tasks.register<DocBookPipeline>("stage") {
  dependsOn(copyBuildArtifacts)
  inputs.dir(layout.projectDirectory.file("../../tools"))
  inputs.dir(layout.buildDirectory.dir("publishers-${docbookVersion}"))
  outputs.file(layout.buildDirectory.dir("publishers-${docbookVersion}/catalog.xml"))

  pipeline = layout.projectDirectory.file("../../tools/build-catalog.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  result = layout.buildDirectory.file("publishers-${docbookVersion}/catalog.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "publishers",
      "prefix" to "publishers",
      "version" to docbookVersion,
      "title" to "XML Catalog for DocBook Publishers V${docbookVersion}"
  )
}

val testPublishers = tasks.register<DocBookPipeline>("testPublishers") {
  dependsOn("stage")
  outputs.file(layout.buildDirectory.file("test-reports/publshers.xml"))
  inputs.dir(layout.projectDirectory.dir("src/test/publishers"))

  doFirst {
    mkdir(layout.buildDirectory.file("test-reports"))
  }

  pipeline = layout.projectDirectory.file("../../tools/test-suite.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory.file("src/test/publishers")}",
      "rngschema" to "${layout.buildDirectory.file("publishers-${docbookVersion}/rng/publishers.rng").get().asFile}",
      "schschema" to "${layout.buildDirectory.file("publishers-${docbookVersion}/sch/publishers.sch").get().asFile}",
      "report" to "${layout.buildDirectory.file("test-reports/publishers.xml").get().asFile}"
  )
}

val test = tasks.register("schematest") {
  dependsOn(testPublishers)
}

val release = tasks.register<Zip>("release") {
  dependsOn(stage, test)
  from(layout.buildDirectory.dir("publishers-${docbookVersion}"))
  into("publishers-${docbookVersion}/")
  archiveFileName = "publishers-${docbookVersion}.zip"
}



// ============================================================

val jarsetup = tasks.register("jarsetup") {
  dependsOn(stage)
  inputs.dir(layout.projectDirectory.dir("../../src/main/schemas/publishers"))
  inputs.dir(layout.buildDirectory.dir("publishers-${docbookVersion}"))
  outputs.dir(layout.buildDirectory.dir("tmp/stage-jar"))

  val dbv = docbookVersion

  doLast {
    copy {
      into(layout.buildDirectory.dir(
               "tmp/stage-jar/publishers-${dbv}/org/docbook/schemas/publishers"))
      from(layout.projectDirectory.dir("../../src/main/schemas/publishers")) {
        exclude("catalog.src")
      }
    }
    copy {
      into(layout.buildDirectory.dir(
               "tmp/stage-jar/publishers-${dbv}/org/docbook/schemas/publishers/${dbv}"))
      from(layout.buildDirectory.dir("publishers-${dbv}")) {
        exclude("release-notes")
      }
    }
    copy {
      into(layout.buildDirectory.dir(
               "tmp/stage-jar/publishers-${dbv}/org/docbook/schemas/publishers"))
      from(layout.projectDirectory.dir("../../src/main/schemas/publishers")) {
        include("catalog.src")
      }
      rename { "catalog.xml" }
      filter { line ->
        line.replace("@@DBVER@@", dbv)
      }
    }
  }
}

val jarcatalog = tasks.register<DocBookPipeline>("jarcatalog") {
  dependsOn(jarsetup)

  val dbv = docbookVersion

  pipeline = layout.projectDirectory.file("../../tools/resolver-catalog.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  source = layout.buildDirectory.file(
      "tmp/stage-jar/publishers-${dbv}/org/docbook/schemas/publishers/${dbv}/catalog.xml")
  result = layout.buildDirectory.file(
      "tmp/stage-jar/publishers-${dbv}/org/xmlresolver/catalog.xml")
  params = mapOf(
      "schema" to "publishers",
      "version" to docbookVersion
  )
}

tasks.jar {
  dependsOn(release, "compileJava", jarcatalog, test)
  from(layout.buildDirectory.dir("tmp/stage-jar/publishers-${docbookVersion}"))
  from(layout.buildDirectory.dir("classes/java/docbook"))
  archiveBaseName = "publishers-${docbookVersion}"
  manifest {
    attributes(mapOf("Built-By" to "Norman Walsh",
                     "Implementation-Vendor" to "Norman Walsh",
                     "Implementation-Title" to "DocBook Publishers Schemas",
                     "Implementation-Version" to docbookVersion))
  }
}

val javadocJar = tasks.register<Jar>("javadocJar") {
  dependsOn("javadoc")
  archiveClassifier = "javadoc"
  from(tasks.javadoc)
}

val sourcesJar by tasks.registering(Jar::class) {
  archiveClassifier = "sources"
  from(sourceSets.main.get().allSource)
}

tasks.register<Zip>("mavenReleaseArtifact") {
  dependsOn("publish")

  from(layout.buildDirectory.dir("maven"))
  archiveFileName = "publishers-maven-${docbookVersion}.zip"
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      pom {
        name = "DocBook Publishers Schemas"
        packaging = "jar"
        description = "The DocBook Publishers schemas"
        url = "https://codeberg.org/DocBook/docbook"

        scm {
          url = "scm:git@codeberg.org:DocBook/docbook.git"
          connection = "scm:git@codeberg.org:DocBook/docbook.git"
          developerConnection = "scm:git@codeberg.org:DocBook/docbook.git"
        }

        licenses {
          license {
            name = "Apache License version 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0"
            distribution = "repo"
          }
        }

        developers {
          developer {
            id = "ndw"
            name = "Norman Walsh"
          }
        }
      }

      groupId = "org.docbook"
      artifactId = "schemas-publishers"
      version = docbookVersion
      from(components["java"])
      artifact(sourcesJar.get())
      artifact(javadocJar.get())
    }
  }

  repositories {
    maven {
      url = uri(layout.buildDirectory.dir("maven"))
    }
  }
}

signing {
  sign(publishing.publications["mavenJava"])
}

// ============================================================

tasks.register<DocBookPipeline>("defguide") {
  dependsOn(publishers)

  pipeline = layout.projectDirectory.file("../../tools/defguide.xpl")
  source = layout.buildDirectory.file("tmp/src/publishers/publishers.rng")
  result = layout.buildDirectory.file("publishers.rnd")
  params = mapOf(
      "buildhash" to docbookBuild.gitHash()
  )
}
