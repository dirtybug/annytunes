#!/usr/bin/env bash
set -e

echo "===================================================="
echo "            Annytunes - Workspace Clean             "
echo "===================================================="
echo ""

echo "[1/4] Stopping Gradle daemons..."
[ -f "./gradlew" ] && ./gradlew --stop 2>/dev/null || true

echo "[2/4] Removing build directories..."
rm -rf build app/build flutter/build 2>/dev/null || true

echo "[3/4] Removing .gradle and .idea directories..."
rm -rf .gradle .idea 2>/dev/null || true

echo "[4/4] Removing residual cache and iml files..."
find . -name "*.iml" -delete 2>/dev/null || true

echo ""
echo "===================================================="
echo "  Workspace cleaned successfully!                   "
echo "===================================================="
