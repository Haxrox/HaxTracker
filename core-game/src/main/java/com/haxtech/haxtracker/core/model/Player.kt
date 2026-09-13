package com.haxtech.haxtracker.core.model

data class Player(
    val id: String,
    val name: String,
    val shortName: String = if (name.length <= 3) name.uppercase() else name.take(2).uppercase()
) {
    companion object {
        fun default(id: String, name: String) = Player(id = id, name = name)
    }
}

data class Team(
    val side: TeamSide,
    val name: String,
    val player1: Player,
    val player2: Player? = null
) {
    val isDoubles: Boolean get() = player2 != null

    fun players(): List<Player> = listOfNotNull(player1, player2)
}

data class TeamCourtPositions(
    val leftCourtPlayer: Player,
    val rightCourtPlayer: Player? = null
) {
    fun swapped(): TeamCourtPositions {
        return if (rightCourtPlayer != null) {
            TeamCourtPositions(
                leftCourtPlayer = rightCourtPlayer,
                rightCourtPlayer = leftCourtPlayer
            )
        } else {
            this
        }
    }

    fun playerAt(half: CourtHalf): Player {
        return when (half) {
            CourtHalf.LEFT -> leftCourtPlayer
            CourtHalf.RIGHT -> rightCourtPlayer ?: leftCourtPlayer
        }
    }

    fun courtOf(player: Player): CourtHalf {
        return if (rightCourtPlayer?.id == player.id) CourtHalf.RIGHT else CourtHalf.LEFT
    }
}
