package com.example.marcador

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marcador.ui.theme.AquaAccent
import com.example.marcador.ui.theme.CourtGreen
import com.example.marcador.ui.theme.ElectricMint
import com.example.marcador.ui.theme.LimeGlow
import com.example.marcador.ui.theme.NightGreen
import com.example.marcador.ui.theme.SoftIce

@Composable
fun TopStatusCard(
    appMode: AppMode,
    matchSetup: MatchSetup?,
    connectionState: BluetoothState,
    onChangeMode: () -> Unit,
    onViewMatches: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF283199)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CourtGreen.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Marcador de padel",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SoftIce
                    )
                    Text(
                        text = "Partido Bluetooth",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricMint
                    )
                    if (matchSetup != null) {
                        Text(
                            text = "${matchSetup.teamALabel()} vs ${matchSetup.teamBLabel()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftIce.copy(alpha = 0.74f)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onViewMatches,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AquaAccent),
                        border = BorderStroke(1.dp, AquaAccent.copy(alpha = 0.35f))
                    ) {
                        Text("Ver partidos")
                    }
                    OutlinedButton(
                        onClick = onChangeMode,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftIce),
                        border = BorderStroke(1.dp, SoftIce.copy(alpha = 0.3f))
                    ) {
                        Text("Inicio")
                    }
                }
            }

            BluetoothStatus(
                connectionState = connectionState,
                onConnect = onConnect,
                onDisconnect = onDisconnect
            )
        }
    }
}

@Composable
fun BluetoothStatus(
    connectionState: BluetoothState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val label = when (connectionState) {
        BluetoothState.CONNECTED -> "Conectado a PadelMarker"
        BluetoothState.CONNECTING -> "Conectando con PadelMarker"
        BluetoothState.ERROR -> "Error de conexion"
        BluetoothState.DISCONNECTED -> "Sin conectar"
    }
    val color = when (connectionState) {
        BluetoothState.CONNECTED -> CourtGreen
        BluetoothState.CONNECTING -> LimeGlow
        BluetoothState.ERROR -> Color(0xFFFF7B7B)
        BluetoothState.DISCONNECTED -> SoftIce.copy(alpha = 0.7f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color)
        }
        when (connectionState) {
            BluetoothState.CONNECTING -> CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = LimeGlow
            )
            BluetoothState.CONNECTED -> OutlinedButton(
                onClick = onDisconnect,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftIce)
            ) { Text("Desconectar") }
            else -> Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CourtGreen,
                    contentColor = NightGreen
                )
            ) { Text("Conectar") }
        }
    }
}

@Composable
fun ScoreboardCard(
    scoreboard: ScoreboardState,
    teamALabel: String,
    teamBLabel: String
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF283199)),
        border = BorderStroke(1.dp, CourtGreen.copy(alpha = 0.52f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "PARTIDO EN CURSO",
                color = ElectricMint,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            ScoreRow(teamALabel, scoreboard.pointLabelA(), scoreboard.gamesA, scoreboard.setsA, CourtGreen)
            HorizontalDivider(color = CourtGreen.copy(alpha = 0.35f))
            ScoreRow(teamBLabel, scoreboard.pointLabelB(), scoreboard.gamesB, scoreboard.setsB, AquaAccent)
        }
    }
}

@Composable
fun ScoreRow(team: String, point: String, games: Int, sets: Int, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = team, style = MaterialTheme.typography.titleLarge, color = SoftIce)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("Juegos", games.toString())
                StatPill("Sets", sets.toString())
            }
        }

        Box(
            modifier = Modifier
                .size(width = 132.dp, height = 110.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF323CA8))
                .border(1.dp, CourtGreen.copy(alpha = 0.65f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = point, style = MaterialTheme.typography.displayLarge, color = SoftIce)
        }
    }
}

@Composable
fun StatPill(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF323CA8))
            .border(1.dp, CourtGreen.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = SoftIce.copy(alpha = 0.7f))
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = SoftIce)
    }
}

@Composable
fun ActionButtons(
    appMode: AppMode,
    onTeam1Score: () -> Unit,
    onTeam2Score: () -> Unit,
    onReset: () -> Unit,
    onFinishLocalMatch: () -> Unit,
    scoringEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ScoreActionButton("Punto Equipo 1", "Suma al equipo superior", CourtGreen, scoringEnabled, Modifier.weight(1f), onTeam1Score)
            ScoreActionButton("Punto Equipo 2", "Suma al equipo inferior", AquaAccent, scoringEnabled, Modifier.weight(1f), onTeam2Score)
        }
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftIce)
        ) { Text("Reiniciar partido") }
        Button(
            onClick = onFinishLocalMatch,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AquaAccent,
                contentColor = NightGreen
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Guardar partido")
        }
        Text(
            text = "Puedes registrar el partido sin conexion. Si conectas Bluetooth, tambien se enviara al marcador.",
            style = MaterialTheme.typography.bodyMedium,
            color = SoftIce.copy(alpha = 0.74f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PlayerEventGrid(
    setup: MatchSetup,
    scoreboard: ScoreboardState,
    selectedHitter: CourtPosition?,
    scoringEnabled: Boolean,
    onPlayerTap: (CourtPosition) -> Unit,
    onWinner: (CourtPosition) -> Unit,
    onServeFault: (CourtPosition) -> Unit,
    onReset: () -> Unit,
    onFinishLocalMatch: () -> Unit,
    appMode: AppMode
) {
    val namesByPosition = setup.assignments().associate { it.position to it.name }
    val serverName = scoreboard.currentServer?.let { namesByPosition[it] ?: it.label } ?: "Sin sortear"
    val selectedHitterName = selectedHitter?.let { namesByPosition[it] ?: it.label }
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF283199)),
        border = BorderStroke(1.dp, CourtGreen.copy(alpha = 0.52f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Registro del punto",
                style = MaterialTheme.typography.titleLarge,
                color = SoftIce
            )
            Text(
                text = if (scoreboard.firstServeFault) {
                    "Sacador: $serverName · segundo saque"
                } else {
                    "Sacador: $serverName"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = ElectricMint
            )
            Text(
                text = selectedHitterName?.let {
                    "$it seleccionado. Ahora toca el rival que ha fallado."
                } ?: "Toque: golpe bueno, segundo toque: fallo rival. Mantener: winner. Doble toque sobre el sacador: saque fallado.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (selectedHitterName != null) LimeGlow else SoftIce.copy(alpha = 0.78f),
                fontWeight = if (selectedHitterName != null) FontWeight.Bold else FontWeight.Normal
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, CourtGreen.copy(alpha = 0.58f), RoundedCornerShape(24.dp))
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    PlayerEventButton(
                        position = CourtPosition.TOP_LEFT,
                        name = namesByPosition[CourtPosition.TOP_LEFT].orEmpty(),
                        color = Color(0xFF1F4505),
                        isSelected = selectedHitter == CourtPosition.TOP_LEFT,
                        isServer = scoreboard.currentServer == CourtPosition.TOP_LEFT,
                        enabled = scoringEnabled,
                        modifier = Modifier.weight(1f),
                        onPlayerTap = onPlayerTap,
                        onWinner = onWinner,
                        onServeFault = onServeFault
                    )
                    PlayerEventButton(
                        position = CourtPosition.TOP_RIGHT,
                        name = namesByPosition[CourtPosition.TOP_RIGHT].orEmpty(),
                        color = Color(0xFF227C7A),
                        isSelected = selectedHitter == CourtPosition.TOP_RIGHT,
                        isServer = scoreboard.currentServer == CourtPosition.TOP_RIGHT,
                        enabled = scoringEnabled,
                        modifier = Modifier.weight(1f),
                        onPlayerTap = onPlayerTap,
                        onWinner = onWinner,
                        onServeFault = onServeFault
                    )
                }
                Row(modifier = Modifier.weight(1f)) {
                    PlayerEventButton(
                        position = CourtPosition.BOTTOM_LEFT,
                        name = namesByPosition[CourtPosition.BOTTOM_LEFT].orEmpty(),
                        color = Color(0xFF4C6FFF),
                        isSelected = selectedHitter == CourtPosition.BOTTOM_LEFT,
                        isServer = scoreboard.currentServer == CourtPosition.BOTTOM_LEFT,
                        enabled = scoringEnabled,
                        modifier = Modifier.weight(1f),
                        onPlayerTap = onPlayerTap,
                        onWinner = onWinner,
                        onServeFault = onServeFault
                    )
                    PlayerEventButton(
                        position = CourtPosition.BOTTOM_RIGHT,
                        name = namesByPosition[CourtPosition.BOTTOM_RIGHT].orEmpty(),
                        color = Color(0xFFB4871B),
                        isSelected = selectedHitter == CourtPosition.BOTTOM_RIGHT,
                        isServer = scoreboard.currentServer == CourtPosition.BOTTOM_RIGHT,
                        enabled = scoringEnabled,
                        modifier = Modifier.weight(1f),
                        onPlayerTap = onPlayerTap,
                        onWinner = onWinner,
                        onServeFault = onServeFault
                    )
                }
            }
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftIce)
            ) { Text("Reiniciar partido") }
            Button(
                onClick = onFinishLocalMatch,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AquaAccent,
                    contentColor = NightGreen
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Guardar partido")
            }
            Text(
                text = "Puedes registrar el partido sin conexion. Si conectas Bluetooth, tambien se enviara al marcador.",
                style = MaterialTheme.typography.bodyMedium,
                color = SoftIce.copy(alpha = 0.74f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PlayerEventButton(
    position: CourtPosition,
    name: String,
    color: Color,
    isSelected: Boolean,
    isServer: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onPlayerTap: (CourtPosition) -> Unit,
    onWinner: (CourtPosition) -> Unit,
    onServeFault: (CourtPosition) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .border(
                width = if (isSelected) 5.dp else if (isServer) 3.dp else 1.dp,
                color = when {
                    isSelected -> ElectricMint
                    isServer -> LimeGlow
                    else -> Color.White.copy(alpha = 0.28f)
                }
            )
            .background(if (enabled) color else color.copy(alpha = 0.34f))
            .pointerInput(enabled, position) {
                detectTapGestures(
                    onTap = {
                        if (enabled) onPlayerTap(position)
                    },
                    onDoubleTap = {
                        if (enabled) onServeFault(position)
                    },
                    onLongPress = {
                        if (enabled) onWinner(position)
                    }
                )
            }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium,
                color = SoftIce,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            if (isServer) {
                Text(
                    text = "SACA",
                    style = MaterialTheme.typography.labelLarge,
                    color = LimeGlow,
                    letterSpacing = 1.8.sp
                )
            }
        }
    }
}

@Composable
fun ScoreActionButton(
    label: String,
    subtitle: String,
    accent: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(140.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = NightGreen,
            disabledContainerColor = Color.White.copy(alpha = 0.12f),
            disabledContentColor = SoftIce.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ModeCard(
    title: String,
    subtitle: String,
    accent: Color,
    action: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF283199)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CourtGreen.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(CourtGreen))
            }
            Text(text = title, style = MaterialTheme.typography.headlineMedium, color = SoftIce)
            Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, color = SoftIce.copy(alpha = 0.82f))
            OutlinedButton(
                onClick = onClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
            ) { Text(action) }
        }
    }
}




