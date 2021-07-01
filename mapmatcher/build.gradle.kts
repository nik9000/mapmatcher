import java.net.URI

plugins {
  `java-library`
  id("org.cadixdev.licenser") version "0.5.1"
  id("ru.vyarus.quality") version "4.6.0"
  `maven-publish`
  signing
}

group = "io.github.nik9000"
version = "0.0.3"
val isReleaseVersion = false == version.toString().endsWith("SNAPSHOT")

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(
      /*
       * Use java 8 for real work but tell Eclipse we're using 16
       * so we can use it to work with tests in java 16.
       */
      if (System.getProperty("eclipse.launcher") == null) 8 else 16
    ))
  }
  withJavadocJar()
  withSourcesJar()
}

repositories {
  jcenter()
}

dependencies {
  implementation("org.hamcrest:hamcrest:2.2")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.0-M1")
  testImplementation("com.google.code.gson:gson:2.8.6")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.0-M1")
}

tasks.compileTestJava {
  javaCompiler.set(javaToolchains.compilerFor {
    languageVersion.set(JavaLanguageVersion.of(16))
  })
}

tasks.withType<Test>().configureEach {
  environment("version", version)
  useJUnitPlatform()
  javaLauncher.set(javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(16))
  })
}

quality {
  lintOptions = listOf("all")
}

license {
  header = rootProject.file("LICENSE_HEADER")
}

tasks.javadoc {
  javadocTool.set(javaToolchains.javadocToolFor {
    languageVersion.set(JavaLanguageVersion.of(16))
  })
  val o = options
  if (o !is StandardJavadocDocletOptions) {
    throw IllegalArgumentException()
  }
  o.docTitle("MapMatcher")
  o.windowTitle("MapMatcher")
  o.links!!.add("https://docs.oracle.com/en/java/javase/16/docs/api/")
  o.links!!.add("http://hamcrest.org/JavaHamcrest/javadoc/2.2/")
  o.addBooleanOption("Xdoclint:all,-missing", true)
  o.showFromPublic()
}

/*
 * Configure publication
 * Thanks https://dev.to/kengotoda/deploying-to-ossrh-with-gradle-in-2020-1lhi
 */
publishing {
  repositories {
    maven {
      url = if (isReleaseVersion)
          URI("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
        else
          URI("https://s01.oss.sonatype.org/content/repositories/snapshots/")
      credentials {
        username = project.findProperty("ossrh.username")?.toString()
        password = project.findProperty("ossrh.password")?.toString()
      }
    }
  }
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
      pom {
        name.set("mapmatcher")
        description.set("""
          Hamcrest matchers for Map and List that match all elements at once so
          the failure messages show the whole structure, calling out
          differences.
        """.trimIndent().replace("\n",""))
        url.set("https://github.com/nik9000/mapmatcher")
        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }
        scm {
          connection.set("scm:git:git@github.com:nik9000/mapmatcher.git")
          url.set("https://github.com/nik9000/mapmatcher")
        }
        developers {
          developer {
            id.set("nik")
            name.set("Nik Everett")
            email.set("nik9000@gmail.com")
          }
        }
      }
    }
  }
}

signing {
  useGpgCmd()
  sign(publishing.publications.get("maven"))
}

tasks.withType<Sign> {
  onlyIf { isReleaseVersion }
}
