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
    private val onUndo: () -> Unit,
    private val onEnrichPoint: (String, String) -> Unit = { _, _ -> }
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
        when {
            path == "/marcador/point/a" -> onPointA()
            path == "/marcador/point/b" -> onPointB()
            path == "/marcador/undo" -> onUndo()
            path == "/marcador/request_state" -> broadcastState()
            path.startsWith("/marcador/enrich") -> {
                // e.g. /marcador/enrich?type=WINNER&actor=TOP_LEFT
                val parts = path.split("?")
                if (parts.size > 1) {
                    val query = parts[1]
                    val params = query.split("&").associate { 
                        val kv = it.split("=")
                        if (kv.size == 2) kv[0] to kv[1] else "" to ""
                    }
                    val type = params["type"]
                    val actor = params["actor"]
                    if (type != null && actor != null) {
                        onEnrichPoint(type, actor)
                    }
                }
            }
        }
    }

    fun updateMatchState(
        started: Boolean,
        scoreA: String = "00",
        scoreB: String = "00",
        teamA: String = "Eq 1",
        teamB: String = "Eq 2",
        playerTL: String = "Jugador 1",
        playerTR: String = "Jugador 2",
        playerBL: String = "Jugador 3",
        playerBR: String = "Jugador 4"
    ) {
        isMatchStarted = started
        val json = JSONObject().apply {
            put("started", started)
            put("scoreA", scoreA)
            put("scoreB", scoreB)
            put("teamA", teamA)
            put("teamB", teamB)
            put("playerTL", playerTL)
            put("playerTR", playerTR)
            put("playerBL", playerBL)
            put("playerBR", playerBR)
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
