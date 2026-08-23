# Marcador Pádel 🎾

Un proyecto completo para gestionar partidos de pádel, que combina una aplicación móvil Android de seguimiento de estadísticas con un marcador físico construido sobre un microcontrolador ESP32.

## 📱 Aplicación Android

La aplicación móvil está construida nativamente para Android utilizando **Kotlin** y **Jetpack Compose**. Ha evolucionado de ser un simple "mando a distancia" a una completa herramienta de gestión deportiva.

### Características Principales
* **NUEVO (Rama Smartwatch):** Soporte oficial para relojes inteligentes con **Wear OS**. Convierte tu smartwatch en el mando a distancia perfecto con los controles de puntuación y sincronización en tiempo real.
* **NUEVO (Rama Smartwatch):** Sistema de historial integrado para poder **Deshacer (Undo)** puntos erróneos al instante, tanto desde el reloj como desde la app móvil.
* **Modo Partido Completo:** Registro de partidos para 2 o 4 jugadores, con asignación de posiciones en pista.
* **Lógica Avanzada:** Motor de puntuación que calcula automáticamente los puntos (15, 30, 40, Ventaja), deuces, juegos, sets y tie-breaks.
* **Seguimiento de Estadísticas:** Permite al usuario tocar a un jugador específico en pantalla para registrar:
  * Golpes ganadores (*Winners*).
  * Errores no forzados (*Rally Errors*).
  * Faltas de saque y dobles faltas.
* **Historial Local:** Todos los partidos jugados se guardan localmente en el dispositivo permitiendo su consulta posterior y reanudación de partidos pausados.
* **Conectividad Bluetooth:** Se sincroniza en tiempo real con el marcador físico de la pista enviando actualizaciones de puntuación a través de Bluetooth SPP (Serial Port Profile).

## ⌚ Aplicación Smartwatch (Wear OS)

En esta rama, el proyecto incluye un módulo nativo para Wear OS (Jetpack Compose for Wear OS).

### Características
* **Independencia en pista:** No tienes que llevar el móvil en el bolsillo. Deja el teléfono cerca conectado al marcador, y usa tu reloj en la muñeca para sumar puntos o deshacerlos.
* **Feedback de estado:** El reloj muestra en tiempo real la puntuación actual de los equipos.
* **Comunicación Local:** Utiliza el `MessageClient` oficial de Google Play Services para hablar con la app móvil de forma rápida e inalámbrica.

## 🔌 Hardware (ESP32)

El código `hardware.cpp` está preparado para compilarse en el entorno de Arduino y subirse a una placa **ESP32**. 

### Componentes Físicos Soportados:
* **Matrices LED WS2812B (NeoPixels):** Dos paneles de 8x8 (uno por equipo) para mostrar gráficamente los puntos (0, 15, 30, 40, AD).
* **Displays de 7 Segmentos:** Controlados mediante *Shift Registers* SN74HC595 para mostrar los juegos ganados en el set actual.
* **LEDs de Sets:** LEDs individuales (también controlados por 74HC595) para indicar los sets ganados por cada pareja (al mejor de 3).
* **Matriz de Puntos MAX7219:** Para animaciones de texto.

## 🛠️ Cómo empezar

### Para la App Android:
1. Abre este directorio en **Android Studio**.
2. Sincroniza el proyecto de Gradle.
3. Compila e instala en un dispositivo Android físico (el emulador no soporta conexiones Bluetooth).

### Para el Hardware:
1. Instala el entorno de Arduino IDE.
2. Añade el soporte para las placas ESP32.
3. Instala las librerías necesarias: `Adafruit_NeoPixel`, `BluetoothSerial`, `MD_Parola`, `MD_MAX72xx`.
4. Sube el código `hardware.cpp` a tu placa ESP32.

---
*Desarrollado y estructurado siguiendo las mejores prácticas de Arquitectura en Android (State Hoisting, Unidirectional Data Flow) y Clean Code.*
