package com.example.marcador

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.marcador.ui.theme.MarcadorTheme

class MainActivity : ComponentActivity() {
    // Aquí irá tu gestión de Bluetooth más adelante
    private var bluetoothManager: BluetoothManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar Bluetooth (lo implementaremos después)
        bluetoothManager = BluetoothManager(this)

        setContent {
            MarcadorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MarcadorScreen(
                        onTeam1Score = { sendScoreToDevice("0") },
                        onTeam2Score = { sendScoreToDevice("1") }
                    )
                }
            }
        }
    }

    private fun sendScoreToDevice(score: String) {
        // Aquí irá la lógica para enviar datos via Bluetooth
        // Por ahora solo imprimimos en consola
        println("Enviando score: $score")

        // Más adelante: bluetoothManager?.sendData(score)
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager?.disconnect()
    }
}

@Composable
fun MarcadorScreen(
    onTeam1Score: () -> Unit,
    onTeam2Score: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Marcador de Pádel",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(80.dp))

        // Botón para equipo 1
        Button(
            onClick = onTeam1Score,
            modifier = Modifier
                .padding(8.dp)
        ) {
            Text(
                text = "Punto Equipo 1",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Botón para equipo 2
        Button(
            onClick = onTeam2Score,
            modifier = Modifier
                .padding(8.dp)
        ) {
            Text(
                text = "Punto Equipo 2",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "Estado: No conectado",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MarcadorScreenPreview() {
    MarcadorTheme {
        MarcadorScreen(
            onTeam1Score = { },
            onTeam2Score = { }
        )
    }
}

// Clase placeholder para Bluetooth (la implementaremos después)
class BluetoothManager(activity: ComponentActivity) {
    fun disconnect() {
        // Implementar desconexión
    }
}