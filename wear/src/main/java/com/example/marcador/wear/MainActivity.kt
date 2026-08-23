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
    private var playerTL by mutableStateOf("Jugador 1")
    private var playerTR by mutableStateOf("Jugador 2")
    private var playerBL by mutableStateOf("Jugador 3")
    private var playerBR by mutableStateOf("Jugador 4")

    // Ephemeral State
    private var detailPromptTeam by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messageClient = Wearable.getMessageClient(this)

        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isMatchStarted) {
                        if (detailPromptTeam != null) {
                            DetailPromptScreen(
                                team = detailPromptTeam!!,
                                playerTL = playerTL,
                                playerTR = playerTR,
                                playerBL = playerBL,
                                playerBR = playerBR,
                                onEnrich = { type, actor ->
                                    sendMessage("/marcador/enrich?type=$type&actor=$actor")
                                    detailPromptTeam = null
                                }
                            )
                        } else {
                            MatchControls(
                                scoreA = scoreA,
                                scoreB = scoreB,
                                teamALabel = teamALabel,
                                teamBLabel = teamBLabel,
                                onPointA = { 
                                    sendMessage("/marcador/point/a")
                                    detailPromptTeam = "A"
                                },
                                onPointB = { 
                                    sendMessage("/marcador/point/b")
                                    detailPromptTeam = "B"
                                },
                                onUndo = { sendMessage("/marcador/undo") }
                            )
                        }
                    } else {
                        WaitingScreen()
                    }
                }
                
                LaunchedEffect(detailPromptTeam) {
                    if (detailPromptTeam != null) {
                        kotlinx.coroutines.delay(4000)
                        detailPromptTeam = null
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
                    if (json.has("playerTL")) playerTL = json.getString("playerTL")
                    if (json.has("playerTR")) playerTR = json.getString("playerTR")
                    if (json.has("playerBL")) playerBL = json.getString("playerBL")
                    if (json.has("playerBR")) playerBR = json.getString("playerBR")
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

@Composable
fun DetailPromptScreen(
    team: String,
    playerTL: String,
    playerTR: String,
    playerBL: String,
    playerBR: String,
    onEnrich: (String, String) -> Unit
) {
    // If team == A, players are Top (TL, TR). If B, Bottom (BL, BR)
    // Wait, in our domain: 
    // TOP_LEFT / TOP_RIGHT = Team A
    // BOTTOM_LEFT / BOTTOM_RIGHT = Team B
    
    val (myDrivePos, myRevesPos, myDriveName, myRevesName) = if (team == "A") {
        listOf("TOP_RIGHT", "TOP_LEFT", playerTR, playerTL)
    } else {
        listOf("BOTTOM_RIGHT", "BOTTOM_LEFT", playerBR, playerBL)
    }
    
    val (oppDrivePos, oppRevesPos, oppDriveName, oppRevesName) = if (team == "A") {
        listOf("BOTTOM_RIGHT", "BOTTOM_LEFT", playerBR, playerBL)
    } else {
        listOf("TOP_RIGHT", "TOP_LEFT", playerTR, playerTL)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("¿Cómo ha sido?", color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        
        // Winners
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
                onClick = { onEnrich("WINNER", myRevesPos) },
                modifier = Modifier.weight(1f).height(40.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981))
            ) { Text("W: ${myRevesName.take(5)}", fontSize = 9.sp) }
            Button(
                onClick = { onEnrich("WINNER", myDrivePos) },
                modifier = Modifier.weight(1f).height(40.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981))
            ) { Text("W: ${myDriveName.take(5)}", fontSize = 9.sp) }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Errors (Rivals)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(
                onClick = { onEnrich("RALLY_ERROR", oppRevesPos) },
                modifier = Modifier.weight(1f).height(40.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEF4444)) // Red
            ) { Text("F: ${oppRevesName.take(5)}", fontSize = 9.sp) }
            Button(
                onClick = { onEnrich("RALLY_ERROR", oppDrivePos) },
                modifier = Modifier.weight(1f).height(40.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFEF4444)) // Red
            ) { Text("F: ${oppDriveName.take(5)}", fontSize = 9.sp) }
        }
    }
}
