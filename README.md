# CloudPhone Controller

Projek 2 bahagian untuk kawal "cloud phone" dari jauh guna satu app gamepad
fullscreen di Phone 1:

- **`server/`** — Server Python (websockets) yang jalan dalam Termux di cloud
  phone. Terima command WebSocket dan translate ke `adb shell input`.
- **`android-app/`** — Android app (Kotlin + Jetpack Compose) fullscreen
  gamepad UI untuk Phone 1, sambung ke server via WebSocket.
- **`.github/workflows/`** — GitHub Actions yang auto-build APK debug dan
  publish ke GitHub Releases setiap kali push ke `main`.

Video feed (Discord Go Live dari Account 2 ke Account 3) tidak dibina di sini
— guna Discord app sedia ada seperti dirancang.

## 1. Push project ni ke GitHub repo baru

```bash
cd cloudphone-controller
git init
git add .
git commit -m "Initial commit: CloudPhone Controller"
git branch -M main
git remote add origin https://github.com/<username>/<repo-name>.git
git push -u origin main
```

Push ke `main` akan trigger workflow `build-release.yml` secara automatik.

## 2. Download APK dari GitHub Releases

1. Buka repo di GitHub, pergi ke tab **Actions** — pastikan workflow
   "Build & Release APK" selesai dengan status hijau (√).
2. Pergi ke tab **Releases** (sidebar kanan repo, atau `/releases`).
3. Release terbaru akan ada fail `cloudcontroller-v<N>.apk` — muat turun terus
   ke Phone 1.

Kalau nak trigger build manual tanpa push commit baru, guna tab **Actions >
Build & Release APK > Run workflow**.

## 3. Pasang APK di Phone 1

1. Buka **Settings > Apps > Special access > Install unknown apps**, pilih
   browser/file manager yang anda guna untuk download, hidupkan "Allow from
   this source".
2. Buka fail APK yang dimuat turun, tekan **Install**.
3. Buka app "CloudPhone Controller" — ia akan terus jadi fullscreen landscape.

## 4. Setup keseluruhan sistem (urutan penuh)

1. **Cloud Phone**: Setup ADB wireless debugging (pairing + connect) — lihat
   `server/README.md` bahagian "Setup ADB Wireless Debugging".
2. **Cloud Phone (Termux)**: Jalankan `bash install.sh` dalam folder `server/`
   untuk pasang Python, adb, dependencies, dan Termux:Boot autostart.
3. **Cloud Phone**: Sunting `server/config.json`, tukar `token` kepada token
   rahsia anda sendiri (token ini akan sepadan dengan yang di-set dalam app).
4. **Cloud Phone**: Jalankan `python server.py` — pastikan log menunjukkan
   server sudah bermula di port 8765.
5. **Phone 1**: Pasang APK (langkah 3 di atas).
6. **Phone 1**: Buka app, tekan ikon **Settings** (penjuru atas kanan),
   masukkan IP cloud phone, port (`8765`), dan auth token yang sama seperti
   `config.json`. Tekan **Save**.
7. Dot status di penjuru atas kanan app akan bertukar **hijau** apabila
   berjaya sambung. Cuba tekan mana-mana butang — cloud phone patut respon.
8. **Cloud Phone**: Buka Discord (Account 2), mula **Go Live / Screen Share**.
9. **Phone 2**: Buka Discord (Account 3), join stream Account 2 untuk tonton.

## 5. Test connection dari terminal (sebelum guna app)

Boleh test server sebelum sambung dari app, guna Python sekali pakai di
Termux atau di komputer yang sama rangkaian dengan cloud phone:

```bash
python3 - <<'PY'
import asyncio, json, websockets

async def test():
    async with websockets.connect("ws://<IP_CLOUD_PHONE>:8765") as ws:
        await ws.send(json.dumps({"token": "<TOKEN_ANDA>"}))
        print("auth reply:", await ws.recv())
        await ws.send(json.dumps({"type": "tap", "x": 500, "y": 800}))
        print("Tap command dihantar — cuba tengok skrin cloud phone.")

asyncio.run(test())
PY
```

Selengkapnya untuk bahagian server ada dalam `server/README.md`.

## Struktur projek

```
cloudphone-controller/
├── README.md                      <- fail ini
├── server/
│   ├── server.py
│   ├── config.json
│   ├── requirements.txt
│   ├── install.sh
│   └── README.md
├── android-app/
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradle.properties
│   └── app/
│       ├── build.gradle
│       ├── proguard-rules.pro
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── java/com/cloudcontroller/app/
│           │   ├── MainActivity.kt
│           │   ├── ControllerWebSocket.kt
│           │   ├── SettingsManager.kt
│           │   └── ui/
│           │       ├── GamepadScreen.kt
│           │       ├── DPad.kt
│           │       ├── FaceButtons.kt
│           │       ├── AnalogStick.kt
│           │       ├── ShoulderButtons.kt
│           │       ├── ConnectionStatusIndicator.kt
│           │       ├── SettingsDialog.kt
│           │       └── theme/Theme.kt
│           └── res/
│               ├── values/ (colors.xml, strings.xml, themes.xml)
│               ├── drawable/ (adaptive icon layers)
│               ├── mipmap-anydpi-v26/ (adaptive icon, API 26+)
│               └── mipmap-mdpi/ (fallback icon, API 24-25)
└── .github/workflows/
    ├── build-release.yml           <- push to main -> build + GitHub Release
    └── build-check.yml             <- pull request -> build validation only
```

## Nota reka bentuk: kenapa tiada `gradlew` / wrapper jar dalam repo

Fail `gradle-wrapper.jar` adalah binari, dan CI di sini sengaja dibina untuk
**tidak bergantung padanya** — workflow guna
[`gradle/actions/setup-gradle@v4`](https://github.com/gradle/actions) dengan
`gradle-version: '8.7'` untuk sediakan Gradle terus pada runner, jadi build
CI tetap konsisten tanpa perlu commit fail binari.

Untuk build **secara lokal** (contoh dalam Android Studio), buka folder
`android-app/` sebagai project — Android Studio akan auto-generate
`gradlew`/wrapper semasa Gradle Sync pertama. Kalau nak generate secara
manual dari command line dan anda ada Gradle 8.7 dipasang:

```bash
cd android-app
gradle wrapper --gradle-version 8.7
./gradlew assembleDebug
```

## Nota keselamatan rangkaian

Server bind ke `0.0.0.0:8765` tanpa TLS (`ws://`, bukan `wss://`). Ini sesuai
untuk rangkaian peribadi/VPN (contoh Tailscale/WireGuard antara Phone 1 dan
Cloud Phone), tapi **jangan** expose port ini terus ke internet awam tanpa
lapisan VPN/tunnel tambahan, kerana token auth ringkas dalam `config.json`
bukan pengganti TLS + firewall yang betul.
