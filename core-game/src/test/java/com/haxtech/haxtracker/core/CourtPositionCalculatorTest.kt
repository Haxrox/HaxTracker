package com.haxtech.haxtracker.core

import com.haxtech.haxtracker.core.engine.CourtPositionCalculator
import com.haxtech.haxtracker.core.engine.CourtSlot
import com.haxtech.haxtracker.core.engine.GameEngine
import com.haxtech.haxtracker.core.model.*
import org.junit.Assert.*
import org.junit.Test

class CourtPositionCalculatorTest {

    @Test
    fun testBadmintonDoublesCourtPositions() {
        val config = MatchConfig.defaultBadmintonDoubles("Team A", "Team B")
        val state = GameEngine.newMatch(config)
        val renderModel = CourtPositionCalculator.calculate(state)

        assertNotNull(renderModel.bottomLeftPlayer)
        assertNotNull(renderModel.bottomRightPlayer)
        assertNotNull(renderModel.topLeftPlayer)
        assertNotNull(renderModel.topRightPlayer)

        if (state.servingTeam == TeamSide.TEAM_A) {
            assertEquals(CourtSlot.BOTTOM_RIGHT, renderModel.activeServerSlot)
            assertEquals(CourtSlot.TOP_LEFT, renderModel.activeReceiverSlot)
        } else {
            assertEquals(CourtSlot.TOP_LEFT, renderModel.activeServerSlot)
            assertEquals(CourtSlot.BOTTOM_RIGHT, renderModel.activeReceiverSlot)
        }
    }

    @Test
    fun testSinglesCourtPositions() {
        val config = MatchConfig.defaultBadmintonSingles("Alice", "Bob")
        val state = GameEngine.newMatch(config)
        val renderModel = CourtPositionCalculator.calculate(state)

        if (state.servingTeam == TeamSide.TEAM_A) {
            assertNotNull(renderModel.bottomRightPlayer)
            assertNull(renderModel.bottomLeftPlayer)
            assertNotNull(renderModel.topLeftPlayer)
            assertNull(renderModel.topRightPlayer)
        } else {
            assertNotNull(renderModel.topLeftPlayer)
            assertNull(renderModel.topRightPlayer)
            assertNotNull(renderModel.bottomRightPlayer)
            assertNull(renderModel.bottomLeftPlayer)
        }
    }

    @Test
    fun testSwappedSideCourtPositions() {
        val config = MatchConfig.defaultBadmintonDoubles("Team A", "Team B")
        var state = GameEngine.newMatch(config)
        state = state.copy(isSideSwapped = true)

        val renderModel = CourtPositionCalculator.calculate(state)

        if (state.servingTeam == TeamSide.TEAM_A) {
            // Team A is on the top court (Far side: Even = TOP_LEFT)
            assertEquals(CourtSlot.TOP_LEFT, renderModel.activeServerSlot)
            assertEquals(CourtSlot.BOTTOM_RIGHT, renderModel.activeReceiverSlot)
        } else {
            // Team B is on the bottom court (Near side: Even = BOTTOM_RIGHT)
            assertEquals(CourtSlot.BOTTOM_RIGHT, renderModel.activeServerSlot)
            assertEquals(CourtSlot.TOP_LEFT, renderModel.activeReceiverSlot)
        }
    }
}
