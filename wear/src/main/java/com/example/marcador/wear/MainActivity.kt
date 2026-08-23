package com.example.marcador.wear

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var messageClient: MessageClient
    private var connectedNodeId: String? = null

    // Compose State
    private var isMatchStarted by mutableStateOf(false)
    private var scoreA by mutableStateOf("00")
    private var scoreB by mutableStateOf("00")
    private var teamALabel by mutableStateOf("Eq 1")
    private var teamBLabel by mutableStateOf("Eq 2")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messageClient = Wearable.getMessageClient(this)

        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A)), // NightGreen equivalent
                    contentAlignment = Alignment.Center
                ) {
                    if (isMatchStarted) {
                        MatchControls(
                            scoreA = scoreA,
                            scoreB = scoreB,
                            teamALabel = teamALabel,
                            teamBLabel = teamBLabel,
                            onPointA = { sendMessage("/marcador/point/a") },
                            onPointB = { sendMessage("/marcador/point/b") },
                            onUndo = { sendMessage("/marcador/undo") }
                        )
                    } else {
                        WaitingScreen()
                    }
                }
            }
        }
        
        findPhoneNode()
    }

    override fun onResume() {
        super.onResume()
        messageClient.addListener(this)
        sendMessage("/marcador/request_state")
    }

    override fun onPause() {
        super.onPause()
        messageClient.removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/marcador/state") {
            val payload = String(messageEvent.data)
            try {
                val json = JSONObject(payload)
                isMatchStarted = json.getBoolean("started")
                if (isMatchStarted) {
                    scoreA = json.getString("scoreA")
                    scoreB = json.getString("scoreB")
                    teamALabel = json.getString("teamA")
                    teamBLabel = json.getString("teamB")
                }
            } catch (e: Exception) {
                Log.e("Wear", "Error parsing state", e)
            }
        }
    }

    private fun findPhoneNode() {
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            connectedNodeId = nodes.firstOrNull()?.id
        }.addOnFailureListener { e ->
            Log.e("Wear", "Error finding node", e)
        }
    }

    private fun sendMessage(path: String) {
        connectedNodeId?.let { nodeId ->
            messageClient.sendMessage(nodeId, path, ByteArray(0))
        }
    }
}

@Composable
fun WaitingScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Esperando",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Inicia un partido\\nen el móvil",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun MatchControls(
    scoreA: String,
    scoreB: String,
    teamALabel: String,
    teamBLabel: String,
    onPointA: () -> Unit,
    onPointB: () -> Unit,
    onUndo: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Score Display
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = scoreA, color = Color(0xFF10B981), fontSize = 24.sp, fontWeight = FontWeight.Bold) // Green
            Text(text = " - ", color = Color.White, fontSize = 20.sp)
            Text(text = scoreB, color = Color(0xFF3B82F6), fontSize = 24.sp, fontWeight = FontWeight.Bold) // Blue
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onPointA,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)), // CourtGreen
                modifier = Modifier.size(56.dp)
            ) {
                Text("A", fontWeight = FontWeight.Bold)
            }
            
            Button(
                onClick = onPointB,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3B82F6)), // AquaAccent equivalent
                modifier = Modifier.size(56.dp)
            ) {
                Text("B", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = onUndo,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray),
            modifier = Modifier.height(32.dp).width(80.dp)
        ) {
            Text("Deshacer", fontSize = 10.sp)
        }
    }
}
