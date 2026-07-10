# WiFi Auditor for Flipper Zero

Safe, passive Wi-Fi configuration auditor for Flipper Zero and the official
Wi-Fi Developer Board (ESP32-S2). It lists nearby access points and highlights
weak or open security settings. It does not connect to networks, capture
handshakes, transmit deauthentication frames, or attempt passwords.

## Ratings

- `OK`: WPA2, WPA3, WPA2/WPA3, or WPA2 Enterprise
- `WARN`: WPA, mixed/unknown security
- `RISK`: open network or WEP

## Build the Flipper app

```sh
cd wifi_auditor/flipper
ufbt
ufbt launch
```

The FAP is created under `wifi_auditor/flipper/dist/`. `ufbt launch` installs
and starts it on a USB-connected Flipper.

## Flash the Wi-Fi Developer Board

Open `esp32/wifi_auditor.ino` in Arduino IDE, select `ESP32S2 Dev Module`, and
upload it to the board over USB. The sketch uses UART1 at 115200 baud
(ESP32-S2 GPIO44 RX / GPIO43 TX) to communicate with Flipper.

Cold-plug the board into Flipper after flashing: turn Flipper off, attach the
board, then turn Flipper on.

## Controls

- `OK`: scan again
- `Up` / `Down`: select a network
- `Back`: exit

Only audit networks and radio environments you own or are authorized to test.
