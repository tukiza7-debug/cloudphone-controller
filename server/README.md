# CloudPhone Controller — Termux Server

Server WebSocket yang terima command daripada Android control app dan
translate kepada `adb shell input` pada cloud phone ini (localhost adb, atau
device lain jika `adb_serial` di-set dalam `config.json`).

## 1. Pasang dependencies

```bash
bash install.sh
```

Script ini akan pasang `python`, `android-tools` (adb), pip dependencies, dan
setup Termux:Boot supaya server auto-start bila cloud phone reboot (perlu
pasang app **Termux:Boot** dari F-Droid dahulu, buka sekali untuk grant
permission).

## 2. Setup ADB Wireless Debugging (Android 11+)

Cloud phone perlu enable **Wireless debugging** dalam Developer Options, dan
kita pair Termux dengan device itu sendiri (localhost) — ini membolehkan
`adb shell input` berjalan tanpa perlu USB atau root.

1. Buka **Settings > System > Developer options > Wireless debugging** pada
   cloud phone, hidupkan.
2. Ketik **"Pair device with pairing code"** — nota port & 6-digit kod yang
   dipaparkan.
3. Dalam Termux:

   ```bash
   adb pair localhost:<PAIRING_PORT>
   # masukkan 6-digit pairing code bila diminta
   ```

4. Selepas pairing berjaya, kembali ke skrin utama **Wireless debugging** —
   nota port sambungan yang berbeza (bukan pairing port), biasanya dipaparkan
   sebagai "IP address & Port".

   ```bash
   adb connect localhost:<CONNECT_PORT>
   adb devices
   # patut nampak: localhost:<CONNECT_PORT>   device
   ```

5. Jika ada lebih daripada satu device disambung ke adb, salin serial yang
   dipaparkan oleh `adb devices` ke medan `adb_serial` dalam `config.json`.

## 3. Sunting config.json

```json
{
  "token": "tukar-ke-token-rahsia-anda",
  "port": 8765,
  "adb_serial": ""
}
```

`token` mesti sepadan dengan token yang di-set dalam Settings pada Android
control app (Phone 1). Boleh juga override guna environment variable:

```bash
export CPC_TOKEN="token-rahsia"
export CPC_PORT=8765
export CPC_ADB_SERIAL="localhost:5555"
```

## 4. Jalankan server

```bash
python server.py
```

Anda akan nampak log macam:

```
[2026-09-09 10:00:00] CloudPhone Controller server bermula di 0.0.0.0:8765
[2026-09-09 10:00:00] ADB serial: (default/only device)
```

## 5. Test connection sebelum guna app

Boleh test guna `websocat` (Termux: `pkg install websocat`) atau Python
sekali pakai:

```bash
python3 - <<'PY'
import asyncio, json, websockets

async def test():
    async with websockets.connect("ws://127.0.0.1:8765") as ws:
        await ws.send(json.dumps({"token": "tukar-ke-token-rahsia-anda"}))
        print("auth reply:", await ws.recv())
        await ws.send(json.dumps({"type": "tap", "x": 500, "y": 800}))

asyncio.run(test())
PY
```

Kalau nampak `{"type": "auth", "ok": true}` dan skrin cloud phone dapat tap,
server berfungsi dan sedia untuk sambung dari Android control app (Phone 1).

## Nota firewall / rangkaian

- `server.py` bind ke `0.0.0.0`, jadi ia boleh dicapai dari device lain dalam
  rangkaian yang sama menggunakan IP cloud phone.
- Kalau Phone 1 dan Cloud Phone berada di rangkaian berbeza (contohnya guna
  data selular masing-masing), anda perlukan VPN peribadi (contoh:
  Tailscale/WireGuard) supaya kedua-dua device boleh nampak satu sama lain —
  jangan expose port 8765 terus ke internet awam tanpa lapisan tambahan itu.
