plugins {
  `java-library`
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
