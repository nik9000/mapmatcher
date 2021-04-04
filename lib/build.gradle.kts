plugins {
  `java-library`
  id("com.github.hierynomus.license") version "0.15.0"
  checkstyle
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

tasks.javadoc {
  val o = options
  if (o !is StandardJavadocDocletOptions) {
    throw IllegalArgumentException()
  }
  o.docTitle("MapMatcher")
  o.windowTitle("MapMatcher")
  o.links!!.add("https://docs.oracle.com/en/java/javase/11/docs/api/")
  o.links!!.add("http://hamcrest.org/JavaHamcrest/javadoc/2.2/")
  o.addBooleanOption("Xdoclint:all,-missing", true)
  o.showFromPublic()
}

tasks.register<Jar>("javadocJar") {
  from(tasks.javadoc)
  classifier = "javadoc"
}

tasks.build {
  dependsOn(tasks.withType<Jar>())
}
