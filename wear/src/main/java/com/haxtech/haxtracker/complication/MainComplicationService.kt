package com.haxtech.haxtracker.wear.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.haxtech.haxtracker.wear.sync.WatchSyncRepository

/**
 * Complication data source that returns the current match score.
 */
class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) {
            return null
        }
        return createComplicationData("0:0", "Score 0 to 0")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val state = WatchSyncRepository.matchState.value
        val scoreText = "${state.scoreTeamA}:${state.scoreTeamB}"
        val desc = "${state.config.sport.displayName} Score: $scoreText"
        return createComplicationData(scoreText, desc)
    }

    private fun createComplicationData(text: String, contentDescription: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        ).build()
}