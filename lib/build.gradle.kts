plugins {
  `java-library`
  id("com.github.hierynomus.license") version "0.15.0"
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

repositories {
  jcenter()
}

dependencies {
  implementation("org.hamcrest:hamcrest:2.2")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
  testImplementation("com.google.code.gson:gson:2.8.6")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.test {
  useJUnitPlatform()
}

license {
  header = rootProject.file("LICENSE_HEADER")
  exclude("*.json")
}

tasks.register<Jar>("sourcesJar") {
  from(sourceSets.main.get().getAllJava())
  classifier = "sources"
}

tasks.register<Jar>("javadocJar") {
  from(tasks.javadoc)
  classifier = "javadoc"
}

tasks.build {
  dependsOn(tasks.withType<Jar>())
}
