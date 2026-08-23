package com.example.marcador

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

class WearableManager(
    context: Context,
    private val onPointA: () -> Unit,
    private val onPointB: () -> Unit,
    private val onUndo: () -> Unit
) : MessageClient.OnMessageReceivedListener {

    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private var isMatchStarted = false
    private var lastStateJson = ""

    fun initialize() {
        messageClient.addListener(this)
    }

    fun cleanup() {
        messageClient.removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        when (path) {
            "/marcador/point/a" -> onPointA()
            "/marcador/point/b" -> onPointB()
            "/marcador/undo" -> onUndo()
            "/marcador/request_state" -> broadcastState()
        }
    }

    fun updateMatchState(
        started: Boolean,
        scoreA: String = "00",
        scoreB: String = "00",
        teamA: String = "Eq 1",
        teamB: String = "Eq 2"
    ) {
        isMatchStarted = started
        val json = JSONObject().apply {
            put("started", started)
            put("scoreA", scoreA)
            put("scoreB", scoreB)
            put("teamA", teamA)
            put("teamB", teamB)
        }.toString()
        lastStateJson = json
        broadcastState()
    }

    private fun broadcastState() {
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, "/marcador/state", lastStateJson.toByteArray())
            }
        }.addOnFailureListener { e ->
            Log.e("WearableManager", "Failed to get nodes", e)
        }
    }
}
