package com.haxtech.haxtracker

import com.haxtech.haxtracker.core.engine.GameEngine
import com.haxtech.haxtracker.core.model.GameAction
import com.haxtech.haxtracker.core.model.MatchConfig
import com.haxtech.haxtracker.sync.PhoneSyncRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Automated compliance tests for Google Play Developer Policies (Mobile module):
 * - Target API Level & Application ID
 * - Permissions Minimalism & Data Safety (no dangerous/network permissions)
 * - Android 12+ Exported Component Security
 * - No Ads / Tracking / Billing Dark Patterns
 * - Accessibility (TalkBack contentDescription on IconButtons)
 * - Battery & Performance (Wearable sync throttling & non-urgent DataClient)
 */
class GooglePlayPolicyComplianceTest {

    private val androidNs = "http://schemas.android.com/apk/res/android"

    private fun mobileFile(relativePath: String): File {
        val candidates = listOf(
            File(System.getProperty("user.dir"), relativePath),
            File(System.getProperty("user.dir"), "mobile/$relativePath")
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw AssertionError("Could not locate $relativePath")
    }

    private fun parseManifest() =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(mobileFile("src/main/AndroidManifest.xml"))

    @Before
    fun setUp() {
        PhoneSyncRepository.resetThrottleForTest()
    }

    @After
    fun tearDown() {
        PhoneSyncRepository.resetThrottleForTest()
    }

    @Test
    fun buildGradle_meetsGooglePlayTargetApiAndAppId() {
        val gradle = mobileFile("build.gradle.kts").readText()
        val targetSdk = Regex("""targetSdk\s*=\s*(\d+)""").find(gradle)?.groupValues?.get(1)?.toIntOrNull()
        val compileSdk = Regex("""compileSdk\s*=\s*(\d+)""").find(gradle)?.groupValues?.get(1)?.toIntOrNull()

        assertNotNull("targetSdk must be declared", targetSdk)
        assertNotNull("compileSdk must be declared", compileSdk)
        assertTrue("targetSdk ($targetSdk) must be >= 35 for Google Play compliance", targetSdk!! >= 35)
        assertTrue("compileSdk ($compileSdk) must be >= 35", compileSdk!! >= 35)
        assertTrue(
            "applicationId must be com.haxtech.haxtracker",
            gradle.contains("""applicationId = "com.haxtech.haxtracker"""")
        )
    }

    @Test
    fun manifest_usesMinimalPermissions_andNoDangerousOrNetworkPermissions() {
        val doc = parseManifest()
        val permNodes = doc.getElementsByTagName("uses-permission")
        val declared = mutableListOf<String>()
        for (i in 0 until permNodes.length) {
            val name = permNodes.item(i).attributes.getNamedItemNS(androidNs, "name")?.nodeValue
            if (name != null) declared.add(name)
        }

        val forbidden = listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "com.google.android.gms.permission.AD_ID"
        )
        for (perm in forbidden) {
            assertFalse("Mobile manifest must not declare unnecessary permission: $perm", declared.contains(perm))
        }
        assertEquals(listOf("android.permission.VIBRATE"), declared)
    }

    @Test
    fun manifest_doesNotAllowCleartextTraffic_andDeclaresExportedOnAllFilteredComponents() {
        val doc = parseManifest()
        val app = doc.getElementsByTagName("application").item(0)
        val cleartext = app.attributes.getNamedItemNS(androidNs, "usesCleartextTraffic")?.nodeValue
        assertFalse("usesCleartextTraffic must not be true", cleartext == "true")

        for (tag in listOf("activity", "service", "receiver")) {
            val nodes = doc.getElementsByTagName(tag)
            for (i in 0 until nodes.length) {
                val el = nodes.item(i)
                val children = el.childNodes
                var hasIntentFilter = false
                for (j in 0 until children.length) {
                    if (children.item(j).nodeName == "intent-filter") {
                        hasIntentFilter = true
                        break
                    }
                }
                if (hasIntentFilter) {
                    val exported = el.attributes.getNamedItemNS(androidNs, "exported")?.nodeValue
                    val name = el.attributes.getNamedItemNS(androidNs, "name")?.nodeValue
                    assertNotNull("$tag '$name' with intent-filter must explicitly set android:exported", exported)
                }
            }
        }
    }

    @Test
    fun buildGradle_containsNoAdsOrTrackingSdks() {
        val gradle = mobileFile("build.gradle.kts").readText().lowercase()
        val forbiddenDeps = listOf("play-services-ads", "firebase-analytics", "facebook-android-sdk", "billing")
        for (dep in forbiddenDeps) {
            assertFalse("Mobile module must not include undeclared ad/tracking/billing SDK: $dep", gradle.contains(dep))
        }
    }

    @Test
    fun matchScreen_allIconButtonsDeclareContentDescription() {
        val source = mobileFile("src/main/java/com/haxtech/haxtracker/ui/match/MatchScreen.kt").readText()
        val iconButtonCount = Regex("""IconButton\s*\(""").findAll(source).count()
        val contentDescCount = Regex("""contentDescription\s*=""").findAll(source).count()

        assertTrue("Expected IconButtons in MatchScreen", iconButtonCount >= 7)
        assertTrue(
            "Every IconButton ($iconButtonCount) must have a contentDescription ($contentDescCount found)",
            contentDescCount >= iconButtonCount
        )
    }

    @Test
    fun phoneSyncRepository_doesNotUseSetUrgent_andThrottlesRapidUpdatesWithoutDroppingLatest() {
        val syncSource = mobileFile("src/main/java/com/haxtech/haxtracker/sync/PhoneSyncRepository.kt").readText()
        assertFalse(
            "PhoneSyncRepository must not use setUrgent() on PutDataRequest to protect Wear battery",
            syncSource.contains(".setUrgent()")
        )

        var fakeNow = 1000L
        val dispatched = java.util.Collections.synchronizedList(mutableListOf<Int>())
        val latch = CountDownLatch(2)
        PhoneSyncRepository.clock = { fakeNow }
        PhoneSyncRepository.onSyncDispatched = { state, _ ->
            dispatched.add(state.scoreTeamA)
            latch.countDown()
        }

        // Badminton uses rally scoring so every PointTeamA deterministically increments scoreTeamA.
        var state = GameEngine.newMatch(MatchConfig.defaultBadmintonDoubles())
        // 10 rapid score updates within 10ms (well under SYNC_THROTTLE_MS = 100ms)
        repeat(10) { idx ->
            fakeNow = 1000L + idx
            state = GameEngine.process(state, GameAction.PointTeamA)
            PhoneSyncRepository.syncStateToWatch(state)
        }

        // Only 1 immediate dispatch should have occurred during the burst
        assertEquals("Rapid burst must be throttled to 1 immediate sync", 1, dispatched.size)
        assertEquals(1, dispatched.first())

        // Wait for trailing-edge flush so the final state is not lost
        fakeNow = 1200L
        assertTrue("Trailing-edge sync did not fire in time", latch.await(2, TimeUnit.SECONDS))
        assertEquals("Trailing-edge sync must deliver the latest state", 2, dispatched.size)
        assertEquals(10, dispatched.last())
    }
}
