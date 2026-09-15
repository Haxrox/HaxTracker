package com.haxtech.haxtracker.core

import com.haxtech.haxtracker.core.engine.GameEngine
import com.haxtech.haxtracker.core.model.*
import com.haxtech.haxtracker.core.sync.MatchStateSerializer
import org.junit.Assert.*
import org.junit.Test

class MatchStateSerializerTest {

    @Test
    fun testMatchStateJsonRoundTrip() {
        val config = MatchConfig.defaultBadmintonDoubles("Red Team", "Blue Team")
        var state = GameEngine.newMatch(config)
        state = GameEngine.process(state, GameAction.PointTeamA)
        state = GameEngine.process(state, GameAction.Undo)

        val jsonStr = MatchStateSerializer.toJson(state)
        assertNotNull(jsonStr)
        assertTrue(jsonStr.contains("Red Team"))
        assertTrue(jsonStr.contains("Blue Team"))

        val deserialized = MatchStateSerializer.fromJson(jsonStr)
        assertNotNull(deserialized)
        assertEquals(state.config.sport, deserialized!!.config.sport)
        assertEquals(state.config.format, deserialized.config.format)
        assertEquals(state.scoreTeamA, deserialized.scoreTeamA)
        assertEquals(state.scoreTeamB, deserialized.scoreTeamB)
        assertEquals(state.servingTeam, deserialized.servingTeam)
        assertEquals(state.canUndo(), deserialized.canUndo())
        assertEquals(state.canRedo(), deserialized.canRedo())
    }

    @Test
    fun testAllGameActionsJsonRoundTrip() {
        val actions = listOf(
            GameAction.PointTeamA,
            GameAction.PointTeamB,
            GameAction.PointServingTeam,
            GameAction.PointReceivingTeam,
            GameAction.Undo,
            GameAction.Redo,
            GameAction.SwitchSidesManual,
            GameAction.SwitchServerManual,
            GameAction.StartNextGame(),
            GameAction.ResetMatch()
        )

        for (action in actions) {
            val json = MatchStateSerializer.actionToJson(action)
            assertNotNull(json)
            val parsed = MatchStateSerializer.actionFromJson(json)
            assertNotNull("Failed to parse action: $action", parsed)
            assertEquals(action::class, parsed!!::class)
        }
    }

    @Test
    fun testMalformedJsonHandling() {
        val invalidJson = "{ invalid_json: true }"
        val result = MatchStateSerializer.fromJson(invalidJson)
        assertNull(result)

        val invalidActionJson = "{ actionType: \"UnknownAction\" }"
        val actionResult = MatchStateSerializer.actionFromJson(invalidActionJson)
        assertNull(actionResult)
    }
}
