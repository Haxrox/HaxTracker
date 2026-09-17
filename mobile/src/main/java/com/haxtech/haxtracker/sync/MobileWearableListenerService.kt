package com.haxtech.haxtracker.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.haxtech.haxtracker.core.sync.MatchStateSerializer

class MobileWearableListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                when (event.dataItem.uri.path) {
                    "/action" -> {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val jsonStr = dataMap.getString("action_json") ?: continue
                        val action = MatchStateSerializer.actionFromJson(jsonStr) ?: continue
                        PhoneSyncRepository.dispatchActionFromWatch(action)
                    }
                    "/match_state_watch" -> {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val jsonStr = dataMap.getString("match_state_json") ?: continue
                        val state = MatchStateSerializer.fromJson(jsonStr) ?: continue
                        PhoneSyncRepository.onStateReceivedFromWatch(state)
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            "/action" -> {
                val jsonStr = String(messageEvent.data, Charsets.UTF_8)
                val action = MatchStateSerializer.actionFromJson(jsonStr) ?: return
                PhoneSyncRepository.dispatchActionFromWatch(action)
            }
            "/match_state_watch" -> {
                val jsonStr = String(messageEvent.data, Charsets.UTF_8)
                val state = MatchStateSerializer.fromJson(jsonStr) ?: return
                PhoneSyncRepository.onStateReceivedFromWatch(state)
            }
        }
    }
}
