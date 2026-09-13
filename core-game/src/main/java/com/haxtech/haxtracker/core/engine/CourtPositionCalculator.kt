package com.haxtech.haxtracker.core.engine

import com.haxtech.haxtracker.core.model.*

enum class CourtSlot {
    TOP_LEFT,     // Screen Top Left
    TOP_RIGHT,    // Screen Top Right
    BOTTOM_LEFT,  // Screen Bottom Left
    BOTTOM_RIGHT  // Screen Bottom Right
}

data class CourtRenderModel(
    val topLeftPlayer: Player?,
    val topRightPlayer: Player?,
    val bottomLeftPlayer: Player?,
    val bottomRightPlayer: Player?,
    val activeServerSlot: CourtSlot,
    val activeReceiverSlot: CourtSlot,
    val servingTeam: TeamSide,
    val isDoubles: Boolean
)

object CourtPositionCalculator {

    fun calculate(state: MatchState): CourtRenderModel {
        val isDoubles = state.config.format == MatchFormat.DOUBLES
        val nearTeam = if (state.isSideSwapped) TeamSide.TEAM_B else TeamSide.TEAM_A
        val farTeam = nearTeam.other()

        // VISUAL RULES (User Priority):
        // 1. Perspective is relative to the player facing the net.
        // 2. Near Team (Bottom): Right is Screen-Right. Even = BOTTOM_RIGHT.
        // 3. Far Team (Top): Right is Screen-Left. Even = TOP_LEFT.
        // 4. Service is ALWAYS diagonal from the perspective of the server.
        
        val serverSlot: CourtSlot
        val receiverSlot: CourtSlot

        if (state.servingTeam == nearTeam) {
            // Server is at bottom (Near side)
            // Even (RIGHT) -> BOTTOM_RIGHT
            serverSlot = if (state.servingCourt == CourtHalf.RIGHT) CourtSlot.BOTTOM_RIGHT else CourtSlot.BOTTOM_LEFT
            // Receiver must also be in their RIGHT court (TOP_LEFT) for a diagonal serve
            receiverSlot = if (state.servingCourt == CourtHalf.RIGHT) CourtSlot.TOP_LEFT else CourtSlot.TOP_RIGHT
        } else {
            // Server is at top (Far side)
            // Even (RIGHT) -> TOP_LEFT (User said: "Right = left side of the screen")
            serverSlot = if (state.servingCourt == CourtHalf.RIGHT) CourtSlot.TOP_LEFT else CourtSlot.TOP_RIGHT
            // Diagonal receiver at bottom: TOP_LEFT serves to BOTTOM_RIGHT
            receiverSlot = if (state.servingCourt == CourtHalf.RIGHT) CourtSlot.BOTTOM_RIGHT else CourtSlot.BOTTOM_LEFT
        }

        var pTopLeft: Player? = null
        var pTopRight: Player? = null
        var pBottomLeft: Player? = null
        var pBottomRight: Player? = null

        if (isDoubles) {
            val nearPos = if (nearTeam == TeamSide.TEAM_A) state.teamAPositions else state.teamBPositions
            val farPos = if (farTeam == TeamSide.TEAM_A) state.teamAPositions else state.teamBPositions

            // Near team mapping (Bottom)
            pBottomLeft = nearPos.leftCourtPlayer
            pBottomRight = nearPos.rightCourtPlayer

            // Far team mapping (Top) - Inverted logically but mapped to screen slots
            // Their "Right" court player goes to TOP_LEFT to keep perspective accurate.
            pTopLeft = farPos.rightCourtPlayer
            pTopRight = farPos.leftCourtPlayer
        } else {
            // Singles: Only place players in their current active slots
            if (state.servingTeam == nearTeam) {
                if (serverSlot == CourtSlot.BOTTOM_RIGHT) pBottomRight = state.servingPlayer else pBottomLeft = state.servingPlayer
                if (receiverSlot == CourtSlot.TOP_LEFT) pTopLeft = state.receivingPlayer else pTopRight = state.receivingPlayer
            } else {
                if (serverSlot == CourtSlot.TOP_LEFT) pTopLeft = state.servingPlayer else pTopRight = state.servingPlayer
                if (receiverSlot == CourtSlot.BOTTOM_RIGHT) pBottomRight = state.receivingPlayer else pBottomLeft = state.receivingPlayer
            }
        }

        return CourtRenderModel(
            topLeftPlayer = pTopLeft,
            topRightPlayer = pTopRight,
            bottomLeftPlayer = pBottomLeft,
            bottomRightPlayer = pBottomRight,
            activeServerSlot = serverSlot,
            activeReceiverSlot = receiverSlot,
            servingTeam = state.servingTeam,
            isDoubles = isDoubles
        )
    }
}
