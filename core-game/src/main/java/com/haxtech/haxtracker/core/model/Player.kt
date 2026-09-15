package com.haxtech.haxtracker.core.model

data class Player(
    val id: String,
    val name: String,
    val shortName: String = generateShortName(name)
) {
    companion object {
        fun default(id: String, name: String) = Player(id = id, name = name)

        private fun generateShortName(name: String): String {
            val trimmed = name.trim()
            if (trimmed.length <= 3) return trimmed.uppercase()
            val parts = trimmed.split("\\s+".toRegex())
            if (parts.size >= 2) {
                val lastPart = parts.last().uppercase()
                if (lastPart.matches(Regex("^[A-Z][0-9]+$"))) {
                    return lastPart
                }
                if (lastPart.matches(Regex("^[0-9]+$"))) {
                    val prevPart = parts[parts.size - 2].uppercase()
                    if (prevPart.length == 1) {
                        return "$prevPart$lastPart"
                    }
                }
                val teamMatch = Regex("(?i)\\b([A-Z])\\s*([0-9])\\b").find(trimmed)
                if (teamMatch != null) {
                    return "${teamMatch.groupValues[1].uppercase()}${teamMatch.groupValues[2]}"
                }
                if (parts[0].equals("Player", ignoreCase = true) || parts[0].equals("Team", ignoreCase = true)) {
                    val candidate = parts.drop(1).filter { !it.equals("Player", ignoreCase = true) && !it.equals("Team", ignoreCase = true) }
                        .joinToString("") { it.take(1) }.uppercase()
                    if (candidate.isNotEmpty()) return candidate
                }
                return "${parts.first().first()}${parts.last().first()}".uppercase()
            }
            return trimmed.take(2).uppercase()
        }
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
