package com.haxtech.haxtracker.sync

import android.content.Context
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

object PhoneSyncRepository {

    private val _matchState = MutableStateFlow(GameEngine.newMatch(MatchConfig.defaultPickleballDoubles()))
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private val _isMatchActive = MutableStateFlow(false)
    val isMatchActive: StateFlow<Boolean> = _isMatchActive.asStateFlow()

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun startMatch(config: MatchConfig) {
        val fresh = GameEngine.newMatch(config)
        _matchState.value = fresh
        _isMatchActive.value = true
        syncStateToWatch(fresh)
    }

    fun closeMatch() {
        _isMatchActive.value = false
    }

    fun dispatchAction(action: GameAction) {
        val prev = _matchState.value
        val updated = GameEngine.process(prev, action)
        _matchState.value = updated
        _isMatchActive.value = true
        syncStateToWatch(updated)
    }

    fun dispatchActionFromWatch(action: GameAction) {
        if (action is GameAction.Undo || action is GameAction.Redo) {
            // Watch handles history locally and syncs the full state.
            return
        }
        dispatchAction(action)
    }

    fun onStateReceivedFromWatch(state: MatchState) {
        _matchState.value = state
        _isMatchActive.value = true
        syncStateToWatch(state)
    }

    fun syncStateToWatch(state: MatchState) {
        val context = appContext ?: return
        val jsonStr = MatchStateSerializer.toJson(state)
        val bytes = jsonStr.toByteArray(Charsets.UTF_8)

        // 1. DataClient
        try {
            val putDataMapReq = PutDataMapRequest.create("/match_state")
            putDataMapReq.dataMap.putString("match_state_json", jsonStr)
            putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis())
            val req = putDataMapReq.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(req)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. MessageClient
        try {
            val messageClient = Wearable.getMessageClient(context)
            val nodeClient = Wearable.getNodeClient(context)

            nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/match_state", bytes)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
