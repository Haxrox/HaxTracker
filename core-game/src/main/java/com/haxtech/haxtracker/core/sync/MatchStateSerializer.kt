package com.haxtech.haxtracker.core.sync

import com.haxtech.haxtracker.core.model.*
import org.json.JSONObject

object MatchStateSerializer {

    private fun playerToJson(player: Player?): JSONObject? {
        if (player == null) return null
        val json = JSONObject()
        json.put("id", player.id)
        json.put("name", player.name)
        json.put("shortName", player.shortName)
        return json
    }

    private fun playerFromJson(json: JSONObject?): Player? {
        if (json == null) return null
        val id = json.optString("id", "")
        val name = json.optString("name", "Player")
        val shortName = json.optString("shortName", if (name.length <= 3) name.uppercase() else name.take(2).uppercase())
        return Player(id = id, name = name, shortName = shortName)
    }

    private fun teamToJson(team: Team): JSONObject {
        val json = JSONObject()
        json.put("side", team.side.name)
        json.put("name", team.name)
        json.put("player1", playerToJson(team.player1))
        if (team.player2 != null) {
            json.put("player2", playerToJson(team.player2))
        }
        return json
    }

    private fun teamFromJson(json: JSONObject): Team {
        val side = TeamSide.valueOf(json.optString("side", "TEAM_A"))
        val name = json.optString("name", "Team")
        val p1 = playerFromJson(json.optJSONObject("player1")) ?: Player("1", "Player 1")
        val p2 = playerFromJson(json.optJSONObject("player2"))
        return Team(side = side, name = name, player1 = p1, player2 = p2)
    }

    fun toJson(state: MatchState): String {
        val json = JSONObject()

        val configJson = JSONObject()
        configJson.put("sport", state.config.sport.name)
        configJson.put("format", state.config.format.name)
        configJson.put("winningScore", state.config.winningScore)
        configJson.put("winByTwo", state.config.winByTwo)
        configJson.put("bestOfGames", state.config.bestOfGames)
        configJson.put("teamA", teamToJson(state.config.teamA))
        configJson.put("teamB", teamToJson(state.config.teamB))
        json.put("config", configJson)

        json.put("scoreTeamA", state.scoreTeamA)
        json.put("scoreTeamB", state.scoreTeamB)
        json.put("servingTeam", state.servingTeam.name)
        json.put("servingCourt", state.servingCourt.name)
        json.put("servingPlayer", playerToJson(state.servingPlayer))
        json.put("receivingPlayer", playerToJson(state.receivingPlayer))
        json.put("serverNumber", state.serverNumber)
        json.put("currentGameIndex", state.currentGameIndex)
        json.put("gameWinsTeamA", state.gameWinsTeamA)
        json.put("gameWinsTeamB", state.gameWinsTeamB)
        json.put("isGameFinished", state.isGameFinished)
        json.put("isMatchFinished", state.isMatchFinished)
        if (state.gameWinner != null) json.put("gameWinner", state.gameWinner.name)
        if (state.matchWinner != null) json.put("matchWinner", state.matchWinner.name)
        json.put("isSideSwapped", state.isSideSwapped)
        json.put("canUndo", state.canUndo())
        json.put("canRedo", state.canRedo())

        val teamAJson = JSONObject()
        teamAJson.put("left", playerToJson(state.teamAPositions.leftCourtPlayer))
        if (state.teamAPositions.rightCourtPlayer != null) {
            teamAJson.put("right", playerToJson(state.teamAPositions.rightCourtPlayer))
        }
        json.put("teamAPositions", teamAJson)

        val teamBJson = JSONObject()
        teamBJson.put("left", playerToJson(state.teamBPositions.leftCourtPlayer))
        if (state.teamBPositions.rightCourtPlayer != null) {
            teamBJson.put("right", playerToJson(state.teamBPositions.rightCourtPlayer))
        }
        json.put("teamBPositions", teamBJson)

        return json.toString()
    }

    fun fromJson(jsonStr: String): MatchState? {
        return try {
            val json = JSONObject(jsonStr)
            val configJson = json.getJSONObject("config")

            val sport = Sport.valueOf(configJson.getString("sport"))
            val format = MatchFormat.valueOf(configJson.getString("format"))
            val winningScore = configJson.optInt("winningScore", sport.defaultWinningScore)
            val winByTwo = configJson.optBoolean("winByTwo", true)
            val bestOfGames = configJson.optInt("bestOfGames", 3)

            val teamA = teamFromJson(configJson.getJSONObject("teamA"))
            val teamB = teamFromJson(configJson.getJSONObject("teamB"))

            val config = MatchConfig(
                sport = sport,
                format = format,
                teamA = teamA,
                teamB = teamB,
                winningScore = winningScore,
                winByTwo = winByTwo,
                maxScoreCap = sport.maxScoreCap,
                bestOfGames = bestOfGames
            )

            val servingTeam = TeamSide.valueOf(json.optString("servingTeam", "TEAM_A"))
            val servingCourt = CourtHalf.valueOf(json.optString("servingCourt", CourtHalf.RIGHT.name))
            val serverNumber = json.optInt("serverNumber", 1)

            val servingPlayer = playerFromJson(json.optJSONObject("servingPlayer")) ?: teamA.player1
            val receivingPlayer = playerFromJson(json.optJSONObject("receivingPlayer")) ?: teamB.player1

            val teamAJson = json.optJSONObject("teamAPositions")
            val teamAPos = if (teamAJson != null) {
                TeamCourtPositions(
                    leftCourtPlayer = playerFromJson(teamAJson.optJSONObject("left")) ?: teamA.player1,
                    rightCourtPlayer = playerFromJson(teamAJson.optJSONObject("right"))
                )
            } else {
                TeamCourtPositions(
                    leftCourtPlayer = teamA.player2 ?: teamA.player1,
                    rightCourtPlayer = if (format == MatchFormat.DOUBLES) teamA.player1 else null
                )
            }

            val teamBJson = json.optJSONObject("teamBPositions")
            val teamBPos = if (teamBJson != null) {
                TeamCourtPositions(
                    leftCourtPlayer = playerFromJson(teamBJson.optJSONObject("left")) ?: teamB.player1,
                    rightCourtPlayer = playerFromJson(teamBJson.optJSONObject("right"))
                )
            } else {
                TeamCourtPositions(
                    leftCourtPlayer = teamB.player1,
                    rightCourtPlayer = if (format == MatchFormat.DOUBLES) (teamB.player2 ?: teamB.player1) else null
                )
            }

            MatchState(
                config = config,
                currentGameIndex = json.optInt("currentGameIndex", 0),
                gameWinsTeamA = json.optInt("gameWinsTeamA", 0),
                gameWinsTeamB = json.optInt("gameWinsTeamB", 0),
                scoreTeamA = json.optInt("scoreTeamA", 0),
                scoreTeamB = json.optInt("scoreTeamB", 0),
                servingTeam = servingTeam,
                servingPlayer = servingPlayer,
                receivingPlayer = receivingPlayer,
                serverNumber = serverNumber,
                servingCourt = servingCourt,
                teamAPositions = teamAPos,
                teamBPositions = teamBPos,
                isGameFinished = json.optBoolean("isGameFinished", false),
                isMatchFinished = json.optBoolean("isMatchFinished", false),
                isSideSwapped = json.optBoolean("isSideSwapped", false),
                gameWinner = if (json.has("gameWinner") && !json.isNull("gameWinner")) TeamSide.valueOf(json.getString("gameWinner")) else null,
                matchWinner = if (json.has("matchWinner") && !json.isNull("matchWinner")) TeamSide.valueOf(json.getString("matchWinner")) else null,
                history = if (json.optBoolean("canUndo", false)) listOf(MatchState(config = config, servingPlayer = servingPlayer, receivingPlayer = receivingPlayer, teamAPositions = teamAPos, teamBPositions = teamBPos)) else emptyList(),
                redoStack = if (json.optBoolean("canRedo", false)) listOf(MatchState(config = config, servingPlayer = servingPlayer, receivingPlayer = receivingPlayer, teamAPositions = teamAPos, teamBPositions = teamBPos)) else emptyList()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun actionToJson(action: GameAction): String {
        val json = JSONObject()
        val type = when (action) {
            is GameAction.PointTeamA -> "PointTeamA"
            is GameAction.PointTeamB -> "PointTeamB"
            is GameAction.PointServingTeam -> "PointServingTeam"
            is GameAction.PointReceivingTeam -> "PointReceivingTeam"
            is GameAction.Undo -> "Undo"
            is GameAction.Redo -> "Redo"
            is GameAction.SwitchSidesManual -> "SwitchSidesManual"
            is GameAction.SwitchServerManual -> "SwitchServerManual"
            is GameAction.StartNextGame -> "StartNextGame"
            is GameAction.ResetMatch -> "ResetMatch"
        }
        json.put("actionType", type)
        return json.toString()
    }

    fun actionFromJson(jsonStr: String): GameAction? {
        return try {
            val json = JSONObject(jsonStr)
            when (json.getString("actionType")) {
                "PointTeamA" -> GameAction.PointTeamA
                "PointTeamB" -> GameAction.PointTeamB
                "PointServingTeam" -> GameAction.PointServingTeam
                "PointReceivingTeam" -> GameAction.PointReceivingTeam
                "Undo" -> GameAction.Undo
                "Redo" -> GameAction.Redo
                "SwitchSidesManual" -> GameAction.SwitchSidesManual
                "SwitchServerManual" -> GameAction.SwitchServerManual
                "StartNextGame" -> GameAction.StartNextGame()
                "ResetMatch" -> GameAction.ResetMatch()
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
