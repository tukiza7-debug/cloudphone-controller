#!/usr/bin/env python3
"""
CloudPhone Controller - Termux Server
--------------------------------------
Menerima command WebSocket (button / joystick / tap / swipe / text) daripada
Android control app dan translate kepada `adb shell input` untuk kawal
Android system pada cloud phone ini.

Jalankan: python server.py
Config  : config.json (boleh override guna environment variable CPC_TOKEN, CPC_PORT, CPC_ADB_SERIAL)
"""

import asyncio
import json
import math
import os
import subprocess
import sys
from datetime import datetime

try:
    import websockets
except ImportError:
    print("[FATAL] Module 'websockets' tidak dijumpai. Jalankan: pip install -r requirements.txt")
    sys.exit(1)

CONFIG_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "config.json")

DEFAULT_CONFIG = {
    "token": "changeme-please-set-a-real-token",
    "port": 8765,
    "adb_serial": "",
    "joystick_swipe_radius": 150,
    "joystick_swipe_duration_ms": 80,
    "button_keycodes": {
        "btn_a": 96,
        "btn_b": 97,
        "btn_x": 99,
        "btn_y": 100,
        "dpad_up": 19,
        "dpad_down": 20,
        "dpad_left": 21,
        "dpad_right": 22,
        "l1": 102,
        "r1": 103,
        "start": 108,
        "select": 109
    }
}


def load_config():
    cfg = dict(DEFAULT_CONFIG)
    if os.path.exists(CONFIG_PATH):
        try:
            with open(CONFIG_PATH, "r") as f:
                file_cfg = json.load(f)
            cfg.update(file_cfg)
            # merge button map instead of overwriting fully if partially provided
            if "button_keycodes" in file_cfg:
                merged = dict(DEFAULT_CONFIG["button_keycodes"])
                merged.update(file_cfg["button_keycodes"])
                cfg["button_keycodes"] = merged
        except Exception as e:
            print(f"[WARN] Gagal baca config.json ({e}), guna default.")
    else:
        print(f"[WARN] {CONFIG_PATH} tidak wujud, guna default config.")

    # Environment variables override config.json
    cfg["token"] = os.environ.get("CPC_TOKEN", cfg["token"])
    cfg["port"] = int(os.environ.get("CPC_PORT", cfg["port"]))
    cfg["adb_serial"] = os.environ.get("CPC_ADB_SERIAL", cfg["adb_serial"])
    return cfg


CONFIG = load_config()


def log(msg: str):
    ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{ts}] {msg}", flush=True)


def adb_base_cmd():
    cmd = ["adb"]
    if CONFIG.get("adb_serial"):
        cmd += ["-s", CONFIG["adb_serial"]]
    return cmd


def run_adb(args):
    """Run an adb command as a subprocess without blocking the event loop caller directly."""
    cmd = adb_base_cmd() + args
    try:
        result = subprocess.run(
            cmd, capture_output=True, text=True, timeout=10
        )
        if result.returncode != 0:
            log(f"[ADB-ERR] {' '.join(cmd)} -> {result.stderr.strip()}")
        return result.returncode == 0
    except FileNotFoundError:
        log("[FATAL] Command 'adb' tidak dijumpai. Pastikan android-tools/adb dipasang di Termux.")
        return False
    except subprocess.TimeoutExpired:
        log(f"[ADB-ERR] Timeout menjalankan: {' '.join(cmd)}")
        return False
    except Exception as e:
        log(f"[ADB-ERR] {' '.join(cmd)} -> {e}")
        return False


async def adb_async(args):
    """Run run_adb in a thread pool so it doesn't block the asyncio loop."""
    loop = asyncio.get_running_loop()
    return await loop.run_in_executor(None, run_adb, args)


async def handle_keyevent(name: str, pressed: bool):
    keymap = CONFIG.get("button_keycodes", {})
    code = keymap.get(name)
    if code is None:
        log(f"[WARN] Button tidak dikenali dalam mapping: {name}")
        return
    # Android `input keyevent` is a single down+up pulse. We only fire on press
    # (pressed=True) to avoid double-firing; pressed=False is logged for state tracking
    # (e.g. for future "hold" style handling / repeat-cancel).
    if pressed:
        log(f"[BTN] {name} -> keyevent {code}")
        await adb_async(["shell", "input", "keyevent", str(code)])
    else:
        log(f"[BTN] {name} released (keycode {code})")


async def handle_tap(x: int, y: int):
    log(f"[TAP] ({x}, {y})")
    await adb_async(["shell", "input", "tap", str(x), str(y)])


async def handle_swipe(x1: int, y1: int, x2: int, y2: int, duration: int):
    log(f"[SWIPE] ({x1},{y1}) -> ({x2},{y2}) dur={duration}ms")
    await adb_async(["shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(duration)])


async def handle_text(value: str):
    # Android `input text` doesn't like spaces raw; adb needs them escaped as %s
    safe_value = value.replace(" ", "%s")
    log(f"[TEXT] {value!r}")
    await adb_async(["shell", "input", "text", safe_value])


# Anchor point for joystick-to-swipe conversion. Adjust to match a neutral area
# on the cloud phone's screen resolution (e.g. its screen center).
JOYSTICK_ANCHOR_X = 540
JOYSTICK_ANCHOR_Y = 960


async def handle_joystick(dx: float, dy: float):
    dx = max(-1.0, min(1.0, dx))
    dy = max(-1.0, min(1.0, dy))
    magnitude = math.hypot(dx, dy)

    if magnitude < 0.08:
        # Dead zone, ignore near-zero noise
        return

    radius = CONFIG.get("joystick_swipe_radius", 150)
    duration = CONFIG.get("joystick_swipe_duration_ms", 80)

    end_x = int(JOYSTICK_ANCHOR_X + dx * radius)
    end_y = int(JOYSTICK_ANCHOR_Y + dy * radius)

    log(f"[JOY] dx={dx:.2f} dy={dy:.2f} mag={magnitude:.2f} -> swipe to ({end_x},{end_y})")
    await adb_async([
        "shell", "input", "swipe",
        str(JOYSTICK_ANCHOR_X), str(JOYSTICK_ANCHOR_Y),
        str(end_x), str(end_y), str(duration)
    ])


async def dispatch_message(data: dict):
    msg_type = data.get("type")
    try:
        if msg_type == "button":
            await handle_keyevent(data["name"], bool(data.get("pressed", True)))
        elif msg_type == "joystick":
            await handle_joystick(float(data.get("dx", 0.0)), float(data.get("dy", 0.0)))
        elif msg_type == "tap":
            await handle_tap(int(data["x"]), int(data["y"]))
        elif msg_type == "swipe":
            await handle_swipe(
                int(data["x1"]), int(data["y1"]),
                int(data["x2"]), int(data["y2"]),
                int(data.get("duration", 100))
            )
        elif msg_type == "text":
            await handle_text(str(data.get("value", "")))
        else:
            log(f"[WARN] Message type tidak dikenali: {msg_type}")
    except (KeyError, ValueError, TypeError) as e:
        log(f"[WARN] Message tidak sah ({msg_type}): {e} | raw={data}")


async def authenticate(websocket) -> bool:
    """First message after connection must be the auth token."""
    try:
        raw = await asyncio.wait_for(websocket.recv(), timeout=10)
    except asyncio.TimeoutError:
        log("[AUTH] Timeout menunggu token daripada client, tutup connection.")
        return False
    except Exception as e:
        log(f"[AUTH] Gagal terima mesej auth: {e}")
        return False

    try:
        data = json.loads(raw)
        token = data.get("token") if isinstance(data, dict) else raw
    except json.JSONDecodeError:
        token = raw  # allow plain-text token as first message too

    if token != CONFIG["token"]:
        log("[AUTH] Token salah, connection ditolak.")
        try:
            await websocket.send(json.dumps({"type": "auth", "ok": False}))
        except Exception:
            pass
        return False

    log("[AUTH] Client berjaya sahkan token.")
    try:
        await websocket.send(json.dumps({"type": "auth", "ok": True}))
    except Exception:
        pass
    return True


async def handler(websocket):
    peer = getattr(websocket, "remote_address", ("?", "?"))
    log(f"[CONN] Client baru bersambung: {peer}")

    if not await authenticate(websocket):
        await websocket.close()
        log(f"[CONN] Client {peer} ditutup (auth gagal).")
        return

    try:
        async for raw_message in websocket:
            try:
                data = json.loads(raw_message)
            except json.JSONDecodeError:
                log(f"[WARN] Bukan JSON yang sah: {raw_message!r}")
                continue

            if not isinstance(data, dict):
                log(f"[WARN] Payload bukan objek JSON: {raw_message!r}")
                continue

            await dispatch_message(data)

    except websockets.exceptions.ConnectionClosedOK:
        log(f"[CONN] Client {peer} disconnect dengan normal.")
    except websockets.exceptions.ConnectionClosedError as e:
        log(f"[CONN] Client {peer} disconnect secara tiba-tiba: {e}")
    except Exception as e:
        log(f"[ERROR] Ralat tidak dijangka pada client {peer}: {e}")
    finally:
        log(f"[CONN] Sesi ditutup untuk {peer}.")


async def main():
    port = CONFIG["port"]
    log(f"CloudPhone Controller server bermula di 0.0.0.0:{port}")
    log(f"ADB serial: {CONFIG['adb_serial'] or '(default/only device)'}")

    async with websockets.serve(
        handler,
        "0.0.0.0",
        port,
        ping_interval=20,
        ping_timeout=20,
    ):
        await asyncio.Future()  # run forever


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        log("Server dihentikan (Ctrl+C).")
