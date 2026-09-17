package com.haxtech.haxtracker.wear

import com.haxtech.haxtracker.core.engine.GameEngine
import com.haxtech.haxtracker.core.model.GameAction
import com.haxtech.haxtracker.core.model.MatchConfig
import com.haxtech.haxtracker.wear.sync.WatchSyncRepository
import org.junit.Assert.*
import org.junit.Test

class WatchSyncTest {

    @Test
    fun testWatchSyncRepositoryUpdateLocalState() {
        val config = MatchConfig.defaultBadmintonDoubles("Watch Team A", "Watch Team B")
        val initial = GameEngine.newMatch(config)

        WatchSyncRepository.updateLocalState(initial)
        assertEquals("Watch Team A", WatchSyncRepository.matchState.value.config.teamA.name)

        val nextState = GameEngine.process(initial, GameAction.PointTeamA)
        WatchSyncRepository.updateLocalState(nextState)

        assertEquals(1, WatchSyncRepository.matchState.value.scoreTeamA)
        assertTrue(WatchSyncRepository.matchState.value.canUndo())
    }

    @Test
    fun testWatchSyncRepositoryOnStateReceivedFromPhone() {
        val config = MatchConfig.defaultPickleballDoubles("Phone Team A", "Phone Team B")
        val phoneState = GameEngine.newMatch(config)

        WatchSyncRepository.onStateReceivedFromPhone(phoneState)

        assertEquals("Phone Team A", WatchSyncRepository.matchState.value.config.teamA.name)
        assertTrue(WatchSyncRepository.isConnectedToPhone.value)
    }

    @Test
    fun testWatchGameActionsAndUndoRedo() {
        val config = MatchConfig.defaultBadmintonDoubles("Lions", "Tigers")
        var state = GameEngine.newMatch(config)
        WatchSyncRepository.updateLocalState(state)

        // Point Team A
        state = GameEngine.process(state, GameAction.PointTeamA)
        WatchSyncRepository.updateLocalState(state)
        assertEquals(1, WatchSyncRepository.matchState.value.scoreTeamA)
        assertTrue(WatchSyncRepository.matchState.value.canUndo())

        // Point Team B
        state = GameEngine.process(state, GameAction.PointTeamB)
        WatchSyncRepository.updateLocalState(state)
        assertEquals(1, WatchSyncRepository.matchState.value.scoreTeamB)

        // Undo
        state = GameEngine.process(state, GameAction.Undo)
        WatchSyncRepository.updateLocalState(state)
        assertEquals(0, WatchSyncRepository.matchState.value.scoreTeamB)
        assertTrue(WatchSyncRepository.matchState.value.canRedo())

        // Redo
        state = GameEngine.process(state, GameAction.Redo)
        WatchSyncRepository.updateLocalState(state)
        assertEquals(1, WatchSyncRepository.matchState.value.scoreTeamB)

        // Switch Sides
        assertFalse(WatchSyncRepository.matchState.value.isSideSwapped)
        state = GameEngine.process(state, GameAction.SwitchSidesManual)
        WatchSyncRepository.updateLocalState(state)
        assertTrue(WatchSyncRepository.matchState.value.isSideSwapped)

        // Reset Match
        val resetConfig = MatchConfig.defaultPickleballDoubles("Thunder", "Storm")
        state = GameEngine.newMatch(resetConfig)
        WatchSyncRepository.updateLocalState(state)
        assertEquals(0, WatchSyncRepository.matchState.value.scoreTeamA)
        assertEquals(0, WatchSyncRepository.matchState.value.scoreTeamB)
        assertEquals("Thunder", WatchSyncRepository.matchState.value.config.teamA.name)
    }
}
