package com.haxtech.haxtracker.core

import com.haxtech.haxtracker.core.engine.CourtPositionCalculator
import com.haxtech.haxtracker.core.engine.CourtSlot
import com.haxtech.haxtracker.core.engine.GameEngine
import com.haxtech.haxtracker.core.model.CourtHalf
import com.haxtech.haxtracker.core.model.GameAction
import com.haxtech.haxtracker.core.model.MatchConfig
import com.haxtech.haxtracker.core.model.MatchFormat
import com.haxtech.haxtracker.core.model.Sport
import com.haxtech.haxtracker.core.model.TeamSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    @Test
    fun testBadmintonDoublesScoringAndRotations() {
        val config = MatchConfig.defaultBadmintonDoubles("Lions", "Tigers")
        var state = GameEngine.newMatch(config)

        // Game begins at 0-0, Team A serves from Right court
        assertEquals(0, state.scoreTeamA)
        assertEquals(0, state.scoreTeamB)
        assertEquals(TeamSide.TEAM_A, state.servingTeam)
        assertEquals(CourtHalf.RIGHT, state.servingCourt)
        assertEquals("Player A1", state.servingPlayer.name)

        // Point 1: Team A wins
        state = GameEngine.process(state, GameAction.PointTeamA)
        assertEquals(1, state.scoreTeamA)
        assertEquals(0, state.scoreTeamB)
        assertEquals(TeamSide.TEAM_A, state.servingTeam)
        // Same server switches to Left court!
        assertEquals("Player A1", state.servingPlayer.name)
        assertEquals(CourtHalf.LEFT, state.servingCourt)

        // Point 2: Team A wins again
        state = GameEngine.process(state, GameAction.PointTeamA)
        assertEquals(2, state.scoreTeamA)
        assertEquals(0, state.scoreTeamB)
        assertEquals("Player A1", state.servingPlayer.name)
        assertEquals(CourtHalf.RIGHT, state.servingCourt)

        // Point 3: Team B wins (Side-out!)
        state = GameEngine.process(state, GameAction.PointTeamB)
        assertEquals(2, state.scoreTeamA)
        assertEquals(1, state.scoreTeamB)
        assertEquals(TeamSide.TEAM_B, state.servingTeam)
        // Team B score is 1 (ODD) -> server must be in the Left court!
        assertEquals(CourtHalf.LEFT, state.servingCourt)
        assertEquals(state.teamBPositions.leftCourtPlayer.id, state.servingPlayer.id)
    }

    @Test
    fun testBadmintonWinByTwoAndCapAt30() {
        var config = MatchConfig.defaultBadmintonDoubles()
        var state = GameEngine.newMatch(config)

        // Advance score to 20-20 (Deuce)
        repeat(20) {
            state = GameEngine.process(state, GameAction.PointTeamA)
            state = GameEngine.process(state, GameAction.PointTeamB)
        }
        assertEquals(20, state.scoreTeamA)
        assertEquals(20, state.scoreTeamB)
        assertFalse(state.isGameFinished)

        // Team A reaches 21, but winByTwo requires 22
        state = GameEngine.process(state, GameAction.PointTeamA)
        assertEquals(21, state.scoreTeamA)
        assertFalse(state.isGameFinished)

        // Team A reaches 22-20 -> wins game!
        state = GameEngine.process(state, GameAction.PointTeamA)
        assertEquals(22, state.scoreTeamA)
        assertEquals(20, state.scoreTeamB)
        assertTrue(state.isGameFinished)
        assertEquals(TeamSide.TEAM_A, state.gameWinner)
    }

    @Test
    fun testPickleballDoublesSideOutAndFirstServerException() {
        val config = MatchConfig.defaultPickleballDoubles("Thunder", "Lightning")
        var state = GameEngine.newMatch(config)

        // 1. Initial State: USAPA rule 0-0-2 (Server 2 begins)
        assertEquals(0, state.scoreTeamA)
        assertEquals(0, state.scoreTeamB)
        assertEquals(2, state.serverNumber)
        assertEquals("0 - 0 - 2", state.calloutScore)
        assertEquals(TeamSide.TEAM_A, state.servingTeam)

        // 2. Team A loses the rally: Since it was Server 2, immediate side-out to Team B!
        state = GameEngine.process(state, GameAction.PointTeamB)
        assertEquals(0, state.scoreTeamA)
        assertEquals(0, state.scoreTeamB) // Side-out scoring: receiver does not score!
        assertEquals(TeamSide.TEAM_B, state.servingTeam)
        assertEquals(1, state.serverNumber) // Team B starts with Server 1
        assertEquals("0 - 0 - 1", state.calloutScore)

        // 3. Team B wins a rally: Serving team scores 1 point!
        state = GameEngine.process(state, GameAction.PointTeamB)
        assertEquals(1, state.scoreTeamB)
        assertEquals(0, state.scoreTeamA)
        assertEquals(1, state.serverNumber)
        assertEquals("1 - 0 - 1", state.calloutScore)

        // 4. Team B loses a rally: First fault -> moves to Server 2 on Team B!
        state = GameEngine.process(state, GameAction.PointTeamA)
        assertEquals(1, state.scoreTeamB) // No score
        assertEquals(0, state.scoreTeamA)
        assertEquals(2, state.serverNumber) // Server 2
        assertEquals("1 - 0 - 2", state.calloutScore)

        // 5. Team B loses a rally: Second fault -> Side-out to Team A!
        state = GameEngine.process(state, GameAction.PointTeamA)
        assertEquals(TeamSide.TEAM_A, state.servingTeam)
        assertEquals(1, state.serverNumber)
        assertEquals("0 - 1 - 1", state.calloutScore)
    }

    @Test
    fun testUndoRestoresExactPreviousState() {
        val config = MatchConfig.defaultPickleballDoubles()
        var state = GameEngine.newMatch(config)

        val initialState = state

        // Make several actions
        state = GameEngine.process(state, GameAction.PointServingTeam)
        state = GameEngine.process(state, GameAction.PointReceivingTeam)
        state = GameEngine.process(state, GameAction.PointServingTeam)

        assertTrue(state.canUndo())

        // Undo all
        state = GameEngine.process(state, GameAction.Undo)
        state = GameEngine.process(state, GameAction.Undo)
        state = GameEngine.process(state, GameAction.Undo)

        assertEquals(initialState.scoreTeamA, state.scoreTeamA)
        assertEquals(initialState.scoreTeamB, state.scoreTeamB)
        assertEquals(initialState.servingTeam, state.servingTeam)
        assertEquals(initialState.serverNumber, state.serverNumber)
        assertEquals(initialState.servingCourt, state.servingCourt)
        assertFalse(state.canUndo())
    }

    @Test
    fun testCourtPositionCalculator() {
        val config = MatchConfig.defaultBadmintonDoubles()
        val state = GameEngine.newMatch(config)
        val court = CourtPositionCalculator.calculate(state)

        assertEquals(CourtSlot.BOTTOM_RIGHT, court.activeServerSlot)
        assertEquals(CourtSlot.TOP_RIGHT, court.activeReceiverSlot)
        assertEquals(TeamSide.TEAM_A, court.servingTeam)
        assertNotNull(court.bottomRightPlayer)
        assertNotNull(court.topLeftPlayer)
    }
}
