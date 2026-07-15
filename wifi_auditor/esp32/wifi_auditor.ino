#include <WiFi.h>

static String inputLine;
static HardwareSerial flipper(1);

static const char* securityName(wifi_auth_mode_t mode) {
  switch (mode) {
    case WIFI_AUTH_OPEN: return "OPEN";
    case WIFI_AUTH_WEP: return "WEP";
    case WIFI_AUTH_WPA_PSK: return "WPA";
    case WIFI_AUTH_WPA2_PSK: return "WPA2";
    case WIFI_AUTH_WPA_WPA2_PSK: return "WPA/WPA2";
    case WIFI_AUTH_WPA2_ENTERPRISE: return "WPA2-ENT";
#ifdef WIFI_AUTH_WPA3_PSK
    case WIFI_AUTH_WPA3_PSK: return "WPA3";
#endif
#ifdef WIFI_AUTH_WPA2_WPA3_PSK
    case WIFI_AUTH_WPA2_WPA3_PSK: return "WPA2/WPA3";
#endif
    default: return "UNKNOWN";
  }
}

static String safeSsid(String ssid) {
  ssid.replace('\t', ' ');
  ssid.replace('\r', ' ');
  ssid.replace('\n', ' ');
  if (ssid.length() == 0) return "<hidden>";
  return ssid.substring(0, 32);
}

static void scanNetworks() {
  flipper.println("BEGIN");
  WiFi.mode(WIFI_STA);
  WiFi.disconnect(false, true);
  delay(100);

  int count = WiFi.scanNetworks(false, true, false, 250);
  if (count < 0) {
    flipper.println("ERROR\tscan failed");
    WiFi.scanDelete();
    return;
  }

  for (int i = 0; i < count; i++) {
    flipper.print("NET\t");
    flipper.print(safeSsid(WiFi.SSID(i)));
    flipper.print('\t');
    flipper.print(WiFi.channel(i));
    flipper.print('\t');
    flipper.print(WiFi.RSSI(i));
    flipper.print('\t');
    flipper.println(securityName(WiFi.encryptionType(i)));
  }
  WiFi.scanDelete();
  flipper.println("END");
}

void setup() {
  flipper.begin(115200, SERIAL_8N1, 44, 43);
  inputLine.reserve(16);
  WiFi.mode(WIFI_STA);
}

void loop() {
  while (flipper.available()) {
    char value = flipper.read();
    if (value == '\n') {
      inputLine.trim();
      if (inputLine == "SCAN") {
        scanNetworks();
      } else if (inputLine.length()) {
        flipper.println("ERROR\tunknown command");
      }
      inputLine = "";
    } else if (value != '\r' && inputLine.length() < 15) {
      inputLine += value;
    }
  }
  delay(5);
}
