package com.haxtech.haxtracker.core.model

enum class Sport(val displayName: String, val defaultWinningScore: Int, val maxScoreCap: Int?) {
    BADMINTON(
        displayName = "Badminton",
        defaultWinningScore = 21,
        maxScoreCap = 30 // BWF rule: sudden death at 29-29, capped at 30
    ),
    PICKLEBALL(
        displayName = "Pickleball",
        defaultWinningScore = 11,
        maxScoreCap = null // Win by 2, no upper cap
    )
}

enum class MatchFormat {
    SINGLES,
    DOUBLES
}

enum class TeamSide {
    TEAM_A,
    TEAM_B;

    fun other(): TeamSide = if (this == TEAM_A) TEAM_B else TEAM_A
}

enum class CourtHalf {
    LEFT,
    RIGHT;

    fun opposite(): CourtHalf = if (this == LEFT) RIGHT else LEFT
}
