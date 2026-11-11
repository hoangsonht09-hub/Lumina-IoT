#include <WiFi.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <Wire.h>
#include <BH1750.h>
#include <math.h>

// ========== WIFI & MQTT ==========
const char* ssid = "Em iu";
const char* password = "thangbomi";

const char* mqtt_server = "172.20.10.4";
const int mqtt_port = 1884;
const char* mqtt_user = "nguyenhoangson";
const char* mqtt_pass = "b23dccn721";

WiFiClient espClient;
PubSubClient client(espClient);

// ========== PIN ==========
#define DHT_PIN 15
#define DHT_TYPE DHT22

#define LIGHT_SDA 19
#define LIGHT_SCL 21

#define LED_RED 2
#define LED_YELLOW 4
#define LED_GREEN 5
// GPIO18 -> dien tro 330 ohm -> chan dai LED; chan ngan -> GND.
#define LED_4 18

// ========== SENSOR ==========
DHT dht(DHT_PIN, DHT_TYPE);
BH1750 lightMeter(0x23);

float lightThresholdLux = 100.0;
float currentLux = NAN;

bool lightSensorReady = false;
uint8_t lightSensorAddress = 0;

// ========== LED STATUS ==========
bool redState = false;
bool yellowState = false;
bool greenState = false;
bool led4State = false;

// ========== TIMER ==========
const unsigned long SENSOR_INTERVAL = 2000;
const unsigned long LIGHT_INTERVAL = 250;
const unsigned long RETRY_INTERVAL = 5000;

unsigned long lastSensorPrint = 0;
unsigned long lastLightRead = 0;
unsigned long lastLightInitAttempt = 0;
unsigned long lastWifiAttempt = 0;
unsigned long lastMqttAttempt = 0;

// ========== LED CONTROL ==========
void setRed(bool state) {
  redState = state;
  digitalWrite(LED_RED, state ? HIGH : LOW);
}

void setYellow(bool state) {
  yellowState = state;
  digitalWrite(LED_YELLOW, state ? HIGH : LOW);
}

void setGreen(bool state) {
  greenState = state;
  digitalWrite(LED_GREEN, state ? HIGH : LOW);
}

// Dieu khien LED 4
void setLed4(bool state) {
  led4State = state;
  digitalWrite(LED_4, state ? HIGH : LOW);
}

// Bat/tat tat ca 4 LED
void setAllLed(bool state) {
  setRed(state);
  setYellow(state);
  setGreen(state);
  setLed4(state);
}

// ========== LIGHT SENSOR ==========
bool beginLightSensor() {
  const uint8_t addresses[] = {0x23, 0x5C};

  for (uint8_t address : addresses) {
    Wire.beginTransmission(address);
    uint8_t error = Wire.endTransmission();

    Serial.printf(
      "I2C 0x%02X: %s\n",
      address,
      error == 0 ? "ACK" : "Khong phan hoi"
    );

    if (error == 0 &&
        lightMeter.begin(
          BH1750::CONTINUOUS_HIGH_RES_MODE,
          address,
          &Wire
        )) {
      lightSensorAddress = address;
      return true;
    }
  }

  lightSensorAddress = 0;
  return false;
}

void updateLightSensor() {
  unsigned long now = millis();

  if (now - lastLightRead < LIGHT_INTERVAL) {
    return;
  }

  lastLightRead = now;

  if (!lightSensorReady) {
    if (now - lastLightInitAttempt >= RETRY_INTERVAL) {
      lastLightInitAttempt = now;
      lightSensorReady = beginLightSensor();

      Serial.println(
        lightSensorReady
          ? "BH1750 ket noi thanh cong"
          : "Khong tim thay BH1750"
      );
    }

    if (!lightSensorReady) {
      currentLux = NAN;
      return;
    }
  }

  float lux = lightMeter.readLightLevel();

  if (lux < 0 || isnan(lux)) {
    currentLux = NAN;
    Serial.println("Loi doc BH1750");
    return;
  }

  currentLux = lux;
}

// Dung chung payload cho phan hoi lenh va cap nhat dinh ky.
String ledStatusPayload() {
  return "Red:" + String(redState ? "ON" : "OFF") +
      " | Yel:" + String(yellowState ? "ON" : "OFF") +
      " | Gre:" + String(greenState ? "ON" : "OFF") +
      " | LED4:" + String(led4State ? "ON" : "OFF");
}

bool publishLedStatus() {
  if (!client.connected()) return false;
  String status = ledStatusPayload();
  return client.publish("home/esp32/status", status.c_str());
}

// ========== HANDLE LED COMMAND ==========
void handleCommand(String cmd) {
  cmd.trim();
  cmd.toUpperCase();

  if (cmd == "RED ON") {
    setRed(true);
  }
  else if (cmd == "RED OFF") {
    setRed(false);
  }
  else if (cmd == "YELLOW ON") {
    setYellow(true);
  }
  else if (cmd == "YELLOW OFF") {
    setYellow(false);
  }
  else if (cmd == "GREEN ON") {
    setGreen(true);
  }
  else if (cmd == "GREEN OFF") {
    setGreen(false);
  }
  else if (cmd == "LED4 ON" || cmd == "LED 4 ON") {
    setLed4(true);
  }
  else if (cmd == "LED4 OFF" || cmd == "LED 4 OFF") {
    setLed4(false);
  }
  else if (cmd == "ALL ON") {
    setAllLed(true);
  }
  else if (cmd == "ALL OFF") {
    setAllLed(false);
  }
  else {
    Serial.println("Lenh khong hop le");
    return;
  }

  Serial.print("OK: ");
  Serial.println(cmd);
  // Gui ngay trang thai thuc te de backend xac nhan lenh, khong cho doc cam bien.
  bool confirmed = publishLedStatus();
  Serial.println(confirmed ? "MQTT command status=OK" : "MQTT command status=FAIL");
}

// ========== MQTT CALLBACK ==========
void callback(
  char* topic,
  byte* payload,
  unsigned int length
) {
  String message;

  for (unsigned int i = 0; i < length; i++) {
    message += (char)payload[i];
  }

  Serial.printf(
    "MQTT [%s]: %s\n",
    topic,
    message.c_str()
  );

  // Cap nhat nguong anh sang
  if (String(topic) ==
      "home/esp32/config/light-threshold") {

    if (!message.startsWith("THRESHOLD:")) {
      Serial.println("Sai dinh dang nguong");
      return;
    }

    String value = message.substring(
      String("THRESHOLD:").length()
    );

    char* end = nullptr;
    double parsed = strtod(value.c_str(), &end);

    if (end == value.c_str() ||
        *end != '\0' ||
        !isfinite(parsed) ||
        parsed < 0 ||
        parsed > 100000) {
      Serial.println("Nguong khong hop le");
      return;
    }

    lightThresholdLux = (float)parsed;

    Serial.printf(
      "Nguong anh sang: %.1f lux\n",
      lightThresholdLux
    );

    return;
  }

  // Nhan lenh dieu khien LED
  if (String(topic) == "home/esp32/command") {
    handleCommand(message);
  }
}

// ========== WIFI & MQTT ==========
void maintainConnection() {
  unsigned long now = millis();

  if (WiFi.status() != WL_CONNECTED) {
    if (now - lastWifiAttempt >= RETRY_INTERVAL) {
      lastWifiAttempt = now;

      Serial.println("Dang ket noi WiFi...");
      WiFi.reconnect();
    }

    return;
  }

  if (client.connected()) {
    client.loop();
    return;
  }

  if (now - lastMqttAttempt < RETRY_INTERVAL) {
    return;
  }

  lastMqttAttempt = now;

  String clientId =
    "ESP32Client-" + String(random(0xffff), HEX);

  if (client.connect(
        clientId.c_str(),
        mqtt_user,
        mqtt_pass
      )) {

    Serial.println("MQTT da ket noi");

    client.subscribe("home/esp32/command");

    client.subscribe(
      "home/esp32/config/light-threshold"
    );
    publishLedStatus();

  } else {
    Serial.printf(
      "MQTT ket noi that bai, rc=%d\n",
      client.state()
    );
  }
}

// ========== FORMAT SENSOR ==========
String sensorField(
  const char* label,
  float value,
  int precision,
  const char* suffix
) {
  if (isnan(value)) {
    return String(label) + "ERR";
  }

  return String(label) +
         String(value, precision) +
         suffix;
}

// ========== SETUP ==========
void setup() {
  Serial.begin(115200);
  Serial.setTimeout(100);

  // Khoi tao 4 LED
  pinMode(LED_RED, OUTPUT);
  pinMode(LED_YELLOW, OUTPUT);
  pinMode(LED_GREEN, OUTPUT);
  pinMode(LED_4, OUTPUT);

  // Tat tat ca LED khi khoi dong
  setAllLed(false);

  // Khoi tao DHT22
  dht.begin();

  // Khoi tao BH1750
  Wire.begin(LIGHT_SDA, LIGHT_SCL);

  lightSensorReady = beginLightSensor();

  if (lightSensorReady) {
    Serial.println("BH1750 san sang");
  } else {
    Serial.println("BH1750 chua ket noi");
  }

  // Khoi tao WiFi
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);
  WiFi.begin(ssid, password);

  lastWifiAttempt = millis();

  // Khoi tao MQTT
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
  client.setSocketTimeout(1);

  lastMqttAttempt = millis() - RETRY_INTERVAL;
  lastLightRead = millis() - LIGHT_INTERVAL;

  Serial.println("ESP32 bat dau...");
  Serial.println("4 LED: RED=2, YELLOW=4, GREEN=5, LED4=18");
  Serial.println("Lenh: LED4 ON, LED4 OFF, ALL ON, ALL OFF");
  Serial.printf("MQTT broker: %s:%d\n", mqtt_server, mqtt_port);
}

// ========== LOOP ==========
void loop() {
  // Doc cam bien anh sang
  updateLightSensor();

  // Dieu khien LED qua Serial Monitor
  if (Serial.available()) {
    handleCommand(
      Serial.readStringUntil('\n')
    );
  }

  // Duy tri WiFi & MQTT
  maintainConnection();

  unsigned long now = millis();

  if (now - lastSensorPrint < SENSOR_INTERVAL) {
    return;
  }

  lastSensorPrint = now;

  // Doc DHT22
  float temperature = dht.readTemperature();
  float humidity = dht.readHumidity();

  // Du lieu anh sang
  String luxText = isnan(currentLux)
    ? "ERR"
    : String(currentLux, 1) + " lux";

  String lightStatus = isnan(currentLux)
    ? "UNKNOWN"
    : (currentLux < lightThresholdLux
        ? "TOI"
        : "SANG");

  // Sensor payload
  String payload =
    sensorField("Temp:", temperature, 1, "C") +
    " | " +
    sensorField("Hum:", humidity, 1, "%") +
    " | Lux:" + luxText +
    " | Light:" + lightStatus;

  // Trang thai 4 LED
  String ledStatus = ledStatusPayload();

  // Gui MQTT
  if (client.connected()) {
    bool sensorSent = client.publish(
      "home/esp32/sensors",
      payload.c_str()
    );

    bool statusSent = publishLedStatus();

    Serial.printf(
      "MQTT publish: sensors=%s status=%s\n",
      sensorSent ? "OK" : "FAIL",
      statusSent ? "OK" : "FAIL"
    );
  }

  // In Serial Monitor
  Serial.println("------------------------");
  Serial.println(payload);
  Serial.println(ledStatus);

  Serial.println(
    client.connected()
      ? "Da gui du lieu MQTT"
      : "MQTT chua ket noi"
  );
}
