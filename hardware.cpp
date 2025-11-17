/*
  Marcador Pádel - ESP32
  - WS2812B x2 (8x8 cada una) para puntos del juego (una por jugador)
  - MAX7219 matriz de puntos (texto "HOLA")
  - 2 x SN74HC595 -> displays 7 segmentos (juegos del set)
  - 1 x SN74HC595 -> 6 LEDs para sets (3 por jugador)
  - Bluetooth Serial: '0' => punto A, '1' => punto B

  Basado en las conexiones del doc.txt. (Ver doc.txt cargado).
  Autor: generado por assistant (adaptar colores/pins según cableado real).
*/

#include <Arduino.h>
#include <Adafruit_NeoPixel.h>
#include <BluetoothSerial.h>
#include <MD_Parola.h>
#include <MD_MAX72xx.h>
#include <SPI.h>

// ----------------------
// MATRICES PROPORCIONADAS (usar exactamente como pediste)
const int mat1[8][8] = {
  {0, 0, 1, 1, 1, 1, 0, 0},
  {0, 0, 1, 0, 0, 1, 0, 0},
  {0, 0, 1, 0, 0, 1, 0, 0},
  {0, 0, 1, 0, 0, 1, 0, 0},
  {0, 0, 1, 0, 0, 1, 0, 0},
  {0, 0, 1, 0, 0, 1, 0, 0},
  {0, 0, 1, 0, 0, 1, 0, 0},
  {0, 0, 1, 1, 1, 1, 0, 0},
};

const int mat2[8][8] = {
  {0, 0, 1, 0, 0, 1, 1, 1},
  {0, 1, 1, 0, 0, 1, 0, 0},
  {1, 0, 1, 0, 0, 1, 0, 0},
  {0, 0, 1, 0, 0, 1, 0, 0},
  {0, 0, 1, 0, 0, 1, 1, 1},
  {0, 0, 1, 0, 0, 0, 0, 1},
  {0, 0, 1, 0, 0, 0, 0, 1},
  {0, 0, 1, 0, 0, 1, 1, 1},
};

const int mat3[8][8] = {
  {1, 1, 1, 0, 1, 1, 1, 1},
  {0, 0, 1, 0, 1, 0, 0, 1},
  {0, 0, 1, 0, 1, 0, 0, 1},
  {0, 0, 1, 0, 1, 0, 0, 1},
  {1, 1, 1, 0, 1, 0, 0, 1},
  {0, 0, 1, 0, 1, 0, 0, 1},
  {0, 0, 1, 0, 1, 0, 0, 1},
  {1, 1, 1, 0, 1, 1, 1, 1},
};

const int mat4[8][8] = {
  {1, 0, 1, 0, 0, 1, 1, 1},
  {1, 0, 1, 0, 0, 1, 0, 1},
  {1, 0, 1, 0, 0, 1, 0, 1},
  {1, 1, 1, 0, 0, 1, 0, 1},
  {0, 0, 1, 0, 0, 1, 0, 1},
  {0, 0, 1, 0, 0, 1, 0, 1},
  {0, 0, 1, 0, 0, 1, 0, 1},
  {0, 0, 1, 0, 0, 1, 1, 1},
};

const int mat5[8][8] = {
  {0, 0, 0, 1, 1, 0, 0, 0},
  {0, 0, 0, 1, 1, 0, 0, 0},
  {0, 0, 0, 1, 1, 0, 0, 0},
  {1, 1, 1, 1, 1, 1, 1, 1},
  {1, 1, 1, 1, 1, 1, 1, 1},
  {0, 0, 0, 1, 1, 0, 0, 0},
  {0, 0, 0, 1, 1, 0, 0, 0},
  {0, 0, 0, 1, 1, 0, 0, 0},
};
// ----------------------

// ----------------------
// PINS (tomados de doc.txt)
#define WS1_PIN 18
#define WS2_PIN 23
#define NUMPIXELS 64

Adafruit_NeoPixel ws1(NUMPIXELS, WS1_PIN, NEO_GRB + NEO_KHZ800);
Adafruit_NeoPixel ws2(NUMPIXELS, WS2_PIN, NEO_GRB + NEO_KHZ800);

// MAX7219 (matriz de puntos)
#define HARDWARE_TYPE MD_MAX72XX::FC16_HW
#define MAX_DEVICES 4
#define DIN_PIN 5
#define CLK_PIN 32
#define CS_PIN 33
MD_Parola matriz = MD_Parola(HARDWARE_TYPE, DIN_PIN, CLK_PIN, CS_PIN, MAX_DEVICES);

// SN74HC595 chips (pines según doc)
#define c1_SRCLK 22
#define c1_RCLK 21
#define c1_SER  19

#define c2_SRCLK 26
#define c2_RCLK 25
#define c2_SER  27

#define c3_SRCLK 14
#define c3_RCLK 12
#define c3_SER  13

// Bluetooth
BluetoothSerial SerialBT;

// ----------------------
// Estado del marcador
int pointStateA = 0; // 0..4 -> mat1..mat5 (0,15,30,40,AD)
int pointStateB = 0;

int gamesA = 0;
int gamesB = 0;

int setsA = 0; // contados en leds
int setsB = 0;

// ----------------------
// 7-seg (mapa de segmentos para display CC)
// Bits: 0=a 1=b 2=c 3=d 4=e 5=f 6=g 7=dp
// Suponemos que HIGH en pin -> segmento encendido (ajusta si tu hardware es inverso)
const byte SEGMENT_MAP[10] = {
  // gfedcba (LSB = a)
  // a b c d e f g dp -> byte
  // 0
  0b11110110, // 0 -> e d c b dp a f g
  0b00110000, // 1 -> b,c
  0b11010101, // 2 -> a,b,g,e,d
  0b01110101, // 3 -> a,b,c,d,g
  0b00110011, // 4 -> f,g,b,c
  0b01100111, // 5 -> a,f,g,c,d
  0b11100111, // 6 -> a,f,e,d,c,g
  0b00110100, // 7 -> a,b,c
  0b11110111, // 8 -> all
  0b00110111  // 9 -> a,b,c,d,f,g
};

// ----------------------
// Funciones para 74HC595
void escribir595(int ser, int srclk, int rclk, byte data) {
  digitalWrite(rclk, LOW);
  // shiftOut: dataPin, clockPin, bitOrder, value
  shiftOut(ser, srclk, MSBFIRST, data);
  digitalWrite(rclk, HIGH);
}

// escribir números en displays: c1 = jugador A, c2 = jugador B
void write7SegA(int val) {
  byte out = 0x00;
  if (val >= 0 && val <= 9) out = SEGMENT_MAP[val];
  else out = 0x00;
  escribir595(c1_SER, c1_SRCLK, c1_RCLK, out);
}
void write7SegB(int val) {
  byte out = 0x00;
  if (val >= 0 && val <= 9) out = SEGMENT_MAP[val];
  else out = 0x00;
  escribir595(c2_SER, c2_SRCLK, c2_RCLK, out);
}

// LEDs sets: bits 0..2 -> setsA (1..3), bits 3..5 -> setsB (1..3)
void updateSetLeds() {
  byte out = 0;
  if (setsA >= 1) out |= (1 << 0);
  if (setsA >= 2) out |= (1 << 1);
  if (setsA >= 3) out |= (1 << 2);
  if (setsB >= 1) out |= (1 << 3);
  if (setsB >= 2) out |= (1 << 4);
  if (setsB >= 3) out |= (1 << 5);
  escribir595(c3_SER, c3_SRCLK, c3_RCLK, out);
}

// ----------------------
// Dibujar una de las matrices mat1..mat5 en un NeoPixel (8x8)
// idx: 1..5 -> mat1..mat5
void drawMatOnWS(Adafruit_NeoPixel &ws, int idx, uint32_t color) {
  const int (*mat)[8] = nullptr;
  switch (idx) {
    case 1: mat = mat1; break;
    case 2: mat = mat2; break;
    case 3: mat = mat3; break;
    case 4: mat = mat4; break;
    case 5: mat = mat5; break;
    default: mat = mat1; break;
  }
  // Recorre filas (y) y columnas (x) con for anidados 0..7 y checkea si mat[y][x] == 1
  for (int y = 0; y < 8; y++) {
    for (int x = 0; x < 8; x++) {
      int idxPixel = y * 8 + x; // suposición orden fila-major; ajusta si tu hardware es serpenteante
      if (mat[y][x] == 1) {
        ws.setPixelColor(idxPixel, color);
      } else {
        ws.setPixelColor(idxPixel, 0); // apagado
      }
    }
  }
  ws.show();
}

// Actualizar ambas WS según pointState (0..4 => mat1..mat5)
// colorA y colorB son valores ws.Color(r,g,b)
void updateWSAll(uint32_t colorA, uint32_t colorB) {
  // pointState 0..4 mapeado a mat1..mat5 (0->mat1, 1->mat2, ... 4->mat5)
  int idxA = constrain(pointStateA + 1, 1, 5);
  int idxB = constrain(pointStateB + 1, 1, 5);
  drawMatOnWS(ws1, idxA, colorA);
  drawMatOnWS(ws2, idxB, colorB);
}

// ----------------------
// Lógica de puntuación de tenis/pádel (con ventaja)
void winGameForA();
void winGameForB();

void pointToA() {
  // Caso general:
  // si ambos 40 (state=3) -> si B tiene advantage (4) entonces quitar ventaja a B (vuelve a deuce)
  // si A tiene advantage -> gana juego
  // si A < 3 y B < 3 -> subir por 0->1->2->3
  // si A==3 y B<3 -> gana juego (porque supera 40)
  if (pointStateA == 4) {
    // A ya tenía ventaja -> gana juego
    winGameForA();
    return;
  }
  if (pointStateB == 4) {
    // B tenía ventaja y ahora A gana punto -> vuelve a deuce
    pointStateB = 3;
    pointStateA = 3;
    return;
  }
  if (pointStateA == 3 && pointStateB == 3) {
    // deuce -> A ventaja
    pointStateA = 4;
    return;
  }
  if (pointStateA < 3) {
    pointStateA++;
    return;
  }
  // pointStateA == 3 y pointStateB < 3 -> A gana juego
  if (pointStateA == 3 && pointStateB < 3) {
    winGameForA();
    return;
  }
}

void pointToB() {
  if (pointStateB == 4) {
    winGameForB();
    return;
  }
  if (pointStateA == 4) {
    // A tenía ventaja y B gana -> vuelve a deuce
    pointStateA = 3;
    pointStateB = 3;
    return;
  }
  if (pointStateA == 3 && pointStateB == 3) {
    // deuce -> B ventaja
    pointStateB = 4;
    return;
  }
  if (pointStateB < 3) {
    pointStateB++;
    return;
  }
  if (pointStateB == 3 && pointStateA < 3) {
    winGameForB();
    return;
  }
}

void resetPoints() {
  pointStateA = 0;
  pointStateB = 0;
}

void checkSetWin() {
  // Gana set si games >= 6 y diferencia >=2
  // O si llega a 7 (tiebreak final)
  if ((gamesA >= 6 && (gamesA - gamesB) >= 2) || gamesA == 7) {
    setsA++;
    gamesA = 0;
    gamesB = 0;
    resetPoints();
    updateSetLeds();
    write7SegA(gamesA);
    write7SegB(gamesB);
    return;
  }
  if ((gamesB >= 6 && (gamesB - gamesA) >= 2) || gamesB == 7) {
    setsB++;
    gamesA = 0;
    gamesB = 0;
    resetPoints();
    updateSetLeds();
    write7SegA(gamesA);
    write7SegB(gamesB);
    return;
  }
}

void winGameForA() {
  gamesA++;
  resetPoints();
  // actualizar displays
  write7SegA(gamesA);
  write7SegB(gamesB);
  checkSetWin();
}

void winGameForB() {
  gamesB++;
  resetPoints();
  write7SegA(gamesA);
  write7SegB(gamesB);
  checkSetWin();
}

// ----------------------
// Setup & loop
void setup() {
  Serial.begin(115200);
  Serial.println("Marcador Padel - Inicializando...");

  // NeoPixels
  ws1.begin();
  ws2.begin();
  ws1.setBrightness(64); // ajusta brillo si hace falta
  ws2.setBrightness(64);
  ws1.show();
  ws2.show();

  // MAX7219
  matriz.begin();
  matriz.setIntensity(8);
  matriz.displayClear();
  matriz.displayText("HOLA", PA_CENTER, 0, 0, PA_PRINT, PA_NO_EFFECT);

  // Pines 74HC595
  pinMode(c1_SRCLK, OUTPUT);
  pinMode(c1_RCLK, OUTPUT);
  pinMode(c1_SER, OUTPUT);

  pinMode(c2_SRCLK, OUTPUT);
  pinMode(c2_RCLK, OUTPUT);
  pinMode(c2_SER, OUTPUT);

  pinMode(c3_SRCLK, OUTPUT);
  pinMode(c3_RCLK, OUTPUT);
  pinMode(c3_SER, OUTPUT);

  // Inicializa displays y leds apagados
  write7SegA(0);
  write7SegB(0);
  updateSetLeds();

  // Bluetooth
  if (!SerialBT.begin("PadelMarker")) {
    Serial.println("Error iniciando Bluetooth");
  } else {
    Serial.println("Bluetooth iniciado: PadelMarker");
  }
}

unsigned long lastAnimate = 0;

void loop() {
  // Animación fija MAX7219 (mantenemos HOLA)
  if (matriz.displayAnimate()) {
    matriz.displayReset();
  }

  // Lectura Bluetooth (SerialBT)
  if (SerialBT.available()) {
    String s = SerialBT.readStringUntil('\n'); // lee toda la línea
    s.trim();
    for (size_t i = 0; i < s.length(); ++i) {
      char c = s.charAt(i);
      if (c == '0') {
        pointToA();
      } else if (c == '1') {
        pointToB();
      } else if (c == 'A' || c == 'a') {
        pointToA();
      } else if (c == 'B' || c == 'b') {
        pointToB();
      } else {
        // Ignorar otros caracteres
      }
    }
    // Actualizar pantallas/matrices luego de procesar la entrada
    updateWSAll(ws1.Color(0,200,0), ws2.Color(0,0,200)); // A verde, B azul
    write7SegA(gamesA);
    write7SegB(gamesB);
    updateSetLeds();
  }

  // También actualizamos periódicamente (por si no hay bluetooth activo)
  // y para mostrar cambios de punto por defecto
  static unsigned long lastUpdate = 0;
  if (millis() - lastUpdate > 250) {
    updateWSAll(ws1.Color(0,200,0), ws2.Color(0,0,200));
    lastUpdate = millis();
  }

  // Evita bloquear, loop rápido
}
