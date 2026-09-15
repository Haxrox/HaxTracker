package com.haxtech.haxtracker

import com.haxtech.haxtracker.core.model.*
import com.haxtech.haxtracker.sync.PhoneSyncRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MatchViewModelAndSyncTest {

    @Before
    fun setUp() {
        // Reset sync repository state
        PhoneSyncRepository.closeMatch()
    }

    @Test
    fun testPhoneSyncRepositoryStartAndCloseMatch() {
        assertFalse(PhoneSyncRepository.isMatchActive.value)

        val config = MatchConfig.defaultBadmintonDoubles("Lions", "Tigers")
        PhoneSyncRepository.startMatch(config)

        assertTrue(PhoneSyncRepository.isMatchActive.value)
        assertEquals("Lions", PhoneSyncRepository.matchState.value.config.teamA.name)
        assertEquals("Tigers", PhoneSyncRepository.matchState.value.config.teamB.name)

        PhoneSyncRepository.closeMatch()
        assertFalse(PhoneSyncRepository.isMatchActive.value)
    }

    @Test
    fun testPhoneSyncRepositoryActionDispatching() {
        val config = MatchConfig.defaultBadmintonDoubles("A", "B")
        PhoneSyncRepository.startMatch(config)

        val initialScoreA = PhoneSyncRepository.matchState.value.scoreTeamA

        PhoneSyncRepository.dispatchActionFromWatch(GameAction.PointTeamA)
        assertEquals(initialScoreA + 1, PhoneSyncRepository.matchState.value.scoreTeamA)

        assertTrue(PhoneSyncRepository.matchState.value.canUndo())
        PhoneSyncRepository.dispatchActionFromWatch(GameAction.Undo)
        assertEquals(initialScoreA, PhoneSyncRepository.matchState.value.scoreTeamA)

        assertTrue(PhoneSyncRepository.matchState.value.canRedo())
        PhoneSyncRepository.dispatchActionFromWatch(GameAction.Redo)
        assertEquals(initialScoreA + 1, PhoneSyncRepository.matchState.value.scoreTeamA)
    }

    @Test
    fun testPhoneSyncRepositoryResetMatch() {
        val config = MatchConfig.defaultBadmintonDoubles("X", "Y")
        PhoneSyncRepository.startMatch(config)

        PhoneSyncRepository.dispatchActionFromWatch(GameAction.PointTeamA)
        PhoneSyncRepository.dispatchActionFromWatch(GameAction.PointTeamB)

        val newConfig = MatchConfig.defaultPickleballDoubles("P1", "P2")
        PhoneSyncRepository.dispatchActionFromWatch(GameAction.ResetMatch(newConfig))

        assertEquals(Sport.PICKLEBALL, PhoneSyncRepository.matchState.value.config.sport)
        assertEquals(0, PhoneSyncRepository.matchState.value.scoreTeamA)
        assertEquals(0, PhoneSyncRepository.matchState.value.scoreTeamB)
    }
}
