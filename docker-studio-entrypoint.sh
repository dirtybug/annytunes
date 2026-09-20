#!/bin/bash
set -e

echo "====================================================="
echo "       Annytunes - Android Studio GUI Container      "
echo "====================================================="

# Ensure workspace local.properties points to container SDK
if [ -d "/workspace" ]; then
    echo "[SETUP] Configuring /workspace/local.properties for Android SDK..."
    echo "sdk.dir=/opt/android-sdk" > /workspace/local.properties
fi

# Set display resolution (default: 1600x900)
RESOLUTION="${RESOLUTION:-1600x900}"
SCREEN_DEPTH="${SCREEN_DEPTH:-24}"
DISPLAY="${DISPLAY:-:1}"
export DISPLAY

echo "[X11] Starting D-Bus..."
if [ -x /etc/init.d/dbus ]; then
    /etc/init.d/dbus start >/dev/null 2>&1 || true
fi

echo "[X11] Starting Xvfb virtual framebuffer on ${DISPLAY} (${RESOLUTION}x${SCREEN_DEPTH})..."
Xvfb ${DISPLAY} -screen 0 ${RESOLUTION}x${SCREEN_DEPTH} -ac +extension GLX +render -noreset &
XVFB_PID=$!
sleep 1

echo "[GUI] Starting Openbox Window Manager..."
openbox &
OPENBOX_PID=$!
sleep 1

# Set desktop background matching dark theme
xsetroot -solid "#0D1117" 2>/dev/null || true

echo "[VNC] Starting x11vnc server on port 5900..."
x11vnc -display ${DISPLAY} -forever -shared -rfbport 5900 -nopw -bg -o /var/log/x11vnc.log 2>/dev/null || true

echo "[noVNC] Starting noVNC HTML5 WebSocket proxy on port 6080..."
if [ -f /usr/share/novnc/vnc.html ] && [ ! -f /usr/share/novnc/index.html ]; then
    ln -sf /usr/share/novnc/vnc.html /usr/share/novnc/index.html
fi

websockify --web /usr/share/novnc 6080 localhost:5900 &
WEBSOCKIFY_PID=$!
sleep 1

echo "====================================================="
echo "  Annytunes Android Studio GUI is Ready!"
echo "  -------------------------------------------------"
echo "  -> Web Browser GUI (noVNC): http://localhost:6080/"
echo "  -> Direct Auto-Connect:    http://localhost:6080/vnc.html?autoconnect=true&resize=remote"
echo "  -> Native VNC Client:      localhost:5900"
echo "  -> Auto-loading Project:   /workspace (Annytunes)"
echo "====================================================="

# Disable analytics prompt
mkdir -p /root/.android
cat << 'EOF' > /root/.android/analytics.settings
{
  "hasOptedIn": false
}
EOF

# Ensure Google config directories exist
mkdir -p /root/.config/Google
mkdir -p /root/.local/share/Google

# Clean exit handler
cleanup() {
    echo "Stopping Android Studio services..."
    kill -TERM "$WEBSOCKIFY_PID" 2>/dev/null || true
    kill -TERM "$OPENBOX_PID" 2>/dev/null || true
    kill -TERM "$XVFB_PID" 2>/dev/null || true
    exit 0
}
trap cleanup SIGINT SIGTERM

echo ">>> Launching Android Studio loading /workspace..."
/opt/android-studio/bin/studio.sh /workspace &
STUDIO_PID=$!

if [ "$#" -gt 0 ] && [ "$1" != "studio" ]; then
    echo ">>> Running custom command alongside Android Studio: $@"
    exec "$@"
else
    wait $STUDIO_PID
fi
