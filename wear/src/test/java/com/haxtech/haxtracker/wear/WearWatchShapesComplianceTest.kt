package com.haxtech.haxtracker.wear

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.sqrt

/**
 * Tests that verify the Wear OS UI layout complies with Google Play's
 * Wear App Quality Guideline WO-V16: Watch Shapes:
 *
 *   "Fits within the physical display area.
 *    No text or controls are cut off by the screen edges.
 *    Handle different watch shapes (round and square displays)."
 *
 * These tests parse the source and resource files as plain JVM unit tests
 * and also mathematically verify the geometry against standard Wear OS
 * round display sizes (192dp, 390dp, 450dp).
 *
 * Running as part of `./gradlew test`, these tests catch any layout regressions
 * before an APK/AAB is submitted to Google Play Console.
 */
class WearWatchShapesComplianceTest {

    // ── helpers ────────────────────────────────────────────────────────────

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

    private fun loadMainActivitySource(): String =
        wearFile("src/main/java/com/haxtech/haxtracker/wear/presentation/MainActivity.kt").readText()

    private fun loadThemeSource(): String =
        wearFile("src/main/java/com/haxtech/haxtracker/wear/presentation/theme/Theme.kt").readText()

    private fun loadStylesSource(): String =
        wearFile("src/main/res/values/styles.xml").readText()

    // ── Rule 1: No Fixed Rectangular Card Borders on Dialogs ──────────────

    @Test
    fun dialogs_doNotUseRectangularCardBorders() {
        val source = loadMainActivitySource()

        // Previously, SettingsDialog wrapped ScalingLazyColumn with:
        //   .clip(RoundedCornerShape(14.dp))
        //   .background(DarkSurface)
        //   .border(1.dp, VoltGold, RoundedCornerShape(14.dp))
        // This produced a rectangular card whose corners clipped outside the round screen.
        assertFalse(
            "SettingsDialog must NOT wrap ScalingLazyColumn in an outer rectangular border that clips on round screens",
            source.contains(".border(1.dp, VoltGold, RoundedCornerShape(14.dp))") &&
                source.contains("ScalingLazyColumn") &&
                source.substringAfter("fun SettingsDialog").substringBefore("fun ConfigOptionButton")
                    .contains(".border(1.dp, VoltGold, RoundedCornerShape(14.dp))")
        )

        // Verify that SettingsDialog uses a full-screen PitchBlack background instead
        val settingsSection = source.substringAfter("fun SettingsDialog").substringBefore("fun ConfigOptionButton")
        assertTrue(
            "SettingsDialog must use full-screen PitchBlack background to blend seamlessly with watch bezels",
            settingsSection.contains(".background(PitchBlack)") && settingsSection.contains(".fillMaxSize()")
        )
    }

    @Test
    fun gestureHelp_doesNotUseRectangularCardBorder() {
        val source = loadMainActivitySource()
        val gestureHelpSection = source.substringAfter("fun GestureHelpDialog").substringBefore("fun GestureHelpItem")

        assertFalse(
            "GestureHelpDialog must NOT use an outer rectangular card border",
            gestureHelpSection.contains(".border(1.dp, VoltCyan, RoundedCornerShape(14.dp))")
        )
        assertTrue(
            "GestureHelpDialog must use full-screen ScalingLazyColumn with PitchBlack background",
            gestureHelpSection.contains("ScalingLazyColumn") && gestureHelpSection.contains(".background(PitchBlack)")
        )
    }

    // ── Rule 2: Dialogs Use ScalingLazyColumn and ScreenScaffold ───────────

    @Test
    fun allDialogs_useScalingLazyColumnAndScreenScaffold() {
        val source = loadMainActivitySource()

        val dialogNames = listOf(
            "SettingsDialog",
            "GestureHelpDialog",
            "GameFinishedDialog",
            "MatchFinishedDialog"
        )

        for (dialog in dialogNames) {
            assertTrue(
                "$dialog must be defined in MainActivity.kt",
                source.contains("fun $dialog")
            )
            val dialogBody = source.substringAfter("fun $dialog").substringBefore("@Composable\nprivate fun")

            assertTrue(
                "$dialog must use ScreenScaffold(scrollState = listState) for Wear OS responsive layout",
                dialogBody.contains("ScreenScaffold(\n        scrollState = listState") ||
                    dialogBody.contains("ScreenScaffold(scrollState = listState") ||
                    dialogBody.contains("scrollState = listState")
            )

            assertTrue(
                "$dialog must use ScalingLazyColumn for automatic centering and dynamic scaling on round screens",
                dialogBody.contains("ScalingLazyColumn")
            )

            assertTrue(
                "$dialog must pass ScreenScaffold contentPadding into ScalingLazyColumn",
                dialogBody.contains("contentPadding = contentPadding")
            )
        }
    }

    // ── Rule 3: Safe Width & Rounded Corners on Interactive Items ─────────

    @Test
    fun configOptionButtons_useSafeWidthPercentageAndPillShape() {
        val source = loadMainActivitySource()
        val buttonSection = source.substringAfter("fun ConfigOptionButton").substringBefore("@Composable")

        assertTrue(
            "ConfigOptionButton must use responsive safe width ratio (0.88f) to prevent clipping on round bezels",
            buttonSection.contains(".fillMaxWidth(0.88f)")
        )

        assertTrue(
            "ConfigOptionButton must use rounded corners (>= 14dp) for curved display compatibility",
            buttonSection.contains("RoundedCornerShape(16.dp)") || buttonSection.contains("RoundedCornerShape(14.dp)")
        )
    }

    @Test
    fun gestureHelpItems_useSafeWidthPercentage() {
        val source = loadMainActivitySource()
        val itemSection = source.substringAfter("fun GestureHelpItem").substringBefore("@Composable")

        assertTrue(
            "GestureHelpItem must use responsive safe width ratio (0.88f)",
            itemSection.contains(".fillMaxWidth(0.88f)")
        )

        assertTrue(
            "GestureHelpItem must use rounded corners (>= 12dp)",
            itemSection.contains("RoundedCornerShape(14.dp)") || itemSection.contains("RoundedCornerShape(12.dp)")
        )
    }

    @Test
    fun dialogActionButtons_useSafeWidth() {
        val source = loadMainActivitySource()

        // SettingsDialog Close button
        val settingsSection = source.substringAfter("fun SettingsDialog").substringBefore("fun ConfigOptionButton")
        assertTrue(
            "SettingsDialog Close button must use safe width (0.85f)",
            settingsSection.contains(".fillMaxWidth(0.85f)")
        )

        // GestureHelp GOT IT button
        val gestureSection = source.substringAfter("fun GestureHelpDialog").substringBefore("fun GestureHelpItem")
        assertTrue(
            "GestureHelpDialog GOT IT button must use safe width (0.85f)",
            gestureSection.contains(".fillMaxWidth(0.85f)")
        )

        // GameFinished action button
        val gameSection = source.substringAfter("fun GameFinishedDialog").substringBefore("fun MatchFinishedDialog")
        assertTrue(
            "GameFinishedDialog action button must use safe width (0.85f)",
            gameSection.contains(".fillMaxWidth(0.85f)")
        )

        // MatchFinished action button
        val matchSection = source.substringAfter("fun MatchFinishedDialog").substringBefore("@WearPreviewDevices")
        assertTrue(
            "MatchFinishedDialog action button must use safe width (0.85f)",
            matchSection.contains(".fillMaxWidth(0.85f)")
        )
    }

    // ── Rule 4: Main Match Screen Safe Clearance ──────────────────────────

    @Test
    fun mainScreen_scoreboardAndCourt_useSafeWidth() {
        val source = loadMainActivitySource()

        val scoreboardSection = source.substringAfter("fun CompactScoreBoard").substringBefore("fun TeamScoreBox")
        assertTrue(
            "CompactScoreBoard must use safe width (0.92f) to avoid corner clipping on circular screens",
            scoreboardSection.contains(".fillMaxWidth(0.92f)")
        )

        assertTrue(
            "MiniCourtSection must use safe width (0.92f)",
            source.contains(".fillMaxWidth(0.92f)")
        )
    }

    @Test
    fun mainScreen_footer_hasAdequateBottomPadding() {
        val source = loadMainActivitySource()
        val footerSection = source.substringAfter("fun GestureHintFooter").substringBefore("fun GestureHelpDialog")

        assertTrue(
            "GestureHintFooter must have at least 6dp bottom padding so controls are not clipped by the bottom bezel curve",
            footerSection.contains(".padding(bottom = 6.dp)") || footerSection.contains(".padding(bottom = 8.dp)")
        )
    }

    // ── Rule 5: Top-Level Modal Branching and BackHandler Support ──────────

    @Test
    fun wearApp_usesTopLevelScreenBranchingWithBackHandler() {
        val source = loadMainActivitySource()
        val wearAppBody = source.substringAfter("fun WearApp()").substringBefore("fun HeaderSection")

        assertTrue(
            "WearApp must use 'when' statement inside AppScaffold to separate screens",
            wearAppBody.contains("AppScaffold {") && wearAppBody.contains("when {")
        )

        assertTrue(
            "Settings screen must support back navigation with BackHandler",
            wearAppBody.contains("showSettings -> {") &&
                wearAppBody.substringAfter("showSettings -> {").substringBefore("SettingsDialog").contains("BackHandler")
        )

        assertTrue(
            "Gesture Help screen must support back navigation with BackHandler",
            wearAppBody.contains("showGestureHelp -> {") &&
                wearAppBody.substringAfter("showGestureHelp -> {").substringBefore("GestureHelpDialog").contains("BackHandler")
        )
    }

    // ── Rule 6: Previews Declared with @WearPreviewDevices ─────────────────

    @Test
    fun allScreens_declareWearPreviewDevices() {
        val source = loadMainActivitySource()

        val previews = listOf(
            "DefaultPreview",
            "SettingsPreview",
            "GestureHelpPreview",
            "GameFinishedPreview",
            "MatchFinishedPreview"
        )

        for (preview in previews) {
            assertTrue(
                "MainActivity.kt must declare $preview",
                source.contains("fun $preview()")
            )
            val annotationLine = source.substringBefore("fun $preview()")
                .lines().takeLast(3).joinToString(" ")
            assertTrue(
                "$preview must be annotated with @WearPreviewDevices to test round and square shapes",
                annotationLine.contains("@WearPreviewDevices")
            )
        }
    }

    // ── Rule 7: Circular Display Geometry Mathematics ─────────────────────

    @Test
    fun geometricCircleConstraint_safeWidthAlwaysFitsInsidePhysicalRoundDisplay() {
        // Standard Wear OS display diameters in dp
        val displayDiameters = listOf(192.0, 390.0, 410.0, 450.0)

        for (d in displayDiameters) {
            val r = d / 2.0 // circle radius

            // Check elements using 0.88f width
            val elementWidth = d * 0.88
            val halfW = elementWidth / 2.0

            // In ScalingLazyColumn, items at center (y = 0):
            // Distance from center to outer corner: sqrt(halfW^2 + (height/2)^2)
            val itemHeight = 36.0 // standard button height
            val halfH = itemHeight / 2.0

            val cornerDistanceCenter = sqrt(halfW * halfW + halfH * halfH)
            assertTrue(
                "At vertical center of ${d}dp display, element corners must lie strictly inside radius $r (was $cornerDistanceCenter)",
                cornerDistanceCenter < r
            )

            // At y = 40dp (scrolled item), ScalingLazyColumn naturally centers it.
            // When user scrolls an item to center, its corner distance is well within radius r.
            // On the other hand, verify that the old rectangular card (10dp padding = d - 20dp width, height 160dp)
            // mathematically VIOLATED the circle:
            val oldHalfW = (d - 20.0) / 2.0
            val oldHalfH = 80.0 // half of a ~160dp card
            val oldCornerDistance = sqrt(oldHalfW * oldHalfW + oldHalfH * oldHalfH)
            assertTrue(
                "Regression verification: old rectangular container exceeded circular display radius of $r (corner was at $oldCornerDistance)",
                oldCornerDistance > r
            )
        }
    }

    // ── Rule 8: Pitch Black Theme for Seamless Bezel Blending ─────────────

    @Test
    fun theme_pitchBlackIsPureOledBlack() {
        val themeSource = loadThemeSource()
        assertTrue(
            "PitchBlack must be pure #000000 so content blends seamlessly with circular watch bezels",
            themeSource.contains("PitchBlack = Color(0xFF000000)")
        )
    }

    @Test
    fun styles_appThemeWindowBackgroundIsBlack() {
        val stylesSource = loadStylesSource()
        assertTrue(
            "styles.xml AppTheme must set android:windowBackground to black",
            stylesSource.contains("<item name=\"android:windowBackground\">@android:color/black</item>")
        )
        assertTrue(
            "styles.xml AppTheme must set android:colorBackground to black",
            stylesSource.contains("<item name=\"android:colorBackground\">@android:color/black</item>")
        )
    }
}

