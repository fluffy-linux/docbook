import org.docbook.build.*
import org.docbook.gradle.*
import java.io.PrintStream

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

val docbook = tasks.register<DocBookPipeline>("buildDocBook") {
  inputs.dir(layout.projectDirectory.file("src/main"))
  inputs.dir(layout.projectDirectory.dir("../../tools"))
  outputs.dir(layout.buildDirectory.dir("tmp/docbook"))

  pipeline = layout.projectDirectory.file("../../tools/build-schema.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "docbook",
      "release" to "DocBook V${docbookVersion}"
  )
}

val docbookxi = tasks.register<DocBookPipeline>("buildDocBookXi") {
  inputs.dir(layout.projectDirectory.file("src/main"))
  inputs.dir(layout.projectDirectory.dir("../../tools"))
  outputs.dir(layout.buildDirectory.dir("tmp/docbookxi"))

  pipeline = layout.projectDirectory.file("../../tools/build-schema.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "docbookxi",
      "release" to "DocBook V${docbookVersion}"
  )
}

val assembly = tasks.register<DocBookPipeline>("buildAssembly") {
  inputs.dir(layout.projectDirectory.file("src/main"))
  inputs.dir(layout.projectDirectory.dir("../../tools"))
  outputs.dir(layout.buildDirectory.dir("tmp/assembly"))

  pipeline = layout.projectDirectory.file("../../tools/build-schema.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "assembly",
      "release" to "DocBook Assembly V${docbookVersion}"
  )
}

val assemblyxi = tasks.register<DocBookPipeline>("buildAssemblyXi") {
  inputs.dir(layout.projectDirectory.file("src/main"))
  inputs.dir(layout.projectDirectory.dir("../../tools"))
  outputs.dir(layout.buildDirectory.dir("tmp/assemblyxi"))

  pipeline = layout.projectDirectory.file("../../tools/build-schema.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "assemblyxi",
      "release" to "DocBook Assembly V${docbookVersion}"
  )
}

val dbits = tasks.register<DocBookPipeline>("buildDocBookITS") {
  inputs.dir(layout.projectDirectory.file("src/main"))
  inputs.dir(layout.projectDirectory.dir("../../tools"))
  outputs.dir(layout.buildDirectory.dir("tmp/dbits"))

  pipeline = layout.projectDirectory.file("../../tools/build-schema.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "dbits",
      "release" to "DocBook ITS V${docbookVersion}"
  )
}

val dbitsxi = tasks.register<DocBookPipeline>("buildDocBookITSxi") {
  inputs.dir(layout.projectDirectory.file("src/main"))
  inputs.dir(layout.projectDirectory.dir("../../tools"))
  outputs.dir(layout.buildDirectory.dir("tmp/dbitsxi"))

  pipeline = layout.projectDirectory.file("../../tools/build-schema.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "dbitsxi",
      "release" to "DocBook ITS V${docbookVersion}"
  )
}

val schemas = tasks.register("schemas") {
  dependsOn(docbook, docbookxi, assembly, assemblyxi, dbits, dbitsxi)
}

val docbookResources = tasks.register<DocBookResources>("docbookResources") {
  output = layout.buildDirectory.dir("tmp/release-notes")
}

val docbookRelNoteResources = tasks.register<Copy>("docbookReleaseNoteResources") {
  // The release notes actually use only a single CSS file
  dependsOn(docbookResources)
  into(layout.buildDirectory.dir("docbook-${docbookVersion}/release-notes"))
  from(layout.buildDirectory.dir("tmp/release-notes")) {
    include("css/docbook.css")
  }
}

val docbookReleaseNotes = tasks.register<DocBookPipeline>("docbookReleaseNotes") {
  dependsOn(docbookRelNoteResources)
  inputs.file(layout.projectDirectory.file("src/main/xml/release-notes.xml"))
  inputs.dir(layout.projectDirectory.file("../../tools"))
  outputs.dir(layout.buildDirectory.dir("docbook-${docbookVersion}/release-notes"))

  pipeline = layout.projectDirectory.file("../../tools/format.xpl")
  source = layout.projectDirectory.file("src/main/xml/release-notes.xml")
  result = layout.buildDirectory.file("docbook-${docbookVersion}/release-notes/index.html")

  doLast {
    copy {
      into(layout.buildDirectory.dir("docbook-${docbookVersion}/release-notes"))
      from(layout.projectDirectory.file("src/main/xml")) {
        include("release-notes.xml")
      }
    }
  }
}

val copyBuildArtifacts = tasks.register("copyBuildArtifacts") {
  dependsOn(docbook, docbookxi, assembly, assemblyxi, dbits, dbitsxi, docbookReleaseNotes)
  inputs.files(docbook)
  inputs.files(docbookxi)
  inputs.files(assembly)
  inputs.files(assemblyxi)
  inputs.files(dbits)
  inputs.files(dbitsxi)
  outputs.dir(layout.buildDirectory.dir("docbook-${docbookVersion}"))

  doLast {
    copy {
      into(layout.buildDirectory.dir("docbook-${docbookVersion}"))
      from(docbook) { include("rng/*") }
      from(docbookxi) { include("rng/*") }
      from(assembly) { include("rng/*") }
      from(assemblyxi) { include("rng/*") }
      from(dbits) { include("rng/*") }
      from(dbitsxi) { include("rng/*") }
    }

    copy {
      into(layout.buildDirectory.dir("docbook-${docbookVersion}"))
      from(docbook) { include("sch/*") }
      from(docbookxi) { include("sch/*") }
      from(assembly) { include("sch/*") }
      from(assemblyxi) { include("sch/*") }
      from(dbits) { include("sch/*") }
      from(dbitsxi) { include("sch/*") }
    }

    copy {
      into(layout.buildDirectory.dir("docbook-${docbookVersion}"))
      from(layout.projectDirectory.dir("src/main/nvdl")) {
        include("*.nvdl")
      }
    }
  }
}

val stage = tasks.register<DocBookPipeline>("stage") {
  dependsOn(copyBuildArtifacts)
  inputs.dir(layout.projectDirectory.file("../../tools"))
  inputs.dir(layout.buildDirectory.dir("docbook-${docbookVersion}"))
  outputs.file(layout.buildDirectory.dir("docbook-${docbookVersion}/catalog.xml"))

  pipeline = layout.projectDirectory.file("../../tools/build-catalog.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  result = layout.buildDirectory.file("docbook-${docbookVersion}/catalog.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory}",
      "schema" to "docbook",
      "version" to docbookVersion,
      "title" to "XML Catalog for DocBook V${docbookVersion}"
  )
}


val testDocBook = tasks.register<DocBookPipeline>("testDocBook") {
  dependsOn(stage)
  outputs.file(layout.buildDirectory.file("test-reports/docbook.xml"))
  inputs.dir(layout.projectDirectory.dir("src/test/docbook"))

  doFirst {
    mkdir(layout.buildDirectory.file("test-reports"))
  }

  pipeline = layout.projectDirectory.file("../../tools/test-suite.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory.file("src/test/docbook")}",
      "rngschema" to "${layout.buildDirectory.file("docbook-${docbookVersion}/rng/docbook.rng").get().asFile}",
      "schschema" to "${layout.buildDirectory.file("docbook-${docbookVersion}/sch/docbook.sch").get().asFile}",
      "report" to "${layout.buildDirectory.file("test-reports/docbook.xml").get().asFile}"
  )
}

val testDocBookXI = tasks.register<DocBookPipeline>("testDocBookXI") {
  dependsOn(stage)
  outputs.file(layout.buildDirectory.file("test-reports/docbookxi.xml"))
  inputs.dir(layout.projectDirectory.dir("src/test/docbookxi"))

  doFirst {
    mkdir(layout.buildDirectory.file("test-reports"))
  }

  pipeline = layout.projectDirectory.file("../../tools/test-suite.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory.file("src/test/docbookxi")}",
      "rngschema" to "${layout.buildDirectory.file("docbook-${docbookVersion}/rng/docbookxi.rng").get().asFile}",
      "schschema" to "${layout.buildDirectory.file("docbook-${docbookVersion}/sch/docbookxi.sch").get().asFile}",
      "report" to "${layout.buildDirectory.file("test-reports/docbookxi.xml").get().asFile}"
  )
}

val testAssembly = tasks.register<DocBookPipeline>("testAssembly") {
  dependsOn(stage)
  outputs.file(layout.buildDirectory.file("test-reports/assembly.xml"))
  inputs.dir(layout.projectDirectory.dir("src/test/assembly"))

  doFirst {
    mkdir(layout.buildDirectory.file("test-reports"))
  }

  pipeline = layout.projectDirectory.file("../../tools/test-suite.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory.file("src/test/assembly")}",
      "rngschema" to "${layout.buildDirectory.file("docbook-${docbookVersion}/rng/assembly.rng").get().asFile}",
      "schschema" to "${layout.buildDirectory.file("docbook-${docbookVersion}/sch/assembly.sch").get().asFile}",
      "report" to "${layout.buildDirectory.file("test-reports/assembly.xml").get().asFile}"
  )
}

val testAssemblyXI = tasks.register("testAssemblyXI") {
  dependsOn(stage)
  //outputs.file(layout.buildDirectory.file("test-reports/assemblyxi.xml"))
  //inputs.dir(layout.projectDirectory.dir("src/test/assemblyxi"))
  doLast {
    //println("FIXME: there are no Xssembly XI tests")
  }
}

val testBits = tasks.register<DocBookPipeline>("testBits") {
  dependsOn(stage)
  outputs.file(layout.buildDirectory.file("test-reports/docbook.xml"))
  inputs.dir(layout.projectDirectory.dir("src/test/dbits"))

  doFirst {
    mkdir(layout.buildDirectory.file("test-reports"))
  }

  pipeline = layout.projectDirectory.file("../../tools/test-suite.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory.file("src/test/dbits")}",
      "rngschema" to "${layout.buildDirectory.file("docbook-${docbookVersion}/rng/dbits.rng").get().asFile}",
      "schschema" to "${layout.buildDirectory.file("docbook-${docbookVersion}/sch/dbits.sch").get().asFile}",
      "report" to "${layout.buildDirectory.file("test-reports/dbits.xml").get().asFile}"
  )
}

val testBitsXI = tasks.register<DocBookPipeline>("testBitsXI") {
  dependsOn(stage)
  outputs.file(layout.buildDirectory.file("test-reports/docbook.xml"))
  inputs.dir(layout.projectDirectory.dir("src/test/dbitsxi"))

  doFirst {
    mkdir(layout.buildDirectory.file("test-reports"))
  }

  pipeline = layout.projectDirectory.file("../../tools/test-suite.xpl")
  config = layout.projectDirectory.file("../../tools/xmlcalabash.xml")
  params = mapOf(
      "path" to "${layout.projectDirectory.file("src/test/dbitsxi")}",
      "rngschema" to "${layout.buildDirectory.file("docbook-${docbookVersion}/rng/dbitsxi.rng").get().asFile}",
      "schschema" to "${layout.buildDirectory.file("docbook-${docbookVersion}/sch/dbitsxi.sch").get().asFile}",
      "report" to "${layout.buildDirectory.file("test-reports/dbitsxi.xml").get().asFile}"
  )
}

val test = tasks.register("schematest") {
  dependsOn(testDocBook, testDocBookXI, testAssembly, testAssemblyXI, testBits, testBitsXI)
}

val release = tasks.register<Zip>("release") {
  dependsOn(stage, test)
  from(layout.buildDirectory.dir("docbook-${docbookVersion}"))
  into("docbook-${docbookVersion}/")
  archiveFileName = "docbook-${docbookVersion}.zip"
}

// ============================================================

val jarsetup = tasks.register("jarsetup") {
  dependsOn(stage)
  inputs.dir(layout.projectDirectory.dir("../../src/main/schemas/docbook"))
  inputs.dir(layout.buildDirectory.dir("docbook-${docbookVersion}"))
  outputs.dir(layout.buildDirectory.dir("tmp/stage-jar"))

  val dbv = docbookVersion

  doLast {
    copy {
      into(layout.buildDirectory.dir(
               "tmp/stage-jar/docbook-${dbv}/org/docbook/schemas/docbook"))
      from(layout.projectDirectory.dir("../../src/main/schemas/docbook")) {
        exclude("catalog.src")
      }
    }
    copy {
      into(layout.buildDirectory.dir(
               "tmp/stage-jar/docbook-${dbv}/org/docbook/schemas/docbook/${dbv}"))
      from(layout.buildDirectory.dir("docbook-${dbv}")) {
        exclude("release-notes")
      }
    }
    copy {
      into(layout.buildDirectory.dir(
               "tmp/stage-jar/docbook-${dbv}/org/docbook/schemas/docbook"))
      from(layout.projectDirectory.dir("../../src/main/schemas/docbook")) {
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
      "tmp/stage-jar/docbook-${dbv}/org/docbook/schemas/docbook/${dbv}/catalog.xml")
  result = layout.buildDirectory.file(
      "tmp/stage-jar/docbook-${dbv}/org/xmlresolver/catalog.xml")
  params = mapOf(
      "schema" to "docbook",
      "version" to docbookVersion
  )
}

tasks.jar {
  dependsOn(release, "compileJava", jarcatalog, test)
  from(layout.buildDirectory.dir("tmp/stage-jar/docbook-${docbookVersion}"))
  from(layout.buildDirectory.dir("classes/java/docbook"))
  archiveBaseName = "docbook-${docbookVersion}"
  manifest {
    attributes(mapOf("Built-By" to "Norman Walsh",
                     "Implementation-Vendor" to "Norman Walsh",
                     "Implementation-Title" to "DocBook Schemas",
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

tasks.register<Zip>("mavenRelease") {
  dependsOn("publish")

  from(layout.buildDirectory.dir("maven"))
  archiveFileName = "docbook-maven-${docbookVersion}.zip"
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      pom {
        name = "DocBook Schemas"
        packaging = "jar"
        description = "The DocBook schemas"
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
      artifactId = "schemas-docbook"
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

val defguide_allPatterns = tasks.register<DocBookPipeline>("defguide_allPatterns") {
  dependsOn(docbook)
  inputs.file(layout.buildDirectory.file("tmp/docbook/rng/docbook.rng"))
  outputs.file(layout.buildDirectory.file("tmp/defguide/any-docbook.rng"))

  pipeline = layout.projectDirectory.file("../../tools/all-patterns.xpl")
  source = layout.buildDirectory.file("tmp/docbook/rng/docbook.rng")
  result = layout.buildDirectory.file("tmp/defguide/any-docbook.rng")
}

val defguideSrc = tasks.register("defguide_rng_src") {
  dependsOn(docbook, assembly, defguide_allPatterns)
  outputs.file(layout.buildDirectory.file("tmp/defguide/defguide.rng"))

  doFirst {
    mkdir(layout.buildDirectory.file("tmp/defguide"))
  }

  doLast {
    // The defguide.rng file is just a driver that combines 
    // a few schemas together for documentation purposes.
    // it doesn't exist to perform validation of any sort.
    val rng = PrintStream(layout.buildDirectory.file("tmp/defguide/defguide.rng").get().asFile)
    rng.println("<?xml version='1.0' encoding='UTF-8'?>")
    rng.println("<grammar ns='http://docbook.org/ns/docbook'")
    rng.println("         xmlns='http://relaxng.org/ns/structure/1.0'>")
    rng.println("  <include href='../../tmp/src/docbook/docbook.rng'>")
    rng.println("    <start combine='choice'>")
    rng.println("      <ref name='db.assembly'/>")
    rng.println("    </start>")
    rng.println("  </include>")
    rng.println("  <include href='../../tmp/defguide/any-docbook.rng'/>")
    rng.println("  <include href='../../tmp/src/assembly/assembly-core.rng'/>")
    rng.println("</grammar>")
    rng.close()
  }
}

tasks.register<DocBookPipeline>("defguide") {
  dependsOn(defguideSrc)

  pipeline = layout.projectDirectory.file("../../tools/defguide.xpl")
  source = layout.buildDirectory.file("tmp/defguide/defguide.rng")
  result = layout.buildDirectory.file("defguide.rnd")
  params = mapOf(
      "buildhash" to docbookBuild.gitHash()
  )
}
