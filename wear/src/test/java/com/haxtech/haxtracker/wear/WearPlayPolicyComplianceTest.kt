package com.haxtech.haxtracker.wear

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Automated compliance tests for Google Play Developer & Wear OS Policies (Wear module):
 * - Min/Target API Level & matching Application ID
 * - Permissions Minimalism & Watch Hardware Feature / Standalone declaration
 * - Android 12+ Exported Component Security & System Bind Permissions
 * - Battery & Performance (Complication update interval, no setUrgent())
 * - No Ads / Tracking SDKs
 */
class WearPlayPolicyComplianceTest {

    private val androidNs = "http://schemas.android.com/apk/res/android"

    private fun wearFile(relativePath: String): File {
        val candidates = listOf(
            File(System.getProperty("user.dir"), relativePath),
            File(System.getProperty("user.dir"), "wear/$relativePath")
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw AssertionError("Could not locate $relativePath")
    }

    private fun parseManifest() =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(wearFile("src/main/AndroidManifest.xml"))

    @Test
    fun buildGradle_meetsWearOsApiLevelsAndAppId() {
        val gradle = wearFile("build.gradle.kts").readText()
        val minSdk = Regex("""minSdk\s*=\s*(\d+)""").find(gradle)?.groupValues?.get(1)?.toIntOrNull()
        val targetSdk = Regex("""targetSdk\s*=\s*(\d+)""").find(gradle)?.groupValues?.get(1)?.toIntOrNull()
        val compileSdk = Regex("""compileSdk\s*=\s*(\d+)""").find(gradle)?.groupValues?.get(1)?.toIntOrNull()

        assertNotNull("minSdk must be declared", minSdk)
        assertNotNull("targetSdk must be declared", targetSdk)
        assertNotNull("compileSdk must be declared", compileSdk)
        assertTrue("Wear OS minSdk ($minSdk) must be >= 30 (Wear OS 3+)", minSdk!! >= 30)
        assertTrue("Wear OS targetSdk ($targetSdk) must be >= 35", targetSdk!! >= 35)
        assertTrue("Wear OS compileSdk ($compileSdk) must be >= 35", compileSdk!! >= 35)
        assertTrue(
            "Wear applicationId must match mobile (com.haxtech.haxtracker)",
            gradle.contains("""applicationId = "com.haxtech.haxtracker"""")
        )
    }

    @Test
    fun manifest_declaresWatchFeatureStandaloneAndMinimalPermissions() {
        val doc = parseManifest()

        val features = doc.getElementsByTagName("uses-feature")
        var hasWatchFeature = false
        for (i in 0 until features.length) {
            if (features.item(i).attributes.getNamedItemNS(androidNs, "name")?.nodeValue == "android.hardware.type.watch") {
                hasWatchFeature = true
            }
        }
        assertTrue("Wear manifest must declare android.hardware.type.watch", hasWatchFeature)

        val metaData = doc.getElementsByTagName("meta-data")
        var standaloneValue: String? = null
        var complicationUpdateSeconds: Int? = null
        for (i in 0 until metaData.length) {
            val name = metaData.item(i).attributes.getNamedItemNS(androidNs, "name")?.nodeValue
            val value = metaData.item(i).attributes.getNamedItemNS(androidNs, "value")?.nodeValue
            if (name == "com.google.android.wearable.standalone") standaloneValue = value
            if (name == "android.support.wearable.complications.UPDATE_PERIOD_SECONDS") {
                complicationUpdateSeconds = value?.toIntOrNull()
            }
        }
        assertNotNull("Wear manifest must declare com.google.android.wearable.standalone", standaloneValue)
        assertNotNull("Complication UPDATE_PERIOD_SECONDS must be set", complicationUpdateSeconds)
        assertTrue(
            "Complication update period ($complicationUpdateSeconds s) must be >= 1800s for battery compliance",
            complicationUpdateSeconds!! >= 1800
        )

        val permNodes = doc.getElementsByTagName("uses-permission")
        val declared = mutableListOf<String>()
        for (i in 0 until permNodes.length) {
            permNodes.item(i).attributes.getNamedItemNS(androidNs, "name")?.nodeValue?.let { declared.add(it) }
        }
        assertFalse("Wear manifest must not declare INTERNET", declared.contains("android.permission.INTERNET"))
        assertFalse("Wear manifest must not declare AD_ID", declared.contains("com.google.android.gms.permission.AD_ID"))
        assertTrue(
            "Wear permissions must be limited to WAKE_LOCK and VIBRATE",
            declared.toSet() == setOf("android.permission.WAKE_LOCK", "android.permission.VIBRATE")
        )
    }

    @Test
    fun manifest_securesExportedComponentsAndWearServices() {
        val doc = parseManifest()
        val servicePermissions = mutableMapOf<String, String?>()

        for (tag in listOf("activity", "service", "receiver")) {
            val nodes = doc.getElementsByTagName(tag)
            for (i in 0 until nodes.length) {
                val el = nodes.item(i)
                val name = el.attributes.getNamedItemNS(androidNs, "name")?.nodeValue ?: ""
                val exported = el.attributes.getNamedItemNS(androidNs, "exported")?.nodeValue
                val permission = el.attributes.getNamedItemNS(androidNs, "permission")?.nodeValue
                if (tag == "service") {
                    servicePermissions[name] = permission
                }
                var hasIntentFilter = false
                for (j in 0 until el.childNodes.length) {
                    if (el.childNodes.item(j).nodeName == "intent-filter") {
                        hasIntentFilter = true
                        break
                    }
                }
                if (hasIntentFilter) {
                    assertNotNull("$tag '$name' with intent-filter must declare android:exported", exported)
                }
            }
        }

        assertTrue(
            "MainComplicationService must require BIND_COMPLICATION_PROVIDER",
            servicePermissions.entries.any {
                it.key.contains("MainComplicationService") &&
                    it.value == "com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER"
            }
        )
        assertTrue(
            "MainTileService must require BIND_TILE_PROVIDER",
            servicePermissions.entries.any {
                it.key.contains("MainTileService") &&
                    it.value == "com.google.android.wearable.permission.BIND_TILE_PROVIDER"
            }
        )
    }

    @Test
    fun watchSyncRepository_doesNotUseSetUrgent() {
        val syncSource = wearFile("src/main/java/com/haxtech/haxtracker/wear/sync/WatchSyncRepository.kt").readText()
        assertFalse(
            "WatchSyncRepository must not use setUrgent() on PutDataRequest to protect Wear battery",
            syncSource.contains(".setUrgent()")
        )
    }

    @Test
    fun buildGradle_containsNoAdsOrTrackingSdks() {
        val gradle = wearFile("build.gradle.kts").readText().lowercase()
        val forbiddenDeps = listOf("play-services-ads", "firebase-analytics", "facebook-android-sdk", "billing")
        for (dep in forbiddenDeps) {
            assertFalse("Wear module must not include undeclared ad/tracking/billing SDK: $dep", gradle.contains(dep))
        }
    }
}
