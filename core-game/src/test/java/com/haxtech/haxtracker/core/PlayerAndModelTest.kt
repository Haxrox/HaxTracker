package com.haxtech.haxtracker.core

import com.haxtech.haxtracker.core.engine.GameEngine
import com.haxtech.haxtracker.core.model.*
import org.junit.Assert.*
import org.junit.Test

class PlayerAndModelTest {

    @Test
    fun testPlayerShortNameGeneration() {
        val p1 = Player("1", "Player A1")
        assertEquals("A1", p1.shortName)

        val p2 = Player("2", "Player B2")
        assertEquals("B2", p2.shortName)

        val p3 = Player("3", "John Doe")
        assertEquals("JD", p3.shortName)

        val p4 = Player("4", "Sam")
        assertEquals("SAM", p4.shortName)

        val p5 = Player("5", "Team Alpha Player 1")
        assertEquals("A1", p5.shortName)

        val pCustom = Player("6", "Custom Name", "CN")
        assertEquals("CN", pCustom.shortName)
    }

    @Test
    fun testCalloutScoreFormatting() {
        val badmintonConfig = MatchConfig.defaultBadmintonDoubles("A", "B")
        val badmintonState = GameEngine.newMatch(badmintonConfig)
        assertEquals("0 - 0", badmintonState.calloutScore)

        val pickleballConfig = MatchConfig.defaultPickleballDoubles("A", "B")
        val pickleballState = GameEngine.newMatch(pickleballConfig)
        assertEquals("0 - 0 - 2", pickleballState.calloutScore)
    }

    @Test
    fun testSpokenAnnouncementFormatting() {
        val config = MatchConfig.defaultBadmintonDoubles("Lions", "Tigers")
        var state = GameEngine.newMatch(config)

        assertTrue(state.spokenAnnouncement.contains("0, 0"))

        // Game point state
        state = state.copy(scoreTeamA = 20, scoreTeamB = 18)
        assertTrue(state.isGamePoint)
        assertTrue(state.spokenAnnouncement.startsWith("Game point!"))

        // Match point state
        state = state.copy(gameWinsTeamA = 1, scoreTeamA = 20, scoreTeamB = 18)
        assertTrue(state.isMatchPoint)
        assertTrue(state.spokenAnnouncement.startsWith("Match point!"))

        // Game finished state
        state = state.copy(isGameFinished = true, gameWinner = TeamSide.TEAM_A)
        assertTrue(state.spokenAnnouncement.contains("Lions wins game"))

        // Match finished state
        state = state.copy(isMatchFinished = true, matchWinner = TeamSide.TEAM_A)
        assertTrue(state.spokenAnnouncement.contains("Match finished. Lions wins!"))
    }

    @Test
    fun testTeamCourtPositionsSwapped() {
        val p1 = Player("1", "P1", "P1")
        val p2 = Player("2", "P2", "P2")
        val pos = TeamCourtPositions(leftCourtPlayer = p1, rightCourtPlayer = p2)

        assertEquals(p1, pos.leftCourtPlayer)
        assertEquals(p2, pos.rightCourtPlayer)

        val swapped = pos.swapped()
        assertEquals(p2, swapped.leftCourtPlayer)
        assertEquals(p1, swapped.rightCourtPlayer)
    }
}
