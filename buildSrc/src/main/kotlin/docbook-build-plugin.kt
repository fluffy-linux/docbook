package org.docbook.build

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import org.gradle.api.artifacts.Configuration
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.create

interface DocBookBuildExtension {
  abstract val project: Property<Project>
  abstract val buildTime: Property<String>
  abstract val buildDate: Property<String>
  abstract val buildDateId: Property<String>

  fun gitHash(): String {
    val commandLine = listOf("git", "rev-parse", "--short", "HEAD")
    val builder = ProcessBuilder(commandLine)
    val process = builder.start()

    val stdout = ByteArrayOutputStream()
    val stderr = ByteArrayOutputStream()

    val stdoutReader = ProcessOutputReader(process.inputStream, stdout)
    val stderrReader = ProcessOutputReader(process.errorStream, stderr)

    val stdoutThread = Thread(stdoutReader)
    val stderrThread = Thread(stderrReader)

    stdoutThread.start()
    stderrThread.start()

    val rc = process.waitFor()
    stdoutThread.join()
    stderrThread.join()

    if (rc != 0) {
      throw RuntimeException("Attempt to get git hash failed: ${rc}")
    }

    return stdout.toString().trim()
  }

  fun buildId(): String {
    return "${gitHash()}.${buildDateId.get()}"
  }
}

internal class ProcessOutputReader(val stream: InputStream,
                                   val buffer: ByteArrayOutputStream): Runnable {
  override fun run() {
    val reader = InputStreamReader(stream)
    val buf = CharArray(4096)
    var len = reader.read(buf)
    while (len >= 0) {
      if (len == 0) {
        Thread.sleep(250)
      } else {
        // This is the most efficient way? Really!?
        for (pos in 0 until len) {
          buffer.write(buf[pos].code)
        }
      }
      len = reader.read(buf)
    }
  }
}

class DocBookBuildPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val buildTime = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS);
    val extension = project.extensions.create<DocBookBuildExtension>("docbookBuild")
    extension.project.set(project)
    extension.buildTime.set(DateTimeFormatter.ISO_INSTANT.format(buildTime))
    extension.buildDate.set(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(buildTime))

    if (project.findProperty("com.xmlcalabash.onlyDailyBuildId").toString() == "true") {
      extension.buildDateId.set(String.format("%02x%x.%02xHHMM",
                                              buildTime.year % 100,
                                              buildTime.monthValue,
                                              buildTime.dayOfMonth))
    } else {
      extension.buildDateId.set(String.format("%02x%x.%02x%02x%02x",
                                              buildTime.year % 100,
                                              buildTime.monthValue,
                                              buildTime.dayOfMonth, buildTime.hour, buildTime.minute))
    }
  }
}
