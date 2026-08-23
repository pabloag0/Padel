package com.example.marcador

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
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
            bluetoothManager.initialize()
        } else {
            Log.e("Bluetooth", "Permisos denegados")
            connectionState.value = BluetoothState.ERROR
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bluetoothManager = BluetoothManager(deviceName) { state ->
            connectionState.value = state
        }

        setContent {
            MarcadorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PadelMarkerApp(
                        connectionState = connectionState,
                        onBluetoothModeSelected = { requestBluetoothPermissions() },
                        onConnect = { bluetoothManager.connect() },
                        onDisconnect = { bluetoothManager.disconnect() },
                        onSendScore = { score -> bluetoothManager.sendData(score) }
                    )
                }
            }
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
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

private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"

@SuppressLint("MissingPermission")
class BluetoothManager(
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
            val targetDevice = pairedDevices?.firstOrNull { it.name == deviceName }

            if (targetDevice == null) {
                Log.e("Bluetooth", "Dispositivo '$deviceName' no encontrado en dispositivos emparejados")
                onStateChange(BluetoothState.ERROR)
                return
            }

            bluetoothSocket = targetDevice.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID))

            Thread {
                try {
                    bluetoothSocket?.connect()
                    outputStream = bluetoothSocket?.outputStream
                    onStateChange(BluetoothState.CONNECTED)
                    Log.d("Bluetooth", "Conectado exitosamente a $deviceName")
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Error al conectar: ${e.message}")
                    disconnect()
                    onStateChange(BluetoothState.ERROR)
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
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Error al desconectar: ${e.message}")
        } finally {
            outputStream = null
            bluetoothSocket = null
            onStateChange(BluetoothState.DISCONNECTED)
        }
    }
}
