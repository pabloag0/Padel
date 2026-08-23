package com.example.marcador

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.marcador.ui.theme.AquaAccent
import com.example.marcador.ui.theme.CourtGreen
import com.example.marcador.ui.theme.CourtGreenDark
import com.example.marcador.ui.theme.DeepTeal
import com.example.marcador.ui.theme.LimeGlow
import com.example.marcador.ui.theme.NightGreen
import com.example.marcador.ui.theme.SoftIce
import com.example.marcador.ui.theme.SurfaceGreen
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

@Composable
fun SplashIntro(onFinished: () -> Unit) {
    val contentAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.82f) }
    val logoY = remember { Animatable(18f) }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, tween(320, easing = EaseOut))
        logoScale.animateTo(1f, tween(520, easing = LinearOutSlowInEasing))
        logoY.animateTo(0f, tween(520, easing = LinearOutSlowInEasing))
        kotlinx.coroutines.delay(520)
        contentAlpha.animateTo(0f, tween(260, easing = EaseIn))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF4650C6), NightGreen, Color(0xFF202875)),
                    center = Offset(0.5f, 0.32f),
                    radius = 1100f
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(contentAlpha.value)
        ) {
            CourtBackground()
            Text(
                text = "MARCADOR",
                style = MaterialTheme.typography.headlineLarge,
                color = SoftIce,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 36.dp)
            )
            PadelBall(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = logoY.value.dp)
                    .graphicsLayer(
                        scaleX = logoScale.value,
                        scaleY = logoScale.value
                    )
            )
            Text(
                text = "Listo para jugar",
                style = MaterialTheme.typography.bodyLarge,
                color = SoftIce.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 44.dp, start = 32.dp, end = 32.dp)
            )
        }
    }
}

@Composable
fun CourtBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 8.dp)
                .blur(54.dp)
                .background(CourtGreen.copy(alpha = 0.12f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomStart)
                .padding(bottom = 40.dp)
                .blur(60.dp)
                .background(CourtGreen.copy(alpha = 0.16f), CircleShape)
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 1.4.dp.toPx()
            drawRoundRect(
                color = CourtGreen.copy(alpha = 0.16f),
                topLeft = Offset(size.width * 0.08f, size.height * 0.14f),
                size = Size(size.width * 0.84f, size.height * 0.72f),
                cornerRadius = CornerRadius(42f, 42f),
                style = Stroke(width = stroke)
            )
            drawLine(
                color = CourtGreen.copy(alpha = 0.13f),
                start = Offset(size.width * 0.5f, size.height * 0.14f),
                end = Offset(size.width * 0.5f, size.height * 0.86f),
                strokeWidth = stroke
            )
            drawLine(
                color = CourtGreen.copy(alpha = 0.13f),
                start = Offset(size.width * 0.08f, size.height * 0.5f),
                end = Offset(size.width * 0.92f, size.height * 0.5f),
                strokeWidth = stroke
            )
        }
    }
}

@Composable
fun PadelBall(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ball-spin")
    val seamOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "seam"
    )

    Canvas(modifier = modifier.size(120.dp)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF4FF88), LimeGlow, Color(0xFF9CBF1F)),
                center = Offset(size.width * 0.35f, size.height * 0.35f),
                radius = size.minDimension * 0.65f
            )
        )
        drawArc(
            color = Color.White.copy(alpha = 0.9f),
            startAngle = 70f + seamOffset,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(size.width * 0.1f, size.height * 0.1f),
            size = Size(size.width * 0.55f, size.height * 0.8f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = Color.White.copy(alpha = 0.9f),
            startAngle = 250f + seamOffset,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(size.width * 0.35f, size.height * 0.1f),
            size = Size(size.width * 0.55f, size.height * 0.8f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ImpactBurst(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(160.dp)) {
        drawImpactStroke()
    }
}

private fun DrawScope.drawImpactStroke() {
    val center = Offset(size.width / 2f, size.height / 2f)
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = size.minDimension * 0.22f,
        center = center,
        style = Stroke(width = 8f)
    )
    val lineColor = Color(0xFFFFD54A)
    repeat(8) { index ->
        val angle = Math.toRadians((index * 45.0) - 12.0)
        val start = Offset(
            x = center.x + kotlin.math.cos(angle).toFloat() * size.minDimension * 0.18f,
            y = center.y + kotlin.math.sin(angle).toFloat() * size.minDimension * 0.18f
        )
        val end = Offset(
            x = center.x + kotlin.math.cos(angle).toFloat() * size.minDimension * 0.42f,
            y = center.y + kotlin.math.sin(angle).toFloat() * size.minDimension * 0.42f
        )
        drawLine(
            color = lineColor,
            start = start,
            end = end,
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun PadelPaddle(modifier: Modifier = Modifier, rotation: Float) {
    val rotationValue by animateFloatAsState(rotation, label = "paddle-rotation")
    Image(
        painter = painterResource(id = R.drawable.splash_paddle),
        contentDescription = null,
        modifier = modifier
            .size(width = 220.dp, height = 360.dp)
            .graphicsLayer(
                rotationZ = rotationValue,
                transformOrigin = TransformOrigin(0.52f, 0.88f)
            )
    )
}

