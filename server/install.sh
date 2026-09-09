#!/data/data/com.termux/files/usr/bin/bash
# CloudPhone Controller - Termux install script
# Jalankan dalam Termux: bash install.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "== [1/5] Update package list =="
pkg update -y

echo "== [2/5] Pasang Python & android-tools (untuk adb) =="
pkg install -y python android-tools

echo "== [3/5] Pasang Python dependencies =="
pip install -r "$SCRIPT_DIR/requirements.txt"

echo "== [4/5] Setup Termux:Boot supaya server auto-start bila reboot =="
BOOT_DIR="$HOME/.termux/boot"
mkdir -p "$BOOT_DIR"

cat > "$BOOT_DIR/start-cloudphone-server.sh" <<EOF
#!/data/data/com.termux/files/usr/bin/bash
termux-wake-lock
cd "$SCRIPT_DIR"
python server.py >> "$SCRIPT_DIR/server.log" 2>&1
EOF

chmod +x "$BOOT_DIR/start-cloudphone-server.sh"

echo "== [5/5] Siap =="
echo ""
echo "PENTING:"
echo "1. Pasang app 'Termux:Boot' dari F-Droid (bukan Play Store) supaya script auto-start"
echo "   berfungsi selepas reboot. Buka app tu sekali selepas pasang untuk grant permission."
echo "2. Sunting server/config.json dan tukar 'token' kepada token rahsia anda sendiri."
echo "3. Pasangkan ADB wireless debugging (baca README.md untuk langkah 'adb pair' / 'adb connect')."
echo "4. Jalankan server secara manual buat kali pertama dengan: python server.py"
echo ""
