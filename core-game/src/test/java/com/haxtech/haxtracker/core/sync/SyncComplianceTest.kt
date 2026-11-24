package com.haxtech.haxtracker.core.sync

import com.haxtech.haxtracker.core.engine.GameEngine
import com.haxtech.haxtracker.core.model.GameAction
import com.haxtech.haxtracker.core.model.MatchConfig
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncComplianceTest {

    private val sensitiveKeys = setOf(
        "email", "phone", "phonenumber", "location", "latitude", "longitude",
        "address", "ssn", "password", "token", "secret", "deviceid", "advertisingid", "adid"
    )

    private fun assertNoSensitiveKeysRecursive(json: JSONObject, path: String = "root") {
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            assertFalse(
                "Payload at $path contains sensitive key: $key",
                sensitiveKeys.contains(key.lowercase())
            )
            when (val value = json.opt(key)) {
                is JSONObject -> assertNoSensitiveKeysRecursive(value, "$path.$key")
            }
        }
    }

    @Test
    fun `test match state serialization does not contain PII`() {
        val config = MatchConfig.defaultPickleballDoubles(
            teamAName = "Team Alpha",
            teamBName = "Team Beta"
        )
        val initialState = GameEngine.newMatch(config)

        val jsonStr = MatchStateSerializer.toJson(initialState)
        val json = JSONObject(jsonStr)

        assertNoSensitiveKeysRecursive(json)
    }

    @Test
    fun `test game action serialization does not contain PII`() {
        val actions = listOf(
            GameAction.PointTeamA,
            GameAction.PointTeamB,
            GameAction.Undo,
            GameAction.Redo,
            GameAction.SwitchSidesManual,
            GameAction.ResetMatch(MatchConfig.defaultBadmintonDoubles())
        )
        for (action in actions) {
            val json = JSONObject(MatchStateSerializer.actionToJson(action))
            assertNoSensitiveKeysRecursive(json, "action(${action::class.simpleName})")
        }
    }

    @Test
    fun `test match state serialization includes only relevant game data`() {
        val config = MatchConfig.defaultPickleballDoubles()
        val initialState = GameEngine.newMatch(config)
        val jsonStr = MatchStateSerializer.toJson(initialState)
        val json = JSONObject(jsonStr)

        assertTrue(json.has("scoreTeamA"))
        assertTrue(json.has("scoreTeamB"))
        assertTrue(json.has("servingTeam"))
        assertTrue(json.has("servingPlayer"))
        assertTrue(json.has("config"))
    }
}
