package com.haxtech.haxtracker.wear.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.haxtech.haxtracker.core.sync.MatchStateSerializer

class WatchWearableListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/match_state") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val jsonStr = dataMap.getString("match_state_json") ?: continue
                val state = MatchStateSerializer.fromJson(jsonStr) ?: continue
                WatchSyncRepository.onStateReceivedFromPhone(state)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/match_state") {
            val jsonStr = String(messageEvent.data, Charsets.UTF_8)
            val state = MatchStateSerializer.fromJson(jsonStr) ?: return
            WatchSyncRepository.onStateReceivedFromPhone(state)
        }
    }
}
