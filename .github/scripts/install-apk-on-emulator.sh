#!/usr/bin/env bash
set -euo pipefail

DIAG_DIR="app/build/diagnostics/emulator-install"
APK_PATH="app/build/outputs/apk/production/debug/app-production-debug.apk"
PACKAGE_NAME="com.tejas.liquidplayer"

mkdir -p "$DIAG_DIR"

{
  echo "Emulator install diagnostic"
  echo "Commit: ${GITHUB_SHA:-unknown}"
  echo "API level: $(adb shell getprop ro.build.version.sdk | tr -d '\r')"
  echo "Android release: $(adb shell getprop ro.build.version.release | tr -d '\r')"
  echo "Primary ABI: $(adb shell getprop ro.product.cpu.abi | tr -d '\r')"
  echo "Device: $(adb shell getprop ro.product.model | tr -d '\r')"
} > "$DIAG_DIR/device-info.txt" 2>&1

if [[ ! -f "$APK_PATH" ]]; then
  echo "APK_NOT_FOUND: $APK_PATH" | tee "$DIAG_DIR/install-result.txt"
  exit 2
fi

sha256sum "$APK_PATH" > "$DIAG_DIR/apk-sha256.txt"

if adb install "$APK_PATH" > "$DIAG_DIR/adb-install-output.txt" 2>&1; then
  cat "$DIAG_DIR/adb-install-output.txt"
  echo "INSTALL_RESULT=SUCCESS" | tee "$DIAG_DIR/install-result.txt"
  adb shell pm path "$PACKAGE_NAME" | tee "$DIAG_DIR/installed-package-path.txt"
  adb shell dumpsys package "$PACKAGE_NAME" > "$DIAG_DIR/package-dump.txt" 2>&1 || true
else
  INSTALL_EXIT=$?
  cat "$DIAG_DIR/adb-install-output.txt"
  echo "INSTALL_RESULT=FAILED (adb exit code: $INSTALL_EXIT)" | tee "$DIAG_DIR/install-result.txt"
  adb shell df -h /data > "$DIAG_DIR/device-storage.txt" 2>&1 || true
  adb shell dumpsys package "$PACKAGE_NAME" > "$DIAG_DIR/package-dump.txt" 2>&1 || true
  adb logcat -b all -d -v threadtime > "$DIAG_DIR/logcat.txt" 2>&1 || true
  exit "$INSTALL_EXIT"
fi

adb logcat -b all -d -v threadtime > "$DIAG_DIR/logcat.txt" 2>&1 || true
