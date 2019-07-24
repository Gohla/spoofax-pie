plugins {
  id("org.metaborg.gradle.config.java-library")
}

dependencies {
  api(platform(project(":depconstraints")))
  annotationProcessor(platform(project(":depconstraints")))

  api(project(":common"))
  api(project(":jsglr.common"))
  api("org.metaborg:org.spoofax.jsglr")

  compileOnly("org.derive4j:derive4j-annotation")
  compileOnly("org.checkerframework:checker-qual-android")

  annotationProcessor("org.derive4j:derive4j")
}
