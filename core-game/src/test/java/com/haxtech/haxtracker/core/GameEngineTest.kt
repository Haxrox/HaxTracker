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

        // Game begins at 0-0, starting team serves from Right court
        assertEquals(0, state.scoreTeamA)
        assertEquals(0, state.scoreTeamB)
        assertEquals(CourtHalf.RIGHT, state.servingCourt)

        val servingTeam = state.servingTeam
        val action1 = if (servingTeam == TeamSide.TEAM_A) GameAction.PointTeamA else GameAction.PointTeamB
        val initialServerName = state.servingPlayer.name

        // Point 1: Serving team wins
        state = GameEngine.process(state, action1)
        val score1 = if (servingTeam == TeamSide.TEAM_A) state.scoreTeamA else state.scoreTeamB
        assertEquals(1, score1)
        assertEquals(servingTeam, state.servingTeam)
        // Same server switches to Left court!
        assertEquals(initialServerName, state.servingPlayer.name)
        assertEquals(CourtHalf.LEFT, state.servingCourt)

        // Point 2: Serving team wins again
        state = GameEngine.process(state, action1)
        val score2 = if (servingTeam == TeamSide.TEAM_A) state.scoreTeamA else state.scoreTeamB
        assertEquals(2, score2)
        assertEquals(initialServerName, state.servingPlayer.name)
        assertEquals(CourtHalf.RIGHT, state.servingCourt)

        // Point 3: Opponent team wins (Side-out!)
        val opponentTeam = servingTeam.other()
        val action2 = if (opponentTeam == TeamSide.TEAM_A) GameAction.PointTeamA else GameAction.PointTeamB
        state = GameEngine.process(state, action2)
        assertEquals(opponentTeam, state.servingTeam)
        // Opponent team score is 1 (ODD) -> server must be in the Left court!
        assertEquals(CourtHalf.LEFT, state.servingCourt)
        val opponentPositions = if (opponentTeam == TeamSide.TEAM_A) state.teamAPositions else state.teamBPositions
        assertEquals(opponentPositions.leftCourtPlayer.id, state.servingPlayer.id)
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

        val initialServerTeam = state.servingTeam
        val initialReceiverTeam = initialServerTeam.other()

        // 1. Initial State: USAPA rule 0-0-2 (Server 2 begins)
        assertEquals(0, state.scoreTeamA)
        assertEquals(0, state.scoreTeamB)
        assertEquals(2, state.serverNumber)
        assertEquals("0 - 0 - 2", state.calloutScore)

        // 2. Initial serving team loses the rally: Since it was Server 2, immediate side-out to receiving team!
        val pointInitialReceiver = if (initialReceiverTeam == TeamSide.TEAM_A) GameAction.PointTeamA else GameAction.PointTeamB
        state = GameEngine.process(state, pointInitialReceiver)
        assertEquals(0, state.scoreTeamA)
        assertEquals(0, state.scoreTeamB) // Side-out scoring: receiver does not score!
        assertEquals(initialReceiverTeam, state.servingTeam)
        assertEquals(1, state.serverNumber) // New serving team starts with Server 1
        assertEquals("0 - 0 - 1", state.calloutScore)

        // 3. New serving team wins a rally: Serving team scores 1 point!
        state = GameEngine.process(state, pointInitialReceiver)
        val scoreNewServer = if (initialReceiverTeam == TeamSide.TEAM_A) state.scoreTeamA else state.scoreTeamB
        assertEquals(1, scoreNewServer)
        assertEquals(1, state.serverNumber)
        assertEquals("1 - 0 - 1", state.calloutScore)

        // 4. New serving team loses a rally: First fault -> moves to Server 2!
        val pointInitialServer = if (initialServerTeam == TeamSide.TEAM_A) GameAction.PointTeamA else GameAction.PointTeamB
        state = GameEngine.process(state, pointInitialServer)
        assertEquals(1, scoreNewServer) // No score
        assertEquals(2, state.serverNumber) // Server 2
        assertEquals("1 - 0 - 2", state.calloutScore)

        // 5. New serving team loses a rally: Second fault -> Side-out back to initial serving team!
        state = GameEngine.process(state, pointInitialServer)
        assertEquals(initialServerTeam, state.servingTeam)
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
    fun testBadmintonSinglesScoringAndRotations() {
        val config = MatchConfig.defaultBadmintonSingles("Alice", "Bob")
        var state = GameEngine.newMatch(config)

        // Starting team is randomized; force servingTeam to TEAM_A for consistent test expectations
        val servingTeam = state.servingTeam

        // Initial state: score 0-0, Even score means serve from RIGHT court
        assertEquals(0, state.scoreTeamA)
        assertEquals(0, state.scoreTeamB)
        assertEquals(CourtHalf.RIGHT, state.servingCourt)

        // Point 1: Serving team wins rally
        val action1 = if (servingTeam == TeamSide.TEAM_A) GameAction.PointTeamA else GameAction.PointTeamB
        state = GameEngine.process(state, action1)
        val score1 = if (servingTeam == TeamSide.TEAM_A) state.scoreTeamA else state.scoreTeamB
        assertEquals(1, score1)
        assertEquals(servingTeam, state.servingTeam)
        // Score is 1 (ODD) -> Serve court must switch to LEFT
        assertEquals(CourtHalf.LEFT, state.servingCourt)

        // Point 2: Serving team wins rally again
        state = GameEngine.process(state, action1)
        val score2 = if (servingTeam == TeamSide.TEAM_A) state.scoreTeamA else state.scoreTeamB
        assertEquals(2, score2)
        assertEquals(servingTeam, state.servingTeam)
        // Score is 2 (EVEN) -> Serve court switches back to RIGHT
        assertEquals(CourtHalf.RIGHT, state.servingCourt)

        // Point 3: Receiving team wins rally (Side-out)
        val receivingTeam = servingTeam.other()
        val action2 = if (receivingTeam == TeamSide.TEAM_A) GameAction.PointTeamA else GameAction.PointTeamB
        state = GameEngine.process(state, action2)
        assertEquals(receivingTeam, state.servingTeam)
        val receiverScore = if (receivingTeam == TeamSide.TEAM_A) state.scoreTeamA else state.scoreTeamB
        assertEquals(1, receiverScore)
        // Receiver score is 1 (ODD) -> Serving court for new server must be LEFT
        assertEquals(CourtHalf.LEFT, state.servingCourt)
    }

    @Test
    fun testPickleballSinglesScoringAndSideOuts() {
        val config = MatchConfig.defaultPickleballSingles("Charlie", "Dave")
        var state = GameEngine.newMatch(config)

        val initialServer = state.servingTeam

        // 1. Initial State: 0-0 in Singles
        assertEquals(0, state.scoreTeamA)
        assertEquals(0, state.scoreTeamB)
        assertEquals(CourtHalf.RIGHT, state.servingCourt)
        assertEquals("0 - 0", state.calloutScore)

        // 2. Serving team wins rally -> scores 1 point & court moves to LEFT (score 1 is ODD)
        val pointServer = if (initialServer == TeamSide.TEAM_A) GameAction.PointTeamA else GameAction.PointTeamB
        state = GameEngine.process(state, pointServer)
        val serverScore1 = if (initialServer == TeamSide.TEAM_A) state.scoreTeamA else state.scoreTeamB
        assertEquals(1, serverScore1)
        assertEquals(initialServer, state.servingTeam)
        assertEquals(CourtHalf.LEFT, state.servingCourt)

        // 3. Receiving team wins rally -> Side-out! Serving team changes, score does NOT increase
        val initialReceiver = initialServer.other()
        val pointReceiver = if (initialReceiver == TeamSide.TEAM_A) GameAction.PointTeamA else GameAction.PointTeamB
        state = GameEngine.process(state, pointReceiver)
        assertEquals(initialReceiver, state.servingTeam)
        val newServerScore = if (initialReceiver == TeamSide.TEAM_A) state.scoreTeamA else state.scoreTeamB
        assertEquals(0, newServerScore) // Receiver score was 0, no score change on side-out
        assertEquals(CourtHalf.RIGHT, state.servingCourt) // Score 0 is EVEN -> RIGHT court

        // 4. New server wins rally -> scores point 1 & court moves to LEFT
        state = GameEngine.process(state, pointReceiver)
        val newServerScore1 = if (initialReceiver == TeamSide.TEAM_A) state.scoreTeamA else state.scoreTeamB
        assertEquals(1, newServerScore1)
        assertEquals(CourtHalf.LEFT, state.servingCourt)
    }

    @Test
    fun testPickleballDoublesFullGameProgressionAndWinByTwo() {
        val config = MatchConfig.defaultPickleballDoubles("Alpha", "Beta")
        var state = GameEngine.newMatch(config)

        val server = state.servingTeam
        val pointServer = if (server == TeamSide.TEAM_A) GameAction.PointTeamA else GameAction.PointTeamB

        // Advance score to 10-0
        repeat(10) {
            state = GameEngine.process(state, pointServer)
        }
        val topScore = if (server == TeamSide.TEAM_A) state.scoreTeamA else state.scoreTeamB
        assertEquals(10, topScore)
        assertFalse(state.isGameFinished)

        // 11th point -> Game finished! (11-0 wins by 2)
        state = GameEngine.process(state, pointServer)
        assertTrue(state.isGameFinished)
        assertEquals(server, state.gameWinner)
        assertEquals(1, if (server == TeamSide.TEAM_A) state.gameWinsTeamA else state.gameWinsTeamB)

        // Transition to Game 2
        state = GameEngine.process(state, GameAction.StartNextGame())
        assertEquals(1, state.currentGameIndex)
        assertEquals(0, state.scoreTeamA)
        assertEquals(0, state.scoreTeamB)
        assertFalse(state.isGameFinished)
    }

    @Test
    fun testManualActions() {
        val config = MatchConfig.defaultBadmintonDoubles()
        var state = GameEngine.newMatch(config)

        assertFalse(state.isSideSwapped)
        state = GameEngine.process(state, GameAction.SwitchSidesManual)
        assertTrue(state.isSideSwapped)

        val originalServer = state.servingPlayer
        state = GameEngine.process(state, GameAction.SwitchServerManual)
        assertTrue(originalServer.id != state.servingPlayer.id)
    }

    @Test
    fun testCourtPositionCalculator() {
        val config = MatchConfig.defaultBadmintonDoubles()
        val state = GameEngine.newMatch(config)
        val court = CourtPositionCalculator.calculate(state)

        if (state.servingTeam == TeamSide.TEAM_A) {
            assertEquals(CourtSlot.BOTTOM_RIGHT, court.activeServerSlot)
            assertEquals(CourtSlot.TOP_LEFT, court.activeReceiverSlot)
        } else {
            assertEquals(CourtSlot.TOP_LEFT, court.activeServerSlot)
            assertEquals(CourtSlot.BOTTOM_RIGHT, court.activeReceiverSlot)
        }
        assertEquals(state.servingTeam, court.servingTeam)
        assertNotNull(court.bottomRightPlayer)
        assertNotNull(court.topLeftPlayer)
    }
}
