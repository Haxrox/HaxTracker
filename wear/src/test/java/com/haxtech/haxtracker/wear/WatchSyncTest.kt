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
}
