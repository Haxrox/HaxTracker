package com.haxtech.haxtracker.core.engine

import com.haxtech.haxtracker.core.model.*
import kotlin.random.Random

object GameEngine {

    fun newMatch(config: MatchConfig, nowMs: Long = System.currentTimeMillis()): MatchState {
        val teamAPos = TeamCourtPositions(
            leftCourtPlayer = config.teamA.player2 ?: config.teamA.player1,
            rightCourtPlayer = config.teamA.player1
        )
        val teamBPos = TeamCourtPositions(
            leftCourtPlayer = config.teamB.player1,
            rightCourtPlayer = config.teamB.player2 ?: config.teamB.player1
        )

        // Randomize starting team
        val startingTeam = if (Random.nextBoolean()) TeamSide.TEAM_A else TeamSide.TEAM_B
        val pos = if (startingTeam == TeamSide.TEAM_A) teamAPos else teamBPos
        val startingServer = pos.rightCourtPlayer ?: pos.leftCourtPlayer
        val opponentPos = if (startingTeam == TeamSide.TEAM_A) teamBPos else teamAPos
        val startingReceiver = opponentPos.rightCourtPlayer ?: opponentPos.leftCourtPlayer
        
        val serverNumber = if (config.sport == Sport.PICKLEBALL && config.format == MatchFormat.DOUBLES) 2 else 1

        return MatchState(
            config = config, currentGameIndex = 0, gameWinsTeamA = 0, gameWinsTeamB = 0,
            scoreTeamA = 0, scoreTeamB = 0, servingTeam = startingTeam,
            servingPlayer = startingServer, receivingPlayer = startingReceiver,
            serverNumber = serverNumber, servingCourt = CourtHalf.RIGHT,
            teamAPositions = teamAPos, teamBPositions = teamBPos,
            isGameFinished = false, isMatchFinished = false, isSideSwapped = false,
            gameWinner = null, matchWinner = null, rallyStartTimeMs = nowMs,
            rallyDurationsSec = emptyList(), pastGameScores = emptyList(), history = emptyList()
        )
    }

    fun process(state: MatchState, action: GameAction, nowMs: Long = System.currentTimeMillis()): MatchState {
        return when (action) {
            is GameAction.Undo -> handleUndo(state)
            is GameAction.StartNextGame -> handleStartNextGame(state, nowMs)
            is GameAction.ResetMatch -> newMatch(action.config ?: state.config, nowMs)
            is GameAction.SwitchSidesManual -> state.copy(isSideSwapped = !state.isSideSwapped, history = state.history + state.copy(history = emptyList()))
            is GameAction.SwitchServerManual -> handleSwitchServerManual(state)
            is GameAction.PointTeamA -> handlePointWon(state, TeamSide.TEAM_A, nowMs)
            is GameAction.PointTeamB -> handlePointWon(state, TeamSide.TEAM_B, nowMs)
            is GameAction.PointServingTeam -> handlePointWon(state, state.servingTeam, nowMs)
            is GameAction.PointReceivingTeam -> handlePointWon(state, state.servingTeam.other(), nowMs)
        }
    }

    private fun handleUndo(state: MatchState): MatchState {
        val prev = state.history.lastOrNull() ?: return state
        val updatedHistory = state.history.dropLast(1)
        return prev.copy(history = updatedHistory)
    }

    private fun handleStartNextGame(state: MatchState, nowMs: Long): MatchState {
        if (!state.isGameFinished || state.isMatchFinished) return state
        val updatedPastScores = state.pastGameScores + Pair(state.scoreTeamA, state.scoreTeamB)
        val nextServingTeam = if ((state.currentGameIndex + 1) % 2 == 0) TeamSide.TEAM_A else TeamSide.TEAM_B
        
        val teamAPos = TeamCourtPositions(leftCourtPlayer = state.config.teamA.player2 ?: state.config.teamA.player1, rightCourtPlayer = state.config.teamA.player1)
        val teamBPos = TeamCourtPositions(leftCourtPlayer = state.config.teamB.player1, rightCourtPlayer = state.config.teamB.player2 ?: state.config.teamB.player1)

        val serverPos = if (nextServingTeam == TeamSide.TEAM_A) teamAPos else teamBPos
        val startingServer = serverPos.rightCourtPlayer ?: serverPos.leftCourtPlayer
        val opponentPos = if (nextServingTeam == TeamSide.TEAM_A) teamBPos else teamAPos
        val startingReceiver = opponentPos.rightCourtPlayer ?: opponentPos.leftCourtPlayer
        
        return state.copy(
            currentGameIndex = state.currentGameIndex + 1, scoreTeamA = 0, scoreTeamB = 0,
            servingTeam = nextServingTeam, servingPlayer = startingServer,
            receivingPlayer = startingReceiver, serverNumber = (if (state.config.sport == Sport.PICKLEBALL && state.config.format == MatchFormat.DOUBLES) 2 else 1),
            servingCourt = CourtHalf.RIGHT, teamAPositions = teamAPos, teamBPositions = teamBPos,
            isGameFinished = false, gameWinner = null, rallyStartTimeMs = nowMs,
            pastGameScores = updatedPastScores, history = state.history + state.copy(history = emptyList())
        )
    }

    private fun handlePointWon(state: MatchState, rallyWinner: TeamSide, nowMs: Long): MatchState {
        if (state.isGameFinished || state.isMatchFinished) return state
        val rallyDuration = if (state.rallyStartTimeMs > 0) ((nowMs - state.rallyStartTimeMs) / 1000).toInt().coerceAtLeast(1) else 0
        val snapshot = state.copy(history = emptyList())
        val newHistory = state.history + snapshot

        return when (state.config.sport) {
            Sport.BADMINTON -> handleBadmintonPoint(state, rallyWinner, if (rallyDuration > 0) state.rallyDurationsSec + rallyDuration else state.rallyDurationsSec, newHistory, nowMs)
            Sport.PICKLEBALL -> handlePickleballPoint(state, rallyWinner, if (rallyDuration > 0) state.rallyDurationsSec + rallyDuration else state.rallyDurationsSec, newHistory, nowMs)
        }
    }

    private fun handleBadmintonPoint(state: MatchState, rallyWinner: TeamSide, rallies: List<Int>, history: List<MatchState>, nowMs: Long): MatchState {
        val newScoreA = if (rallyWinner == TeamSide.TEAM_A) state.scoreTeamA + 1 else state.scoreTeamA
        val newScoreB = if (rallyWinner == TeamSide.TEAM_B) state.scoreTeamB + 1 else state.scoreTeamB
        val servingTeamWon = (rallyWinner == state.servingTeam)

        var newTeamAPos = state.teamAPositions
        var newTeamBPos = state.teamBPositions
        val newServingTeam = rallyWinner
        val newServingPlayer: Player
        val newReceivingPlayer: Player
        val newServingCourt: CourtHalf

        if (state.config.format == MatchFormat.DOUBLES) {
            if (servingTeamWon) {
                if (newServingTeam == TeamSide.TEAM_A) { newTeamAPos = newTeamAPos.swapped() } else { newTeamBPos = newTeamBPos.swapped() }
                newServingPlayer = state.servingPlayer
                val currentServingPos = if (newServingTeam == TeamSide.TEAM_A) newTeamAPos else newTeamBPos
                newServingCourt = currentServingPos.courtOf(newServingPlayer)
                newReceivingPlayer = (if (newServingTeam == TeamSide.TEAM_A) newTeamBPos else newTeamAPos).playerAt(newServingCourt)
            } else {
                val winnerScore = if (newServingTeam == TeamSide.TEAM_A) newScoreA else newScoreB
                newServingCourt = if (winnerScore % 2 == 0) CourtHalf.RIGHT else CourtHalf.LEFT
                newServingPlayer = (if (newServingTeam == TeamSide.TEAM_A) newTeamAPos else newTeamBPos).playerAt(newServingCourt)
                newReceivingPlayer = (if (newServingTeam == TeamSide.TEAM_A) newTeamBPos else newTeamAPos).playerAt(newServingCourt)
            }
        } else {
            val winnerScore = if (newServingTeam == TeamSide.TEAM_A) newScoreA else newScoreB
            newServingCourt = if (winnerScore % 2 == 0) CourtHalf.RIGHT else CourtHalf.LEFT
            newServingPlayer = if (newServingTeam == TeamSide.TEAM_A) state.config.teamA.player1 else state.config.teamB.player1
            newReceivingPlayer = if (newServingTeam == TeamSide.TEAM_A) state.config.teamB.player1 else state.config.teamA.player1
        }

        return evaluateGameAndMatchEnd(state.copy(
            scoreTeamA = newScoreA, scoreTeamB = newScoreB,
            servingTeam = newServingTeam, servingPlayer = newServingPlayer,
            receivingPlayer = newReceivingPlayer, serverNumber = 1,
            servingCourt = newServingCourt, teamAPositions = newTeamAPos,
            teamBPositions = newTeamBPos, rallyStartTimeMs = nowMs,
            rallyDurationsSec = rallies, history = history
        ))
    }

    private fun handlePickleballPoint(state: MatchState, rallyWinner: TeamSide, rallies: List<Int>, history: List<MatchState>, nowMs: Long): MatchState {
        val isServingTeamWinner = (rallyWinner == state.servingTeam)
        var newScoreA = state.scoreTeamA; var newScoreB = state.scoreTeamB
        var newTeamAPos = state.teamAPositions; var newTeamBPos = state.teamBPositions
        var newServingTeam = state.servingTeam; var newServerNumber = state.serverNumber
        var newServingPlayer = state.servingPlayer; var newReceivingPlayer = state.receivingPlayer
        var newServingCourt = state.servingCourt

        if (state.config.format == MatchFormat.DOUBLES) {
            if (isServingTeamWinner) {
                if (state.servingTeam == TeamSide.TEAM_A) { newScoreA++; newTeamAPos = newTeamAPos.swapped() } else { newScoreB++; newTeamBPos = newTeamBPos.swapped() }
                newServingCourt = (if (state.servingTeam == TeamSide.TEAM_A) newTeamAPos else newTeamBPos).courtOf(state.servingPlayer)
                newReceivingPlayer = (if (state.servingTeam == TeamSide.TEAM_A) newTeamBPos else newTeamAPos).playerAt(newServingCourt)
            } else {
                if (state.serverNumber == 1) {
                    newServerNumber = 2
                    val servingPos = if (state.servingTeam == TeamSide.TEAM_A) state.teamAPositions else state.teamBPositions
                    val partner = if (servingPos.leftCourtPlayer.id == state.servingPlayer.id) (servingPos.rightCourtPlayer ?: servingPos.leftCourtPlayer) else servingPos.leftCourtPlayer
                    newServingPlayer = partner; newServingCourt = servingPos.courtOf(partner)
                    newReceivingPlayer = (if (state.servingTeam == TeamSide.TEAM_A) state.teamBPositions else state.teamAPositions).playerAt(newServingCourt)
                } else {
                    newServingTeam = state.servingTeam.other(); newServerNumber = 1
                    val newServingPos = if (newServingTeam == TeamSide.TEAM_A) state.teamAPositions else state.teamBPositions
                    newServingCourt = CourtHalf.RIGHT; newServingPlayer = newServingPos.playerAt(CourtHalf.RIGHT)
                    newReceivingPlayer = (if (newServingTeam == TeamSide.TEAM_A) state.teamBPositions else state.teamAPositions).playerAt(CourtHalf.RIGHT)
                }
            }
        } else {
            if (isServingTeamWinner) {
                if (state.servingTeam == TeamSide.TEAM_A) newScoreA++ else newScoreB++
                val newScore = if (state.servingTeam == TeamSide.TEAM_A) newScoreA else newScoreB
                newServingCourt = if (newScore % 2 == 0) CourtHalf.RIGHT else CourtHalf.LEFT
            } else {
                newServingTeam = state.servingTeam.other()
                val newScore = if (newServingTeam == TeamSide.TEAM_A) newScoreA else newScoreB
                newServingCourt = if (newScore % 2 == 0) CourtHalf.RIGHT else CourtHalf.LEFT
                newServingPlayer = if (newServingTeam == TeamSide.TEAM_A) state.config.teamA.player1 else state.config.teamB.player1
                newReceivingPlayer = if (newServingTeam == TeamSide.TEAM_A) state.config.teamB.player1 else state.config.teamA.player1
            }
        }

        return evaluateGameAndMatchEnd(state.copy(
            scoreTeamA = newScoreA, scoreTeamB = newScoreB,
            servingTeam = newServingTeam, servingPlayer = newServingPlayer,
            receivingPlayer = newReceivingPlayer, serverNumber = newServerNumber,
            servingCourt = newServingCourt, teamAPositions = newTeamAPos,
            teamBPositions = newTeamBPos, rallyStartTimeMs = nowMs,
            rallyDurationsSec = rallies, history = history
        ))
    }

    private fun evaluateGameAndMatchEnd(state: MatchState): MatchState {
        val target = state.config.winningScore; val cap = state.config.maxScoreCap
        val scoreA = state.scoreTeamA; val scoreB = state.scoreTeamB
        val aWon: Boolean; val bWon: Boolean
        if (cap != null && scoreA >= cap) { aWon = true; bWon = false }
        else if (cap != null && scoreB >= cap) { aWon = false; bWon = true }
        else if (state.config.winByTwo) { aWon = (scoreA >= target && scoreA >= scoreB + 2); bWon = (scoreB >= target && scoreB >= scoreA + 2) }
        else { aWon = scoreA >= target; bWon = scoreB >= target }
        if (!aWon && !bWon) return state.copy(isGameFinished = false, gameWinner = null)
        val gameWinner = if (aWon) TeamSide.TEAM_A else TeamSide.TEAM_B
        val winsA = if (aWon) state.gameWinsTeamA + 1 else state.gameWinsTeamA
        val winsB = if (bWon) state.gameWinsTeamB + 1 else state.gameWinsTeamB
        val gamesNeeded = (state.config.bestOfGames / 2) + 1
        val matchFinished = (winsA >= gamesNeeded || winsB >= gamesNeeded)
        return state.copy(isGameFinished = true, isMatchFinished = matchFinished, gameWinner = gameWinner, matchWinner = (if (matchFinished) (if (winsA >= gamesNeeded) TeamSide.TEAM_A else TeamSide.TEAM_B) else null), gameWinsTeamA = winsA, gameWinsTeamB = winsB)
    }

    private fun handleSwitchServerManual(state: MatchState): MatchState {
        val servingPos = if (state.servingTeam == TeamSide.TEAM_A) state.teamAPositions else state.teamBPositions
        val partner = if (servingPos.leftCourtPlayer.id == state.servingPlayer.id) (servingPos.rightCourtPlayer ?: servingPos.leftCourtPlayer) else servingPos.leftCourtPlayer
        val newServingCourt = servingPos.courtOf(partner)
        val newReceivingPlayer = (if (state.servingTeam == TeamSide.TEAM_A) state.teamBPositions else state.teamAPositions).playerAt(newServingCourt)
        return state.copy(servingPlayer = partner, servingCourt = newServingCourt, receivingPlayer = newReceivingPlayer, serverNumber = (if (state.serverNumber == 1) 2 else 1), history = state.history + state.copy(history = emptyList()))
    }
}
