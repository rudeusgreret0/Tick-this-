import java.io.File
import java.net.URI

pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Tic Tac Toe"

include(":app")

// Ensure bundled Google Fonts (.ttf) exist when building in clean CI/GitHub Actions checkouts
val fontDir = File(rootDir, "app/src/main/res/font")
if (!File(fontDir, "space_grotesk.ttf").exists() ||
    !File(fontDir, "plus_jakarta_sans.ttf").exists() ||
    !File(fontDir, "jetbrains_mono.ttf").exists()
) {
  fontDir.mkdirs()
  val requiredFonts = mapOf(
    "space_grotesk.ttf" to "https://fonts.gstatic.com/s/spacegrotesk/v22/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-gOoraIAEj7oUXskPMU.ttf",
    "plus_jakarta_sans.ttf" to "https://fonts.gstatic.com/s/plusjakartasans/v12/LDIbaomQNQcsA88c7O9yZ4KMCoOg4IA6-91aHEjcWuA_qU79TR_Q.ttf",
    "jetbrains_mono.ttf" to "https://fonts.gstatic.com/s/jetbrainsmono/v24/tDbY2o-flEEny0FZhsfKu5WU4zr3E_BX0PnT8RD8yKxTOlOQ.ttf"
  )
  requiredFonts.forEach { (fileName, url) ->
    val targetFile = File(fontDir, fileName)
    if (!targetFile.exists() || targetFile.length() == 0L) {
      runCatching {
        URI(url).toURL().openStream().use { input ->
          targetFile.outputStream().use { output -> input.copyTo(output) }
        }
      }
    }
  }
}

// Prevent duplicate launcher resource conflicts only when custom .png icons are present alongside legacy .webp
listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi").forEach { density ->
  val mipmapDir = File(rootDir, "app/src/main/res/mipmap-$density")
  if (File(mipmapDir, "ic_launcher.png").exists() && File(mipmapDir, "ic_launcher.webp").exists()) {
    File(mipmapDir, "ic_launcher.webp").delete()
  }
  if (File(mipmapDir, "ic_launcher_round.png").exists() && File(mipmapDir, "ic_launcher_round.webp").exists()) {
    File(mipmapDir, "ic_launcher_round.webp").delete()
  }
}
