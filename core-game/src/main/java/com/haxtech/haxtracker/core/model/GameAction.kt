package com.haxtech.haxtracker.core.model

sealed interface GameAction {
    data object PointTeamA : GameAction
    data object PointTeamB : GameAction
    data object PointServingTeam : GameAction
    data object PointReceivingTeam : GameAction
    data object Undo : GameAction
    data object SwitchSidesManual : GameAction
    data object SwitchServerManual : GameAction
    data class StartNextGame(val timestampMs: Long = System.currentTimeMillis()) : GameAction
    data class ResetMatch(val config: MatchConfig? = null) : GameAction
}
