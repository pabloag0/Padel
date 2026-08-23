package com.example.marcador

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.marcador.ui.theme.CourtGreenDark
import com.example.marcador.ui.theme.DeepTeal
import com.example.marcador.ui.theme.MarcadorTheme
import com.example.marcador.ui.theme.NightGreen
import com.example.marcador.ui.theme.SoftIce

@Composable
fun MarcadorScreen(
    appMode: AppMode,
    matchSetup: MatchSetup?,
    scoreboard: ScoreboardState,
    connectionState: BluetoothState,
    onTeam1Score: () -> Unit,
    onTeam2Score: () -> Unit,
    onPlayerWinner: (CourtPosition) -> Unit,
    onRallyError: (CourtPosition, CourtPosition) -> Unit,
    onServeFault: (CourtPosition) -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onChangeMode: () -> Unit,
    onFinishLocalMatch: () -> Unit,
    onViewMatches: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    var selectedHitter by remember { mutableStateOf<CourtPosition?>(null) }
    val scoringEnabled = true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NightGreen)
            .padding(WindowInsets.safeDrawing.asPaddingValues())
    ) {
        CourtBackground()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TopStatusCard(
                    appMode = appMode,
                    matchSetup = matchSetup,
                    connectionState = connectionState,
                    onChangeMode = onChangeMode,
                    onViewMatches = onViewMatches,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect
                )
            }
            item {
                ScoreboardCard(
                    scoreboard = scoreboard,
                    teamALabel = matchSetup?.teamALabel() ?: "Equipo 1",
                    teamBLabel = matchSetup?.teamBLabel() ?: "Equipo 2"
                )
            }
            item {
                if (matchSetup != null && matchSetup.playerCount == 4 && matchSetup.assignments().size == 4) {
                    PlayerEventGrid(
                        setup = matchSetup,
                        scoreboard = scoreboard,
                        selectedHitter = selectedHitter,
                        scoringEnabled = scoringEnabled,
                        onPlayerTap = { position ->
                            val hitter = selectedHitter
                            if (hitter == null || hitter.teamSide() == position.teamSide()) {
                                selectedHitter = position
                            } else {
                                selectedHitter = null
                                onRallyError(hitter, position)
                            }
                        },
                        onWinner = { position ->
                            selectedHitter = null
                            onPlayerWinner(position)
                        },
                        onServeFault = { position ->
                            selectedHitter = null
                            onServeFault(position)
                        },
                        onUndo = {
                            selectedHitter = null
                            onUndo()
                        },
                        onReset = {
                            selectedHitter = null
                            onReset()
                        },
                        onFinishLocalMatch = onFinishLocalMatch,
                        appMode = appMode
                    )
                } else {
                    ActionButtons(
                        appMode = appMode,
                        onTeam1Score = onTeam1Score,
                        onTeam2Score = onTeam2Score,
                        onUndo = onUndo,
                        onReset = onReset,
                        onFinishLocalMatch = onFinishLocalMatch,
                        scoringEnabled = scoringEnabled
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScorePreview() {
    MarcadorTheme {
        MarcadorScreen(
            appMode = AppMode.BLUETOOTH,
            matchSetup = MatchSetup(topLeft = "Ana", topRight = "Luis", bottomLeft = "Marta", bottomRight = "Pablo", playerCount = 4),
            scoreboard = ScoreboardState(pointStateA = 2, pointStateB = 4, gamesA = 5, gamesB = 4, setsA = 1, setsB = 0),
            connectionState = BluetoothState.DISCONNECTED,
            onTeam1Score = {},
            onTeam2Score = {},
            onPlayerWinner = {},
            onRallyError = { _, _ -> },
            onServeFault = {},
            onUndo = {},
            onReset = {},
            onChangeMode = {},
            onFinishLocalMatch = {},
            onViewMatches = {},
            onConnect = {},
            onDisconnect = {}
        )
    }
}
