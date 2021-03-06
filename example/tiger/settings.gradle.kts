rootProject.name = "spoofax.example.tiger"

pluginManagement {
  repositories {
    // Get plugins from artifacts.metaborg.org, first.
    maven("https://artifacts.metaborg.org/content/repositories/releases/")
    maven("https://artifacts.metaborg.org/content/repositories/snapshots/")
    // Required by several Gradle plugins (Maven central, JCenter).
    maven("https://artifacts.metaborg.org/content/repositories/central/") // Maven central mirror.
    mavenCentral() // Maven central as backup.
    jcenter()
    // Get plugins from Gradle plugin portal.
    gradlePluginPortal()
  }
}

include("tiger")
include("tiger.spoofax")
include("tiger.cli")
include("tiger.intellij")
include("tiger.eclipse")
include("tiger.eclipse.externaldeps")
