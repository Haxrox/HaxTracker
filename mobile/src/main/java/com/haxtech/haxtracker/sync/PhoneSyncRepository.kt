package com.haxtech.haxtracker.sync

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.haxtech.haxtracker.core.engine.GameEngine
import com.haxtech.haxtracker.core.model.GameAction
import com.haxtech.haxtracker.core.model.MatchConfig
import com.haxtech.haxtracker.core.model.MatchState
import com.haxtech.haxtracker.core.sync.MatchStateSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object PhoneSyncRepository {

    private val _matchState = MutableStateFlow(GameEngine.newMatch(MatchConfig.defaultPickleballDoubles()))
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private val _isMatchActive = MutableStateFlow(false)
    val isMatchActive: StateFlow<Boolean> = _isMatchActive.asStateFlow()

    private var appContext: Context? = null

    private val syncLock = Any()
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastSyncTime = 0L
    private var pendingState: MatchState? = null
    private var pendingSyncJob: Job? = null

    internal const val SYNC_THROTTLE_MS = 100L // Throttle to 10 updates per second max
    internal var clock: () -> Long = { System.currentTimeMillis() }
    internal var onSyncDispatched: ((MatchState, Long) -> Unit)? = null

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
        dispatchAction(action)
    }

    fun onStateReceivedFromWatch(state: MatchState) {
        _matchState.value = state
        _isMatchActive.value = true
        syncStateToWatch(state)
    }

    internal fun resetThrottleForTest() {
        synchronized(syncLock) {
            pendingSyncJob?.cancel()
            pendingSyncJob = null
            pendingState = null
            lastSyncTime = 0L
        }
        clock = { System.currentTimeMillis() }
        onSyncDispatched = null
    }

    fun syncStateToWatch(state: MatchState) {
        val context = appContext
        val testDispatcher = onSyncDispatched
        if (context == null && testDispatcher == null) return

        val now = clock()
        var dispatchNow = false
        var delayMs = 0L

        synchronized(syncLock) {
            val elapsed = now - lastSyncTime
            if (lastSyncTime == 0L || elapsed >= SYNC_THROTTLE_MS) {
                pendingSyncJob?.cancel()
                pendingSyncJob = null
                pendingState = null
                lastSyncTime = now
                dispatchNow = true
            } else {
                // Coalesce rapid updates and schedule trailing-edge sync so the latest state is never dropped.
                pendingState = state
                delayMs = (SYNC_THROTTLE_MS - elapsed).coerceAtLeast(1L)
                if (pendingSyncJob?.isActive != true) {
                    pendingSyncJob = syncScope.launch {
                        delay(delayMs)
                        flushPendingSync()
                    }
                }
            }
        }

        if (dispatchNow) {
            dispatchSync(context, state, now)
        }
    }

    private fun flushPendingSync() {
        val stateToSend: MatchState
        val now = clock()
        synchronized(syncLock) {
            stateToSend = pendingState ?: return
            pendingState = null
            pendingSyncJob = null
            lastSyncTime = now
        }
        dispatchSync(appContext, stateToSend, now)
    }

    private fun dispatchSync(context: Context?, state: MatchState, timestampMs: Long) {
        val testDispatcher = onSyncDispatched
        if (testDispatcher != null) {
            testDispatcher(state, timestampMs)
            return
        }
        if (context == null) return

        val jsonStr = MatchStateSerializer.toJson(state)

        // 1. DataClient (standard priority without setUrgent() to preserve Wear OS battery)
        try {
            val putDataMapReq = PutDataMapRequest.create("/match_state")
            putDataMapReq.dataMap.putString("match_state_json", jsonStr)
            putDataMapReq.dataMap.putLong("timestamp", timestampMs)
            val req = putDataMapReq.asPutDataRequest()
            Wearable.getDataClient(context).putDataItem(req)
        } catch (_: Exception) {
        }

        // 2. MessageClient
        try {
            val messageClient = Wearable.getMessageClient(context)
            val nodeClient = Wearable.getNodeClient(context)

            nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/match_state", jsonStr.toByteArray(Charsets.UTF_8))
                }
            }
        } catch (_: Exception) {
        }
    }
}
