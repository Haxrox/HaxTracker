package com.haxtech.haxtracker.wear

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Tests that verify the Wear OS splash screen configuration complies with
 * Google Play's Wear App Quality Guideline WO-V15:
 *
 *   "Your app must show a 48x48dp app icon on a black background during app startup."
 *
 * These tests parse the raw resource XML files in the source tree so they run as
 * plain JVM unit tests (no emulator required) and will catch regressions before
 * the app is ever submitted to the Play Store.
 */
class WearSplashScreenComplianceTest {

    // ── helpers ────────────────────────────────────────────────────────────

    /** Resolves a path relative to the wear module root. */
    private fun wearFile(relativePath: String): File {
        // Gradle sets user.dir to the project root for unit tests.
        val candidates = listOf(
            File(System.getProperty("user.dir"), relativePath),              // run from wear/
            File(System.getProperty("user.dir"), "wear/$relativePath"),      // run from project root
        )
        return candidates.firstOrNull { it.exists() }
            ?: fail("Could not locate $relativePath (tried ${candidates.map { it.absolutePath }})")
    }

    private fun parseXml(file: File) =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

    private fun File.readText(): String = readText(Charsets.UTF_8)

    // ── AndroidManifest.xml ───────────────────────────────────────────────

    @Test
    fun manifest_launcherActivity_usesSplashTheme() {
        val doc = parseXml(wearFile("src/main/AndroidManifest.xml"))
        val activities = doc.getElementsByTagName("activity")

        var found = false
        for (i in 0 until activities.length) {
            val el = activities.item(i)
            val name = el.attributes.getNamedItemNS(
                "http://schemas.android.com/apk/res/android", "name"
            )?.nodeValue ?: continue

            // Check for the launcher activity
            if (name.contains("MainActivity")) {
                val theme = el.attributes.getNamedItemNS(
                    "http://schemas.android.com/apk/res/android", "theme"
                )?.nodeValue

                assertNotNull(
                    "MainActivity must have an android:theme attribute", theme
                )
                assertTrue(
                    "MainActivity theme ($theme) must reference the splash starting theme " +
                        "(expected it to contain 'Starting' — e.g. @style/MainActivityTheme.Starting)",
                    theme!!.contains("Starting")
                )
                found = true
            }
        }
        assertTrue("AndroidManifest must contain a MainActivity", found)
    }

    @Test
    fun manifest_application_declaresRoundIcon() {
        val doc = parseXml(wearFile("src/main/AndroidManifest.xml"))
        val app = doc.getElementsByTagName("application").item(0)

        val roundIcon = app.attributes.getNamedItemNS(
            "http://schemas.android.com/apk/res/android", "roundIcon"
        )?.nodeValue

        assertNotNull(
            "The <application> tag must declare android:roundIcon for Wear OS", roundIcon
        )
        assertTrue(
            "android:roundIcon ($roundIcon) should reference ic_launcher_round",
            roundIcon!!.contains("ic_launcher_round")
        )
    }

    // ── styles.xml — splash theme ─────────────────────────────────────────

    @Test
    fun splashTheme_parentIsThemeSplashScreen() {
        val file = wearFile("src/main/res/values/styles.xml")
        val doc = parseXml(file)
        val styles = doc.getElementsByTagName("style")

        var foundSplashStyle = false
        for (i in 0 until styles.length) {
            val el = styles.item(i)
            val name = el.attributes.getNamedItem("name")?.nodeValue ?: continue
            if (name.contains("Starting")) {
                val parent = el.attributes.getNamedItem("parent")?.nodeValue
                assertEquals(
                    "Splash starting theme must extend Theme.SplashScreen",
                    "Theme.SplashScreen", parent
                )
                foundSplashStyle = true
            }
        }
        assertTrue(
            "styles.xml must contain a splash starting theme (name containing 'Starting')",
            foundSplashStyle
        )
    }

    @Test
    fun splashTheme_backgroundIsBlack() {
        val content = wearFile("src/main/res/values/styles.xml").readText()

        assertTrue(
            "Splash theme must set windowSplashScreenBackground to @android:color/black",
            content.contains("windowSplashScreenBackground") &&
                content.contains("@android:color/black")
        )
    }

    @Test
    fun splashTheme_hasAnimatedIcon() {
        val content = wearFile("src/main/res/values/styles.xml").readText()

        assertTrue(
            "Splash theme must set windowSplashScreenAnimatedIcon to a drawable",
            content.contains("windowSplashScreenAnimatedIcon")
        )
    }

    @Test
    fun splashTheme_hasPostSplashTheme() {
        val content = wearFile("src/main/res/values/styles.xml").readText()

        assertTrue(
            "Splash theme must set postSplashScreenTheme so the app transitions properly",
            content.contains("postSplashScreenTheme")
        )
    }

    // ── splash_icon.xml — the 48dp icon drawable ──────────────────────────

    @Test
    fun splashIcon_exists() {
        val file = wearFile("src/main/res/drawable/splash_icon.xml")
        assertTrue("splash_icon.xml must exist in drawable/", file.exists())
    }

    @Test
    fun splashIcon_size48dp() {
        val content = wearFile("src/main/res/drawable/splash_icon.xml").readText()

        assertTrue(
            "splash_icon.xml must specify android:width=\"48dp\"",
            content.contains("android:width=\"48dp\"")
        )
        assertTrue(
            "splash_icon.xml must specify android:height=\"48dp\"",
            content.contains("android:height=\"48dp\"")
        )
    }

    @Test
    fun splashIcon_isCenteredViaGravity() {
        val content = wearFile("src/main/res/drawable/splash_icon.xml").readText()

        assertTrue(
            "splash_icon.xml must center the icon with android:gravity=\"center\"",
            content.contains("android:gravity=\"center\"")
        )
    }

    @Test
    fun splashIcon_referencesLauncherIcon() {
        val content = wearFile("src/main/res/drawable/splash_icon.xml").readText()

        assertTrue(
            "splash_icon.xml must reference the app launcher icon " +
                "(expected @mipmap/ic_launcher or @mipmap/ic_launcher_round)",
            content.contains("@mipmap/ic_launcher")
        )
    }

    // ── Launcher icon assets exist at all density buckets ─────────────────

    @Test
    fun launcherIcon_existsAtAllDensities() {
        val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
        for (density in densities) {
            val file = wearFile("src/main/res/mipmap-$density/ic_launcher.webp")
            assertTrue(
                "ic_launcher.webp must exist at mipmap-$density",
                file.exists()
            )
        }
    }

    @Test
    fun launcherRoundIcon_existsAtAllDensities() {
        val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
        for (density in densities) {
            val file = wearFile("src/main/res/mipmap-$density/ic_launcher_round.webp")
            assertTrue(
                "ic_launcher_round.webp must exist at mipmap-$density",
                file.exists()
            )
        }
    }

    @Test
    fun launcherForeground_existsAtAllDensities() {
        val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
        for (density in densities) {
            val file = wearFile("src/main/res/mipmap-$density/ic_launcher_foreground.webp")
            assertTrue(
                "ic_launcher_foreground.webp must exist at mipmap-$density",
                file.exists()
            )
        }
    }

    @Test
    fun adaptiveIcon_xml_exists() {
        val file = wearFile("src/main/res/mipmap-anydpi-v26/ic_launcher.xml")
        assertTrue("Adaptive icon XML must exist at mipmap-anydpi-v26/", file.exists())

        val roundFile = wearFile("src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml")
        assertTrue("Adaptive round icon XML must exist at mipmap-anydpi-v26/", roundFile.exists())
    }

    // ── MainActivity.kt — installSplashScreen() call ──────────────────────

    @Test
    fun mainActivity_callsInstallSplashScreen() {
        val file = wearFile(
            "src/main/java/com/haxtech/haxtracker/wear/presentation/MainActivity.kt"
        )
        val content = file.readText()

        assertTrue(
            "MainActivity must call installSplashScreen() before super.onCreate()",
            content.contains("installSplashScreen()")
        )
    }

    @Test
    fun mainActivity_hasSplashScreenComposable() {
        val file = wearFile(
            "src/main/java/com/haxtech/haxtracker/wear/presentation/MainActivity.kt"
        )
        val content = file.readText()

        assertTrue(
            "MainActivity.kt must define or reference a SplashScreen composable",
            content.contains("fun SplashScreen(")
        )
    }

    @Test
    fun mainActivity_splashScreenUsesPitchBlackBackground() {
        val file = wearFile(
            "src/main/java/com/haxtech/haxtracker/wear/presentation/MainActivity.kt"
        )
        val content = file.readText()

        // The SplashScreen composable should use PitchBlack (which is Color(0xFF000000))
        assertTrue(
            "SplashScreen composable must use PitchBlack (pure black) background",
            content.contains(".background(PitchBlack)")
        )
    }

    @Test
    fun mainActivity_splashScreenIcon48dp() {
        val file = wearFile(
            "src/main/java/com/haxtech/haxtracker/wear/presentation/MainActivity.kt"
        )
        val content = file.readText()

        assertTrue(
            "SplashScreen composable icon must be sized to 48.dp",
            content.contains(".size(48.dp)")
        )
    }

    @Test
    fun theme_pitchBlackIsPureBlack() {
        val file = wearFile(
            "src/main/java/com/haxtech/haxtracker/wear/presentation/theme/Theme.kt"
        )
        val content = file.readText()

        assertTrue(
            "PitchBlack must be pure black (0xFF000000) per Wear OS guidelines",
            content.contains("0xFF000000")
        )
    }
}

