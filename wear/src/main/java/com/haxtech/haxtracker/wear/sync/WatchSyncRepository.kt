package com.haxtech.haxtracker.wear.sync

import android.content.Context
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.haxtech.haxtracker.core.engine.GameEngine
import com.haxtech.haxtracker.core.model.GameAction
import com.haxtech.haxtracker.core.model.MatchConfig
import com.haxtech.haxtracker.core.model.MatchState
import com.haxtech.haxtracker.core.sync.MatchStateSerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WatchSyncRepository {

    private val _matchState = MutableStateFlow(GameEngine.newMatch(MatchConfig.defaultBadmintonDoubles()))
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private val _isConnectedToPhone = MutableStateFlow(false)
    val isConnectedToPhone: StateFlow<Boolean> = _isConnectedToPhone.asStateFlow()

    fun checkInitialSyncState(context: Context) {
        val nodeClient = Wearable.getNodeClient(context)
        val dataClient = Wearable.getDataClient(context)

        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isNotEmpty()) {
                _isConnectedToPhone.value = true
            }
        }

        dataClient.dataItems.addOnSuccessListener { dataBuffer ->
            try {
                for (item in dataBuffer) {
                    if (item.uri.path == "/match_state") {
                        val dataMap = DataMapItem.fromDataItem(item).dataMap
                        val jsonStr = dataMap.getString("match_state_json") ?: continue
                        val state = MatchStateSerializer.fromJson(jsonStr) ?: continue
                        _matchState.value = state
                        _isConnectedToPhone.value = true
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                dataBuffer.release()
            }
        }
    }

    fun onStateReceivedFromPhone(state: MatchState) {
        _matchState.value = state
        _isConnectedToPhone.value = true
    }

    fun sendActionToPhone(context: Context, action: GameAction, fallbackLocalProcess: (GameAction) -> Unit) {
        val jsonStr = MatchStateSerializer.actionToJson(action)
        val bytes = jsonStr.toByteArray(Charsets.UTF_8)

        // 1. DataClient
        try {
            val putDataMapReq = PutDataMapRequest.create("/action")
            putDataMapReq.dataMap.putString("action_json", jsonStr)
            putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis())
            val req = putDataMapReq.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(req)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. MessageClient
        try {
            val nodeClient = Wearable.getNodeClient(context)
            val messageClient = Wearable.getMessageClient(context)

            nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                if (nodes.isNotEmpty()) {
                    _isConnectedToPhone.value = true
                    for (node in nodes) {
                        messageClient.sendMessage(node.id, "/action", bytes)
                    }
                } else {
                    fallbackLocalProcess(action)
                }
            }.addOnFailureListener {
                fallbackLocalProcess(action)
            }
        } catch (e: Exception) {
            fallbackLocalProcess(action)
        }
    }

    fun updateLocalState(newState: MatchState) {
        _matchState.value = newState
    }
}
