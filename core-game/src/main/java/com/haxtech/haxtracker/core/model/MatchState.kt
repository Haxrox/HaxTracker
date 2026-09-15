package com.haxtech.haxtracker.core.model

data class MatchConfig(
    val sport: Sport,
    val format: MatchFormat,
    val teamA: Team,
    val teamB: Team,
    val winningScore: Int = sport.defaultWinningScore,
    val winByTwo: Boolean = true,
    val maxScoreCap: Int? = sport.maxScoreCap,
    val bestOfGames: Int = 3
) {
    companion object {
        fun defaultBadmintonDoubles(
            teamAName: String = "Team A",
            teamBName: String = "Team B"
        ): MatchConfig {
            val a1 = Player("a1", "Player A1", "A1")
            val a2 = Player("a2", "Player A2", "A2")
            val b1 = Player("b1", "Player B1", "B1")
            val b2 = Player("b2", "Player B2", "B2")
            return MatchConfig(
                sport = Sport.BADMINTON,
                format = MatchFormat.DOUBLES,
                teamA = Team(TeamSide.TEAM_A, teamAName, a1, a2),
                teamB = Team(TeamSide.TEAM_B, teamBName, b1, b2),
                winningScore = 21,
                winByTwo = true,
                maxScoreCap = 30,
                bestOfGames = 3
            )
        }

        fun defaultPickleballDoubles(
            teamAName: String = "Team A",
            teamBName: String = "Team B"
        ): MatchConfig {
            val a1 = Player("a1", "Player A1", "A1")
            val a2 = Player("a2", "Player A2", "A2")
            val b1 = Player("b1", "Player B1", "B1")
            val b2 = Player("b2", "Player B2", "B2")
            return MatchConfig(
                sport = Sport.PICKLEBALL,
                format = MatchFormat.DOUBLES,
                teamA = Team(TeamSide.TEAM_A, teamAName, a1, a2),
                teamB = Team(TeamSide.TEAM_B, teamBName, b1, b2),
                winningScore = 11,
                winByTwo = true,
                maxScoreCap = null,
                bestOfGames = 3
            )
        }
        fun defaultBadmintonSingles(
            playerAName: String = "Player A",
            playerBName: String = "Player B"
        ): MatchConfig {
            val a1 = Player("a1", playerAName, "A1")
            val b1 = Player("b1", playerBName, "B1")
            return MatchConfig(
                sport = Sport.BADMINTON,
                format = MatchFormat.SINGLES,
                teamA = Team(TeamSide.TEAM_A, playerAName, a1),
                teamB = Team(TeamSide.TEAM_B, playerBName, b1),
                winningScore = 21,
                winByTwo = true,
                maxScoreCap = 30,
                bestOfGames = 3
            )
        }

        fun defaultPickleballSingles(
            playerAName: String = "Player A",
            playerBName: String = "Player B"
        ): MatchConfig {
            val a1 = Player("a1", playerAName, "A1")
            val b1 = Player("b1", playerBName, "B1")
            return MatchConfig(
                sport = Sport.PICKLEBALL,
                format = MatchFormat.SINGLES,
                teamA = Team(TeamSide.TEAM_A, playerAName, a1),
                teamB = Team(TeamSide.TEAM_B, playerBName, b1),
                winningScore = 11,
                winByTwo = true,
                maxScoreCap = null,
                bestOfGames = 3
            )
        }
    }
}

data class MatchState(
    val config: MatchConfig,
    val currentGameIndex: Int = 0,
    val gameWinsTeamA: Int = 0,
    val gameWinsTeamB: Int = 0,
    val scoreTeamA: Int = 0,
    val scoreTeamB: Int = 0,
    val servingTeam: TeamSide = TeamSide.TEAM_A,
    val servingPlayer: Player,
    val receivingPlayer: Player,
    val serverNumber: Int = 1, // 1 or 2 in Pickleball doubles
    val servingCourt: CourtHalf = CourtHalf.RIGHT,
    val teamAPositions: TeamCourtPositions,
    val teamBPositions: TeamCourtPositions,
    val isGameFinished: Boolean = false,
    val isMatchFinished: Boolean = false,
    val isSideSwapped: Boolean = false, // True if Team B is at the bottom (near side)
    val gameWinner: TeamSide? = null,
    val matchWinner: TeamSide? = null,
    val rallyStartTimeMs: Long = 0L,
    val rallyDurationsSec: List<Int> = emptyList(),
    val pastGameScores: List<Pair<Int, Int>> = emptyList(),
    val history: List<MatchState> = emptyList(),
    val redoStack: List<MatchState> = emptyList()
) {
    val serverTeamScore: Int
        get() = if (servingTeam == TeamSide.TEAM_A) scoreTeamA else scoreTeamB

    val receiverTeamScore: Int
        get() = if (servingTeam == TeamSide.TEAM_A) scoreTeamB else scoreTeamA

    val receivingCourt: CourtHalf
        get() = servingCourt // Diagonal court: right serves to right half, left serves to left half

    val calloutScore: String
        get() {
            return if (config.sport == Sport.PICKLEBALL && config.format == MatchFormat.DOUBLES) {
                "$serverTeamScore - $receiverTeamScore - $serverNumber"
            } else {
                "$serverTeamScore - $receiverTeamScore"
            }
        }

    val spokenAnnouncement: String
        get() {
            if (isMatchFinished) {
                val winnerName = if (matchWinner == TeamSide.TEAM_A) config.teamA.name else config.teamB.name
                return "Match finished. $winnerName wins!"
            }
            if (isGameFinished) {
                val winnerName = if (gameWinner == TeamSide.TEAM_A) config.teamA.name else config.teamB.name
                return "Game over. $winnerName wins game ${currentGameIndex + 1}."
            }

            val base = if (config.sport == Sport.PICKLEBALL && config.format == MatchFormat.DOUBLES) {
                "$serverTeamScore, $receiverTeamScore, server $serverNumber"
            } else {
                "$serverTeamScore, $receiverTeamScore"
            }

            val prefix = if (isMatchPoint) "Match point! " else if (isGamePoint) "Game point! " else ""
            return "$prefix$base"
        }

    val isGamePoint: Boolean
        get() {
            if (isGameFinished || isMatchFinished) return false
            val target = config.winningScore
            val canAWin = scoreTeamA >= target - 1 && (!config.winByTwo || scoreTeamA >= scoreTeamB + 1)
            val canBWin = scoreTeamB >= target - 1 && (!config.winByTwo || scoreTeamB >= scoreTeamA + 1)
            return canAWin || canBWin
        }

    val isMatchPoint: Boolean
        get() {
            if (!isGamePoint) return false
            val gamesNeeded = (config.bestOfGames / 2) + 1
            val canAWinMatch = gameWinsTeamA == gamesNeeded - 1 && scoreTeamA >= scoreTeamB
            val canBWinMatch = gameWinsTeamB == gamesNeeded - 1 && scoreTeamB >= scoreTeamA
            return canAWinMatch || canBWinMatch
        }

    fun canUndo(): Boolean = history.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()
}
