package com.haxtech.haxtracker.wear

import androidx.compose.ui.graphics.Color
import com.haxtech.haxtracker.wear.presentation.theme.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Regression and compliance tests for Google Play Wear App Quality Guideline:
 *
 *   "Wear App Quality Guidelines: Background not black
 *    Your app does not use a black background for all apps and tiles.
 *    For more information, see Color (https://developer.android.com/training/wearables/design/color)."
 *
 * Wear OS apps and tiles must use a solid, pure black (#000000 / Color.Black) background to:
 *   1. Save battery power on AMOLED/OLED displays (OLED pixels completely power down on #000000).
 *   2. Blend seamlessly with circular and square watch bezels without visible borders or cutoffs.
 *
 * These tests run as plain JVM unit tests on every `./gradlew test` and in CI to guarantee that
 * neither current code nor future changes can reintroduce non-black backgrounds.
 */
class WearBlackBackgroundComplianceTest {

    // ── Helper functions ───────────────────────────────────────────────────

    private fun wearFile(relativePath: String): File {
        val candidates = listOf(
            File(System.getProperty("user.dir"), relativePath),
            File(System.getProperty("user.dir"), "wear/$relativePath"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw AssertionError(
                "Could not locate $relativePath (tried ${candidates.map { it.absolutePath }})"
            )
    }

    private fun loadThemeSource(): String =
        wearFile("src/main/java/com/haxtech/haxtracker/wear/presentation/theme/Theme.kt").readText(Charsets.UTF_8)

    private fun loadMainActivitySource(): String =
        wearFile("src/main/java/com/haxtech/haxtracker/wear/presentation/MainActivity.kt").readText(Charsets.UTF_8)

    private fun loadStylesSource(): String =
        wearFile("src/main/res/values/styles.xml").readText(Charsets.UTF_8)

    private fun loadManifestXml() =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(wearFile("src/main/AndroidManifest.xml"))

    // ── 1. Color Constants & Theme ColorScheme ─────────────────────────────

    @Test
    fun theme_pitchBlackConstant_isStrictlyPureOledBlack() {
        // Evaluate the compiled Kotlin symbol directly
        assertEquals(
            "PitchBlack must evaluate to pure black Color(0xFF000000)",
            Color(0xFF000000),
            PitchBlack
        )
        assertEquals(
            "PitchBlack must match Compose Color.Black",
            Color.Black,
            PitchBlack
        )
        assertEquals("Red channel must be 0", 0f, PitchBlack.red, 0.0001f)
        assertEquals("Green channel must be 0", 0f, PitchBlack.green, 0.0001f)
        assertEquals("Blue channel must be 0", 0f, PitchBlack.blue, 0.0001f)
        assertEquals("Alpha channel must be 1.0 (opaque)", 1f, PitchBlack.alpha, 0.0001f)

        // Regression guard: must NOT equal previous violating colors
        assertNotEquals(
            "PitchBlack must NOT equal the rejected blue-gray tint #090A0F",
            Color(0xFF090A0F),
            PitchBlack
        )
        assertNotEquals(
            "PitchBlack must NOT equal mobile handheld background #07090E",
            Color(0xFF07090E),
            PitchBlack
        )
    }

    @Test
    fun theme_sourceCode_definesPitchBlackAsHexPureBlack() {
        val themeSource = loadThemeSource()

        assertTrue(
            "Theme.kt must explicitly define PitchBlack = Color(0xFF000000) or Color.Black",
            themeSource.contains("PitchBlack = Color(0xFF000000)") ||
                themeSource.contains("PitchBlack = Color.Black")
        )
        assertFalse(
            "Theme.kt must NOT contain the rejected #090A0F color",
            themeSource.contains("0xFF090A0F")
        )
    }

    @Test
    fun theme_colorScheme_usesPitchBlackForBackground() {
        val themeSource = loadThemeSource()

        assertTrue(
            "ColorScheme must use PitchBlack for background",
            themeSource.contains("background = PitchBlack")
        )
        assertTrue(
            "ColorScheme must use PitchBlack for onPrimary (high-contrast text on volt green)",
            themeSource.contains("onPrimary = PitchBlack")
        )
        assertTrue(
            "ColorScheme must use PitchBlack for onSecondary",
            themeSource.contains("onSecondary = PitchBlack")
        )
        assertTrue(
            "ColorScheme must use PitchBlack for onError",
            themeSource.contains("onError = PitchBlack")
        )
    }

    @Test
    fun theme_accentColors_haveHighLuminanceAgainstPitchBlack() {
        // WCAG AAA requires high contrast against pure black (#000000)
        fun relativeLuminance(c: Color): Float =
            0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue

        val voltGreenLuminance = relativeLuminance(VoltGreen)
        val voltCyanLuminance = relativeLuminance(VoltCyan)
        val voltGoldLuminance = relativeLuminance(VoltGold)
        val textPrimaryLuminance = relativeLuminance(TextPrimary)

        assertTrue("VoltGreen must have luminance > 0.5 against black", voltGreenLuminance > 0.5f)
        assertTrue("VoltCyan must have luminance > 0.5 against black", voltCyanLuminance > 0.5f)
        assertTrue("VoltGold must have luminance > 0.5 against black", voltGoldLuminance > 0.5f)
        assertTrue("TextPrimary must have luminance > 0.7 against black", textPrimaryLuminance > 0.7f)
    }

    // ── 2. Window Themes & styles.xml ──────────────────────────────────────

    @Test
    fun styles_appTheme_windowBackgroundIsExplicitlyBlack() {
        val styles = loadStylesSource()

        assertTrue(
            "styles.xml must define AppTheme extending Theme.DeviceDefault",
            styles.contains("<style name=\"AppTheme\" parent=\"@android:style/Theme.DeviceDefault\">")
        )
        assertTrue(
            "AppTheme must explicitly declare android:windowBackground as @android:color/black",
            styles.contains("<item name=\"android:windowBackground\">@android:color/black</item>")
        )
        assertTrue(
            "AppTheme must explicitly declare android:colorBackground as @android:color/black",
            styles.contains("<item name=\"android:colorBackground\">@android:color/black</item>")
        )
    }

    @Test
    fun styles_splashTheme_setsBlackBackgroundAndTransitionsToAppTheme() {
        val styles = loadStylesSource()

        assertTrue(
            "Splash theme must set windowSplashScreenBackground to @android:color/black",
            styles.contains("<item name=\"windowSplashScreenBackground\">@android:color/black</item>")
        )
        assertTrue(
            "Splash theme postSplashScreenTheme must transition to @style/AppTheme (with black windowBackground)",
            styles.contains("<item name=\"postSplashScreenTheme\">@style/AppTheme</item>")
        )
    }

    // ── 3. AndroidManifest.xml Verification ────────────────────────────────

    @Test
    fun manifest_applicationTag_usesAppTheme() {
        val doc = loadManifestXml()
        val app = doc.getElementsByTagName("application").item(0)
        val theme = app.attributes.getNamedItemNS(
            "http://schemas.android.com/apk/res/android", "theme"
        )?.nodeValue

        assertNotNull("The <application> tag must declare an android:theme", theme)
        assertEquals(
            "<application> theme must be @style/AppTheme to enforce black windowBackground",
            "@style/AppTheme",
            theme
        )
    }

    @Test
    fun manifest_mainActivity_usesSplashStartingTheme() {
        val doc = loadManifestXml()
        val activities = doc.getElementsByTagName("activity")

        var mainActivityFound = false
        for (i in 0 until activities.length) {
            val el = activities.item(i)
            val name = el.attributes.getNamedItemNS(
                "http://schemas.android.com/apk/res/android", "name"
            )?.nodeValue ?: continue

            if (name.contains("MainActivity")) {
                mainActivityFound = true
                val theme = el.attributes.getNamedItemNS(
                    "http://schemas.android.com/apk/res/android", "theme"
                )?.nodeValue

                assertEquals(
                    "MainActivity must use @style/MainActivityTheme.Starting which transitions to @style/AppTheme",
                    "@style/MainActivityTheme.Starting",
                    theme
                )
            }
        }
        assertTrue("MainActivity must be declared in AndroidManifest.xml", mainActivityFound)
    }

    // ── 4. Composable Screens and Root Containers in MainActivity.kt ───────

    @Test
    fun mainActivity_mainMatchScreen_rootBoxUsesPitchBlackBeforePadding() {
        val source = loadMainActivitySource()
        val wearAppBody = source.substringAfter("fun WearApp()").substringBefore("fun HeaderSection")

        // In the main match view, Box must fillMaxSize and set background(PitchBlack)
        // BEFORE applying contentPadding, so that no outer screen area is left unpainted
        assertTrue(
            "Main screen root Box must set .fillMaxSize().background(PitchBlack)",
            wearAppBody.contains(".fillMaxSize()") && wearAppBody.contains(".background(PitchBlack)")
        )

        val elseBranch = wearAppBody.substringAfter("else -> {").substringBefore("HeaderSection")
        val backgroundIndex = elseBranch.indexOf(".background(PitchBlack)")
        val paddingIndex = elseBranch.indexOf(".padding(contentPadding)")

        assertTrue(
            "Root Box must apply .background(PitchBlack) BEFORE .padding(contentPadding)",
            backgroundIndex != -1 && paddingIndex != -1 && backgroundIndex < paddingIndex
        )
    }

    @Test
    fun mainActivity_splashScreen_usesPitchBlackBackground() {
        val source = loadMainActivitySource()
        val splashSection = source.substringAfter("fun SplashScreen(").substringBefore("@Composable")

        assertTrue(
            "SplashScreen composable must use .fillMaxSize().background(PitchBlack)",
            splashSection.contains(".fillMaxSize()") && splashSection.contains(".background(PitchBlack)")
        )
    }

    @Test
    fun mainActivity_allDialogs_useSolidPitchBlackBackground() {
        val source = loadMainActivitySource()

        val dialogs = listOf(
            "SettingsDialog",
            "GestureHelpDialog",
            "GameFinishedDialog",
            "MatchFinishedDialog"
        )

        for (dialog in dialogs) {
            val dialogSection = source.substringAfter("fun $dialog(").substringBefore("@Composable\nprivate fun")
            assertTrue(
                "$dialog must use .background(PitchBlack) on its full screen container",
                dialogSection.contains(".background(PitchBlack)") && dialogSection.contains(".fillMaxSize()")
            )

            // Regression guard: ensure dialogs do NOT use translucent scrims (copy(alpha = ...))
            // at their root layer which could expose non-black backgrounds
            assertFalse(
                "$dialog must use solid PitchBlack, NOT PitchBlack.copy(alpha = ...)",
                dialogSection.contains("PitchBlack.copy(") || dialogSection.contains("Color.Black.copy(")
            )
        }
    }

    @Test
    fun mainActivity_noRootContainerUsesNonBlackSurface() {
        val source = loadMainActivitySource()

        // Full-screen containers (.fillMaxSize()) must never use DarkSurface or DarkSurfaceElevated
        assertFalse(
            "DarkSurface must not be used with .fillMaxSize() as a screen background",
            source.contains(".fillMaxSize().background(DarkSurface)") ||
                source.contains(".background(DarkSurface).fillMaxSize()")
        )
        assertFalse(
            "DarkSurfaceElevated must not be used with .fillMaxSize() as a screen background",
            source.contains(".fillMaxSize().background(DarkSurfaceElevated)") ||
                source.contains(".background(DarkSurfaceElevated).fillMaxSize()")
        )
    }

    // ── 5. Static Codebase Scanner ─────────────────────────────────────────

    @Test
    fun codebase_noForbiddenNonBlackColorsInWearKotlinFiles() {
        val wearJavaDir = wearFile("src/main/java")
        val ktFiles = wearJavaDir.walkTopDown().filter { it.extension == "kt" }.toList()

        assertTrue("Must find Kotlin files in wear/src/main/java", ktFiles.isNotEmpty())

        for (file in ktFiles) {
            val text = file.readText(Charsets.UTF_8)
            val relativePath = file.relativeTo(wearJavaDir).path

            // 0xFF090A0F was the exact hex color rejected by Google Play
            assertFalse(
                "File $relativePath contains forbidden non-black color 0xFF090A0F",
                text.contains("0xFF090A0F")
            )
            // 0xFF07090E is the mobile OLED background color, not allowed on Wear OS
            assertFalse(
                "File $relativePath contains mobile non-black color 0xFF07090E",
                text.contains("0xFF07090E")
            )
        }
    }

    // ── 6. Tiles & Complications Compliance ────────────────────────────────

    @Test
    fun tilePreview_imageExistsAndHasPureBlackBackground() {
        val previewFile = wearFile("src/main/res/drawable/tile_preview.png")
        assertTrue("tile_preview.png must exist in res/drawable", previewFile.exists())

        val image = ImageIO.read(previewFile)
        assertNotNull("tile_preview.png must be a readable image", image)

        val w = image.width
        val h = image.height
        assertTrue("tile_preview.png must have valid dimensions", w > 0 && h > 0)

        // Sample corners and borders to verify #000000 background
        val samplePoints = listOf(
            0 to 0,
            w - 1 to 0,
            0 to h - 1,
            w - 1 to h - 1,
            w / 2 to 0,
            0 to h / 2,
            w - 1 to h / 2,
            w / 2 to h - 1,
            w / 2 to h / 2
        )

        for ((x, y) in samplePoints) {
            val rgb = image.getRGB(x, y)
            val r = (rgb shr 16) and 0xFF
            val g = (rgb shr 8) and 0xFF
            val b = rgb and 0xFF

            assertEquals("Pixel at ($x, $y) in tile_preview.png must have Red = 0", 0, r)
            assertEquals("Pixel at ($x, $y) in tile_preview.png must have Green = 0", 0, g)
            assertEquals("Pixel at ($x, $y) in tile_preview.png must have Blue = 0", 0, b)
        }
    }

    @Test
    fun tileService_doesNotDefineNonBlackBackgroundColor() {
        val candidateFiles = listOf(
            File(System.getProperty("user.dir"), "src/main/java/com/haxtech/haxtracker/tile/MainTileService.kt"),
            File(System.getProperty("user.dir"), "wear/src/main/java/com/haxtech/haxtracker/tile/MainTileService.kt"),
            File(System.getProperty("user.dir"), "src/main/java/com/haxtech/haxtracker/wear/tile/MainTileService.kt"),
            File(System.getProperty("user.dir"), "wear/src/main/java/com/haxtech/haxtracker/wear/tile/MainTileService.kt")
        )
        val tileServiceFile = candidateFiles.firstOrNull { it.exists() }
        assertNotNull("MainTileService.kt must exist in the source tree", tileServiceFile)
        val text = tileServiceFile!!.readText(Charsets.UTF_8)
        // Ensure no custom background color is set that would override the default black background
        assertFalse(
            "MainTileService must not set a non-black background color",
            text.contains("setBackgroundColor") && !text.contains("0xFF000000") && !text.contains("BLACK")
        )
    }
}
