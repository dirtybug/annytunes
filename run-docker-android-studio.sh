#!/bin/bash
# Annytunes - Run Android Studio with Native GUI or Docker Web GUI

echo "===================================================="
echo "    Annytunes - Android Studio GUI Runner (Native)  "
echo "===================================================="
echo ""

ACTION="${1:-start}"

case "$ACTION" in
    stop)
        echo "[INFO] Stopping Android Studio..."
        killall -9 studio.sh java 2>/dev/null || true
        docker compose stop android-studio 2>/dev/null || true
        echo "[INFO] Android Studio stopped successfully!"
        exit 0
        ;;
    docker)
        echo "[INFO] Starting Android Studio in Docker (Web noVNC + VNC)..."
        docker compose up -d android-studio
        echo "[INFO] Web noVNC interface ready at: http://localhost:6080/vnc.html?autoconnect=true&resize=remote"
        exit 0
        ;;
    start|*)
        echo "[1/2] Preparing native graphical environment (without VNC)..."
        echo "[2/2] Opening Android Studio in native window..."
        if [ -f "/opt/android-studio/bin/studio.sh" ]; then
            env ANDROID_HOME=/opt/android-sdk PATH="/opt/android-sdk/platform-tools:/opt/android-sdk/cmdline-tools/latest/bin:$PATH" /opt/android-studio/bin/studio.sh . &
        else
            echo "[INFO] /opt/android-studio/bin/studio.sh not found locally, launching via Docker container..."
            docker compose up -d android-studio
            echo "[INFO] Open http://localhost:6080/vnc.html?autoconnect=true&resize=remote in your browser."
        fi

        echo ""
        echo "===================================================="
        echo "        Android Studio Opened Successfully!         "
        echo "===================================================="
        echo "  - Mode:    Native Graphical Interface or Docker Web"
        echo "  - Project: Annytunes"
        echo "  - SDK:     /opt/android-sdk (API 36)"
        echo "===================================================="
        ;;
esac
