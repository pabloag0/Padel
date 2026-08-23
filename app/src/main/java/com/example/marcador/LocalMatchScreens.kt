package com.example.marcador

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marcador.ui.theme.AquaAccent
import com.example.marcador.ui.theme.CourtGreen
import com.example.marcador.ui.theme.CourtGreenDark
import com.example.marcador.ui.theme.DeepTeal
import com.example.marcador.ui.theme.ElectricMint
import com.example.marcador.ui.theme.NightGreen
import com.example.marcador.ui.theme.SoftIce

@Composable
fun LocalMatchSetupScreen(
    appMode: AppMode,
    setup: MatchSetup,
    errorMessage: String?,
    onPlayerCountSelected: (Int) -> Unit,
    onNameChanged: (CourtPosition, String) -> Unit,
    onStartMatch: () -> Unit,
    onBack: () -> Unit,
    onViewMatches: () -> Unit
) {
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
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF283199)),
                    border = BorderStroke(1.dp, CourtGreen.copy(alpha = 0.52f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (appMode == AppMode.BLUETOOTH) "Crear partido Bluetooth" else "Crear partido local",
                            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                            color = SoftIce
                        )
                        Text(
                            "Coloca a los jugadores en su posicion inicial. La parte superior sera el Equipo 1 y la inferior el Equipo 2.",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            color = SoftIce.copy(alpha = 0.82f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PlayerCountChip("2 jugadores", setup.playerCount == 2) { onPlayerCountSelected(2) }
                            PlayerCountChip("4 jugadores", setup.playerCount == 4) { onPlayerCountSelected(4) }
                        }
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage,
                                color = Color(0xFFFF8A8A),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            item {
                PadelCourtEditor(setup = setup, onNameChanged = onNameChanged)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onStartMatch,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CourtGreen,
                            contentColor = SoftIce
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(if (appMode == AppMode.BLUETOOTH) "Crear y conectar" else "Empezar partido")
                    }
                    OutlinedButton(
                        onClick = onViewMatches,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, AquaAccent.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AquaAccent)
                    ) {
                        Text("Ver partidos")
                    }
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, SoftIce.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftIce)
                    ) {
                        Text("Volver")
                    }
                }
            }
        }
    }
}

@Composable
fun MatchHistoryScreen(
    matches: List<MatchRecord>,
    onBack: () -> Unit,
    onDeleteMatches: (Set<Long>) -> Unit,
    onContinueMatch: (MatchRecord) -> Unit
) {
    var selectedMatch by remember(matches) { mutableStateOf<MatchRecord?>(null) }
    var selectedMatchIds by remember(matches) { mutableStateOf(emptySet<Long>()) }

    selectedMatch?.let { match ->
        MatchDetailScreen(
            match = match,
            onBack = { selectedMatch = null },
            onContinueMatch = onContinueMatch
        )
        return
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Partidos jugados", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge, color = SoftIce)
                    Text(
                        "Historial con fecha, participantes y resultado final.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        color = SoftIce.copy(alpha = 0.8f)
                    )
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, SoftIce.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftIce)
                    ) {
                        Text("Volver")
                    }
                    if (matches.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    selectedMatchIds = if (selectedMatchIds.size == matches.size) {
                                        emptySet()
                                    } else {
                                        matches.map { it.playedAtMillis }.toSet()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CourtGreen,
                                    contentColor = SoftIce
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(if (selectedMatchIds.size == matches.size) "Quitar seleccion" else "Seleccionar todos")
                            }
                            Button(
                                onClick = {
                                    onDeleteMatches(selectedMatchIds)
                                    selectedMatchIds = emptySet()
                                },
                                enabled = selectedMatchIds.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF8B1E1E),
                                    contentColor = SoftIce,
                                    disabledContainerColor = SoftIce.copy(alpha = 0.14f),
                                    disabledContentColor = SoftIce.copy(alpha = 0.45f)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Borrar seleccionados")
                            }
                        }
                        Text(
                            text = "${selectedMatchIds.size} seleccionados",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = SoftIce.copy(alpha = 0.76f)
                        )
                    }
                }
            }
            if (matches.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF283199))
                    ) {
                        Text(
                            text = "Todavia no hay partidos guardados.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            color = SoftIce.copy(alpha = 0.82f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(matches) { match ->
                    MatchHistoryCard(
                        match = match,
                        isSelected = match.playedAtMillis in selectedMatchIds,
                        onClick = { selectedMatch = match },
                        onToggleSelection = {
                            selectedMatchIds = if (match.playedAtMillis in selectedMatchIds) {
                                selectedMatchIds - match.playedAtMillis
                            } else {
                                selectedMatchIds + match.playedAtMillis
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchHistoryCard(
    match: MatchRecord,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF283199)),
        border = BorderStroke(
            width = if (isSelected) 3.dp else 1.dp,
            color = if (isSelected) ElectricMint else CourtGreen.copy(alpha = 0.52f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(match.playedAtLabel, color = ElectricMint, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text("${match.teamALabel} vs ${match.teamBLabel}", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = SoftIce)
            Text(match.scoreSummary, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, color = SoftIce)
            Text(
                "Toca para ver el resultado detallado",
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                color = CourtGreen
            )
            Text(
                match.participants.joinToString("  |  ") { "${it.position.label}: ${it.name}" },
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = SoftIce.copy(alpha = 0.78f)
            )
            OutlinedButton(
                onClick = onToggleSelection,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, if (isSelected) ElectricMint else SoftIce.copy(alpha = 0.28f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) ElectricMint.copy(alpha = 0.14f) else Color.Transparent,
                    contentColor = if (isSelected) ElectricMint else SoftIce
                )
            ) {
                Text(if (isSelected) "Seleccionado" else "Seleccionar")
            }
        }
    }
}

@Composable
private fun MatchDetailScreen(
    match: MatchRecord,
    onBack: () -> Unit,
    onContinueMatch: (MatchRecord) -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Detalle del partido",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                        color = SoftIce
                    )
                    Text(
                        match.playedAtLabel,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        color = SoftIce.copy(alpha = 0.78f)
                    )
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, SoftIce.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftIce)
                    ) {
                        Text("Volver al historial")
                    }
                    if (!match.isFinished()) {
                        Button(
                            onClick = { onContinueMatch(match) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CourtGreen,
                                contentColor = SoftIce
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Continuar")
                        }
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
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
                            "RESULTADO",
                            color = ElectricMint,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        MatchScoreGrid(match = match)
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF283199)),
                    border = BorderStroke(1.dp, CourtGreen.copy(alpha = 0.38f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Participantes",
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            color = SoftIce
                        )
                        Text(
                            "EQUIPO A: ${match.teamALabel}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            color = SoftIce.copy(alpha = 0.88f)
                        )
                        Text(
                            "EQUIPO B: ${match.teamBLabel}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            color = SoftIce.copy(alpha = 0.88f)
                        )
                    }
                }
            }
            if (match.playerCount == 4) {
                item {
                    PlayerStatsCard(match = match)
                }
            }
        }
    }
}

@Composable
private fun PlayerStatsCard(match: MatchRecord) {
    val stats = match.playerStats()
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF283199)),
        border = BorderStroke(1.dp, CourtGreen.copy(alpha = 0.38f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Estadisticas",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = SoftIce
            )
            if (match.events.isEmpty()) {
                Text(
                    "Todavia no hay eventos detallados para calcular ranking.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    color = SoftIce.copy(alpha = 0.78f)
                )
            } else {
                stats.firstOrNull()?.let { mvp ->
                    Text(
                        "MVP: ${mvp.name}",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        color = ElectricMint
                    )
                }
                stats.forEachIndexed { index, playerStats ->
                    PlayerStatsRow(rank = index + 1, stats = playerStats)
                }
            }
        }
    }
}

@Composable
private fun PlayerStatsRow(
    rank: Int,
    stats: PlayerMatchStats
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CourtGreen.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .background(Color(0xFF323CA8), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "$rank. ${stats.name}",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = SoftIce
            )
            Text(
                "W ${stats.winners} · Fallos ${stats.errors} · Provocados ${stats.forcedErrorsCaused} · Saque ${stats.serveFaults}",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = SoftIce.copy(alpha = 0.72f)
            )
        }
        Text(
            stats.score.toString(),
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            color = ElectricMint,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MatchScoreGrid(match: MatchRecord) {
    val setScores = match.detailSetScores()
    val showPointColumn = !match.isFinished()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CourtGreen.copy(alpha = 0.58f), RoundedCornerShape(18.dp))
            .background(Color(0xFF323CA8), RoundedCornerShape(18.dp))
            .padding(1.dp)
    ) {
        ScoreGridRow(
            players = match.teamADetailLabel(),
            setValues = setScores.map { it?.gamesA?.toString().orEmpty() },
            point = if (showPointColumn) match.pointA else null,
            highlight = true
        )
        ScoreGridRow(
            players = match.teamBDetailLabel(),
            setValues = setScores.map { it?.gamesB?.toString().orEmpty() },
            point = if (showPointColumn) match.pointB else null,
            highlight = false
        )
    }
}

@Composable
private fun ScoreGridRow(
    players: String,
    setValues: List<String>,
    point: String?,
    highlight: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        ScoreGridCell(
            text = players,
            modifier = Modifier.weight(1.9f),
            textAlign = TextAlign.Start,
            highlight = highlight
        )
        setValues.take(3).forEach { setValue ->
            ScoreGridCell(text = setValue, modifier = Modifier.weight(0.55f), highlight = highlight)
        }
        if (point != null) {
            ScoreGridCell(text = point, modifier = Modifier.weight(0.72f), highlight = highlight)
        }
    }
}

@Composable
private fun ScoreGridCell(
    text: String,
    modifier: Modifier,
    highlight: Boolean = false,
    textAlign: TextAlign = TextAlign.Center
) {
    val backgroundColor = if (highlight) Color(0xFF283199) else Color(0xFF323CA8)
    Box(
        modifier = modifier
            .height(64.dp)
            .border(0.5.dp, CourtGreen.copy(alpha = 0.45f))
            .background(backgroundColor)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = SoftIce.copy(alpha = 0.96f),
            textAlign = textAlign,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun PlayerCountChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, if (selected) CourtGreen else SoftIce.copy(alpha = 0.24f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) CourtGreen.copy(alpha = 0.16f) else Color.Transparent,
            contentColor = if (selected) CourtGreen else SoftIce
        )
    ) {
        Text(label)
    }
}

@Composable
private fun PadelCourtEditor(
    setup: MatchSetup,
    onNameChanged: (CourtPosition, String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF283199)),
        border = BorderStroke(1.dp, CourtGreen.copy(alpha = 0.52f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pista y posiciones",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = SoftIce
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp)
                    .padding(vertical = 10.dp)
            ) {
                RealisticPadelCourtLines(modifier = Modifier.fillMaxSize())
                if (setup.playerCount == 2) {
                    TwoPlayerCourtLayout(setup = setup, onNameChanged = onNameChanged)
                } else {
                    FourPlayerCourtLayout(setup = setup, onNameChanged = onNameChanged)
                }
            }
        }
    }
}

@Composable
private fun TwoPlayerCourtLayout(
    setup: MatchSetup,
    onNameChanged: (CourtPosition, String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 70.dp)
    ) {
        PositionField(
            title = "Jugador arriba",
            value = setup.topLeft,
            centered = true,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            onValueChange = { onNameChanged(CourtPosition.TOP_LEFT, it) }
        )
        PositionField(
            title = "Jugador abajo",
            value = setup.bottomLeft,
            centered = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            onValueChange = { onNameChanged(CourtPosition.BOTTOM_LEFT, it) }
        )
    }
}

@Composable
private fun FourPlayerCourtLayout(
    setup: MatchSetup,
    onNameChanged: (CourtPosition, String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp, vertical = 70.dp)
    ) {
        PositionField(
            title = CourtPosition.TOP_LEFT.label,
            value = setup.topLeft,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp, start = 10.dp),
            onValueChange = { onNameChanged(CourtPosition.TOP_LEFT, it) }
        )
        PositionField(
            title = CourtPosition.TOP_RIGHT.label,
            value = setup.topRight,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 10.dp),
            onValueChange = { onNameChanged(CourtPosition.TOP_RIGHT, it) }
        )
        PositionField(
            title = CourtPosition.BOTTOM_LEFT.label,
            value = setup.bottomLeft,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 8.dp, start = 10.dp),
            onValueChange = { onNameChanged(CourtPosition.BOTTOM_LEFT, it) }
        )
        PositionField(
            title = CourtPosition.BOTTOM_RIGHT.label,
            value = setup.bottomRight,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp, end = 10.dp),
            onValueChange = { onNameChanged(CourtPosition.BOTTOM_RIGHT, it) }
        )
    }
}

@Composable
private fun RealisticPadelCourtLines(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val courtWidth = size.width * 0.68f
        val courtHeight = size.height * 0.88f
        val left = (size.width - courtWidth) / 2f
        val top = (size.height - courtHeight) / 2f
        val right = left + courtWidth
        val bottom = top + courtHeight
        val centerX = left + courtWidth / 2f
        val netY = top + courtHeight / 2f
        val serviceTopY = top + courtHeight * 0.23f
        val serviceBottomY = bottom - courtHeight * 0.23f

        drawRoundRect(
            color = Color(0xFF26383B),
            topLeft = Offset(left - 8.dp.toPx(), top - 8.dp.toPx()),
            size = Size(courtWidth + 16.dp.toPx(), courtHeight + 16.dp.toPx()),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF4AA8D5), Color(0xFF247BAF), Color(0xFF1B5F92)),
                startY = top,
                endY = bottom
            ),
            topLeft = Offset(left, top),
            size = Size(courtWidth, courtHeight),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        drawRoundRect(
            color = AquaAccent.copy(alpha = 0.38f),
            topLeft = Offset(left - 4.dp.toPx(), top - 4.dp.toPx()),
            size = Size(courtWidth + 8.dp.toPx(), courtHeight + 8.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        val lineColor = Color.White.copy(alpha = 0.72f)
        val lineWidth = 2.dp.toPx()
        drawRoundRect(
            color = lineColor,
            topLeft = Offset(left, top),
            size = Size(courtWidth, courtHeight),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = lineWidth)
        )
        drawLine(lineColor, Offset(left, serviceTopY), Offset(right, serviceTopY), lineWidth)
        drawLine(lineColor, Offset(left, serviceBottomY), Offset(right, serviceBottomY), lineWidth)
        drawLine(lineColor, Offset(centerX, serviceTopY), Offset(centerX, netY - 2.dp.toPx()), lineWidth)
        drawLine(lineColor, Offset(centerX, netY + 2.dp.toPx()), Offset(centerX, serviceBottomY), lineWidth)

        val netHeight = 28.dp.toPx()
        drawRoundRect(
            color = SoftIce.copy(alpha = 0.82f),
            topLeft = Offset(left, netY - netHeight / 2f),
            size = Size(courtWidth, netHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF062B2B),
            topLeft = Offset(left, netY - netHeight / 2f),
            size = Size(courtWidth, netHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        var meshX = left + 8.dp.toPx()
        while (meshX < right) {
            drawLine(
                Color(0xFF062B2B).copy(alpha = 0.24f),
                Offset(meshX, netY - netHeight / 2f),
                Offset(meshX + 18.dp.toPx(), netY + netHeight / 2f),
                1.dp.toPx()
            )
            drawLine(
                Color(0xFF062B2B).copy(alpha = 0.2f),
                Offset(meshX, netY + netHeight / 2f),
                Offset(meshX + 18.dp.toPx(), netY - netHeight / 2f),
                1.dp.toPx()
            )
            meshX += 16.dp.toPx()
        }
        drawLine(
            color = Color(0xFF062B2B),
            start = Offset(left, netY),
            end = Offset(right, netY),
            strokeWidth = 3.dp.toPx()
        )

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(190, 234, 251, 246)
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 13.sp.toPx()
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            drawText("EQUIPO 1", centerX, top - 18.dp.toPx(), paint)
            drawText("RED", centerX, netY + 5.dp.toPx(), paint)
            drawText("EQUIPO 2", centerX, bottom + 30.dp.toPx(), paint)
        }
    }
}

@Composable
private fun PositionField(
    title: String,
    value: String,
    modifier: Modifier,
    centered: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth(0.42f)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = {
                Text(
                    "Nombre",
                    color = SoftIce.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF283199),
                unfocusedContainerColor = Color(0xFF283199),
                focusedBorderColor = CourtGreen,
                unfocusedBorderColor = CourtGreen.copy(alpha = 0.45f),
                focusedTextColor = SoftIce,
                unfocusedTextColor = SoftIce,
                cursorColor = CourtGreen
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                textAlign = if (centered) TextAlign.Center else TextAlign.Start
            )
        )
    }
}

