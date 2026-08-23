package com.example.marcador

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.marcador.ui.theme.AquaAccent
import com.example.marcador.ui.theme.CourtGreen
import com.example.marcador.ui.theme.MarcadorTheme
import com.example.marcador.ui.theme.NightGreen
import com.example.marcador.ui.theme.SoftIce

@Composable
fun PadelMarkerApp(
    connectionState: MutableState<BluetoothState>,
    onBluetoothModeSelected: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSendScore: (String) -> Unit
) {
    val context = LocalContext.current
    val historyRepository = remember { MatchHistoryRepository(context.applicationContext) }
    var appMode by remember { mutableStateOf<AppMode?>(null) }
    var scoreboard by remember { mutableStateOf(ScoreboardState()) }
    var localSetup by remember { mutableStateOf(MatchSetup()) }
    var setupError by remember { mutableStateOf<String?>(null) }
    var viewingHistory by remember { mutableStateOf(false) }
    var localMatchStarted by remember { mutableStateOf(false) }
    var savedMatches by remember { mutableStateOf(historyRepository.loadMatches()) }
    var continuedMatchMillis by remember { mutableStateOf<Long?>(null) }
    var showIntro by remember { mutableStateOf(true) }
    
    // Undo stack
    var historyStack by remember { mutableStateOf(listOf<ScoreboardState>()) }

    fun undoLastAction() {
        if (historyStack.isNotEmpty()) {
            val previousState = historyStack.last()
            historyStack = historyStack.dropLast(1)
            scoreboard = previousState
        }
    }

    val wearableManager = remember {
        WearableManager(
            context = context,
            onPointA = { 
                val updatedScoreboard = scoreboard.pointToTeamA()
                historyStack = historyStack + scoreboard
                scoreboard = updatedScoreboard
                if (updatedScoreboard.isMatchFinished()) { /* Handle finish later if needed */ }
            },
            onPointB = { 
                val updatedScoreboard = scoreboard.pointToTeamB()
                historyStack = historyStack + scoreboard
                scoreboard = updatedScoreboard
                if (updatedScoreboard.isMatchFinished()) { /* Handle finish later if needed */ }
            },
            onUndo = { undoLastAction() },
            onEnrichPoint = { typeStr, posStr ->
                // Type: WINNER, RALLY_ERROR
                // Pos: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
                val type = try { PointEventType.valueOf(typeStr) } catch (e: Exception) { null }
                val pos = try { CourtPosition.valueOf(posStr) } catch (e: Exception) { null }
                
                if (type != null && pos != null) {
                    val events = scoreboard.events.toMutableList()
                    if (events.isNotEmpty()) {
                        val lastEvent = events.last()
                        val enrichedEvent = if (type == PointEventType.RALLY_ERROR) {
                            MatchEvent(
                                type = type,
                                actor = null,
                                target = pos,
                                pointWinner = lastEvent.pointWinner,
                                server = lastEvent.server
                            )
                        } else {
                            MatchEvent(
                                type = type,
                                actor = pos,
                                target = null,
                                pointWinner = lastEvent.pointWinner,
                                server = lastEvent.server
                            )
                        }
                        events[events.lastIndex] = enrichedEvent
                        scoreboard = scoreboard.copy(events = events.toList())
                    }
                }
            }
        )
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        wearableManager.initialize()
    }
    
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { wearableManager.cleanup() }
    }

    // Sync state with Wearable & Bluetooth whenever it changes
    androidx.compose.runtime.LaunchedEffect(scoreboard, localMatchStarted) {
        wearableManager.updateMatchState(
            started = localMatchStarted,
            scoreA = scoreboard.pointLabelA(),
            scoreB = scoreboard.pointLabelB(),
            teamA = localSetup.teamALabel(),
            teamB = localSetup.teamBLabel(),
            playerTL = localSetup.topLeft.ifBlank { "Jugador 1" },
            playerTR = localSetup.topRight.ifBlank { "Jugador 2" },
            playerBL = localSetup.bottomLeft.ifBlank { "Jugador 3" },
            playerBR = localSetup.bottomRight.ifBlank { "Jugador 4" }
        )
        if (connectionState.value == BluetoothState.CONNECTED && localMatchStarted) {
            onSendScore(scoreboard.bluetoothStateMessage())
        }
    }

    fun finishAndStoreLocalMatch(finalScoreboard: ScoreboardState) {
        historyRepository.saveMatch(
            setup = localSetup,
            scoreboard = finalScoreboard,
            replacePlayedAtMillis = continuedMatchMillis
        )
        savedMatches = historyRepository.loadMatches()
        continuedMatchMillis = null
        scoreboard = ScoreboardState()
        historyStack = emptyList()
        localSetup = MatchSetup()
        localMatchStarted = false
        appMode = null
        viewingHistory = true
    }

    fun applyScoringUpdate(updatedScoreboard: ScoreboardState, scoringTeam: TeamSide?) {
        historyStack = historyStack + scoreboard
        scoreboard = updatedScoreboard
        if (updatedScoreboard.isMatchFinished()) {
            finishAndStoreLocalMatch(updatedScoreboard)
        }
    }

    if (showIntro) {
        SplashIntro(onFinished = { showIntro = false })
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (viewingHistory) {
            MatchHistoryScreen(
                matches = savedMatches,
                onBack = { viewingHistory = false },
                onDeleteMatches = { selectedMatches ->
                    historyRepository.deleteMatches(selectedMatches)
                    savedMatches = historyRepository.loadMatches()
                },
                onContinueMatch = { match ->
                    connectionState.value = BluetoothState.DISCONNECTED
                    onDisconnect()
                    val resumedSetup = match.toMatchSetup()
                    localSetup = resumedSetup
                    scoreboard = match.toScoreboardState().withInitialServiceIfMissing(resumedSetup)
                    continuedMatchMillis = match.playedAtMillis
                    setupError = null
                    appMode = AppMode.BLUETOOTH
                    localMatchStarted = true
                    viewingHistory = false
                }
            )
        } else if (appMode == null) {
            ModeSelectionScreen(
                onCreateBluetoothMatch = {
                    continuedMatchMillis = null
                    appMode = AppMode.BLUETOOTH
                    localMatchStarted = false
                },
                onViewMatches = { viewingHistory = true },
                lastMatch = savedMatches.firstOrNull()
            )
        } else if (appMode != null && !localMatchStarted) {
            LocalMatchSetupScreen(
                appMode = appMode!!,
                setup = localSetup,
                errorMessage = setupError,
                onPlayerCountSelected = {
                    setupError = null
                    localSetup = MatchSetup(playerCount = it)
                },
                onNameChanged = { position, value ->
                    setupError = null
                    localSetup = localSetup.update(position, value)
                },
                onStartMatch = {
                    val validation = localSetup.validate()
                    if (validation != null) {
                        setupError = validation
                    } else {
                        setupError = null
                        continuedMatchMillis = null
                        scoreboard = localSetup.initialScoreboardState()
                        localMatchStarted = true
                    }
                },
                onBack = {
                    onDisconnect()
                    connectionState.value = BluetoothState.DISCONNECTED
                    continuedMatchMillis = null
                    localSetup = MatchSetup()
                    localMatchStarted = false
                    appMode = null
                },
                onViewMatches = { viewingHistory = true }
            )
        } else {
            MarcadorScreen(
                appMode = appMode!!,
                matchSetup = localSetup,
                scoreboard = scoreboard,
                connectionState = connectionState.value,
                onTeam1Score = {
                    val updatedScoreboard = scoreboard.pointToTeamA()
                    applyScoringUpdate(updatedScoreboard, TeamSide.A)
                },
                onTeam2Score = {
                    val updatedScoreboard = scoreboard.pointToTeamB()
                    applyScoringUpdate(updatedScoreboard, TeamSide.B)
                },
                onPlayerWinner = { winner ->
                    applyScoringUpdate(scoreboard.recordWinner(winner), winner.teamSide())
                },
                onRallyError = { hitter, failedPlayer ->
                    applyScoringUpdate(scoreboard.recordRallyError(hitter, failedPlayer), hitter.teamSide())
                },
                onServeFault = { server ->
                    val scoringTeam = if (
                        scoreboard.firstServeFault &&
                        (scoreboard.currentServer == null || scoreboard.currentServer == server)
                    ) {
                        server.teamSide().opponent()
                    } else {
                        null
                    }
                    applyScoringUpdate(scoreboard.recordServeFault(server), scoringTeam)
                },
                onReset = { scoreboard = localSetup.initialScoreboardState() },
                onChangeMode = {
                    onDisconnect()
                    connectionState.value = BluetoothState.DISCONNECTED
                    continuedMatchMillis = null
                    scoreboard = ScoreboardState()
                    localSetup = MatchSetup()
                    localMatchStarted = false
                    appMode = null
                },
                onFinishLocalMatch = { finishAndStoreLocalMatch(scoreboard) },
                onViewMatches = {
                    savedMatches = historyRepository.loadMatches()
                    viewingHistory = true
                },
                onConnect = onConnect,
                onDisconnect = onDisconnect
            )
        }
    }
}

@Composable
fun ModeSelectionScreen(
    onCreateBluetoothMatch: () -> Unit,
    onViewMatches: () -> Unit,
    lastMatch: MatchRecord?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NightGreen)
            .padding(WindowInsets.safeDrawing.asPaddingValues())
    ) {
        CourtBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeModeButton(text = "Crear partido (Bluetooth)", onClick = onCreateBluetoothMatch)
                HomeModeButton(text = "Mostrar partidos", onClick = onViewMatches)
            }
            LastMatchCard(lastMatch = lastMatch)
        }
    }
}

@Composable
private fun HomeModeButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = CourtGreen,
            contentColor = SoftIce
        ),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.height(42.dp),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LastMatchCard(lastMatch: MatchRecord?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF283199)),
        border = BorderStroke(1.dp, CourtGreen.copy(alpha = 0.52f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Ultimo partido",
                style = MaterialTheme.typography.labelLarge,
                color = CourtGreen
            )
            if (lastMatch == null) {
                Text(
                    text = "Todavia no hay partidos guardados.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SoftIce.copy(alpha = 0.82f)
                )
            } else {
                Text(
                    text = lastMatch.playedAtLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftIce.copy(alpha = 0.72f)
                )
                Text(
                    text = "${lastMatch.teamALabel} vs ${lastMatch.teamBLabel}",
                    style = MaterialTheme.typography.titleLarge,
                    color = SoftIce
                )
                Text(
                    text = lastMatch.scoreSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftIce.copy(alpha = 0.86f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ModePreview() {
    MarcadorTheme {
        ModeSelectionScreen({}, {}, null)
    }
}
