package com.example.marcador

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.marcador.ui.theme.MarcadorTheme
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothManager: BluetoothManager
    private val deviceName = "PadelMarker"

    private val connectionState = mutableStateOf(BluetoothState.DISCONNECTED)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            initializeBluetooth()
        } else {
            Log.e("Bluetooth", "Permisos denegados")
            connectionState.value = BluetoothState.ERROR
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bluetoothManager = BluetoothManager(this, deviceName) { state ->
            connectionState.value = state
        }

        requestBluetoothPermissions()

        setContent {
            MarcadorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentState by connectionState

                    MarcadorScreen(
                        connectionState = currentState,
                        onTeam1Score = { sendScoreToDevice("0") },
                        onTeam2Score = { sendScoreToDevice("1") },
                        onConnect = { bluetoothManager.connect() },
                        onDisconnect = { bluetoothManager.disconnect() }
                    )
                }
            }
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val android12Permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        val allPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissions + android12Permissions
        } else {
            permissions
        }

        requestPermissionLauncher.launch(allPermissions)
    }

    private fun initializeBluetooth() {
        bluetoothManager.initialize()
    }

    private fun sendScoreToDevice(score: String) {
        bluetoothManager.sendData(score)
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.disconnect()
    }
}

enum class BluetoothState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

@Composable
fun MarcadorScreen(
    connectionState: BluetoothState,
    onTeam1Score: () -> Unit,
    onTeam2Score: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
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

        Spacer(modifier = Modifier.height(40.dp))

        when (connectionState) {
            BluetoothState.CONNECTING -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Conectando...")
                }
            }
            BluetoothState.CONNECTED -> {
                Text(
                    text = "✅ Conectado a PadelMarker",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDisconnect) {
                    Text("Desconectar")
                }
            }
            BluetoothState.ERROR -> {
                Text(
                    text = "❌ Error de conexión",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onConnect) {
                    Text("Reintentar conexión")
                }
            }
            else -> {
                Text(
                    text = "❌ No conectado",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onConnect) {
                    Text("Conectar a PadelMarker")
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        Button(
            onClick = onTeam1Score,
            enabled = connectionState == BluetoothState.CONNECTED,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Punto Equipo 1",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onTeam2Score,
            enabled = connectionState == BluetoothState.CONNECTED,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Punto Equipo 2",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MarcadorScreenPreview() {
    MarcadorTheme {
        MarcadorScreen(
            connectionState = BluetoothState.DISCONNECTED,
            onTeam1Score = { },
            onTeam2Score = { },
            onConnect = { },
            onDisconnect = { }
        )
    }
}

private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"

@SuppressLint("MissingPermission")
class BluetoothManager(
    private val activity: ComponentActivity,
    private val deviceName: String,
    private val onStateChange: (BluetoothState) -> Unit
) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    fun initialize() {
        if (bluetoothAdapter == null) {
            Log.e("Bluetooth", "Bluetooth no soportado")
            onStateChange(BluetoothState.ERROR)
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.e("Bluetooth", "Bluetooth no activado")
            onStateChange(BluetoothState.ERROR)
            return
        }

        connect()
    }

    fun connect() {
        onStateChange(BluetoothState.CONNECTING)

        try {
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            var targetDevice: BluetoothDevice? = null

            pairedDevices?.forEach { device ->
                if (device.name == deviceName) {
                    targetDevice = device
                    return@forEach
                }
            }

            if (targetDevice == null) {
                Log.e("Bluetooth", "Dispositivo '$deviceName' no encontrado en dispositivos emparejados")
                onStateChange(BluetoothState.ERROR)
                return
            }

            val uuid = UUID.fromString(SPP_UUID)
            bluetoothSocket = targetDevice?.createRfcommSocketToServiceRecord(uuid)

            Thread {
                try {
                    bluetoothSocket?.connect()
                    outputStream = bluetoothSocket?.outputStream
                    onStateChange(BluetoothState.CONNECTED)
                    Log.d("Bluetooth", "Conectado exitosamente a $deviceName")
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Error al conectar: ${e.message}")
                    onStateChange(BluetoothState.ERROR)
                    disconnect()
                }
            }.start()

        } catch (e: Exception) {
            Log.e("Bluetooth", "Error: ${e.message}")
            onStateChange(BluetoothState.ERROR)
        }
    }

    fun sendData(data: String) {
        if (bluetoothSocket?.isConnected != true) {
            Log.e("Bluetooth", "No conectado, no se puede enviar datos")
            return
        }

        try {
            outputStream?.write(data.toByteArray())
            outputStream?.flush()
            Log.d("Bluetooth", "Datos enviados: $data")
        } catch (e: IOException) {
            Log.e("Bluetooth", "Error al enviar datos: ${e.message}")
            onStateChange(BluetoothState.ERROR)
        }
    }

    fun disconnect() {
        try {
            bluetoothSocket?.close()
            outputStream?.close()
            onStateChange(BluetoothState.DISCONNECTED)
            Log.d("Bluetooth", "Desconectado")
        } catch (e: IOException) {
            Log.e("Bluetooth", "Error al desconectar: ${e.message}")
        }
    }
}