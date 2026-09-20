#!/usr/bin/env bash
set -e

echo "===================================================="
echo "           Annytunes - Docker Test Runner           "
echo "===================================================="

TARGET="${1:-all}"

if ! command -v docker >/dev/null 2>&1; then
    echo "[ERROR] Docker is not installed or not in PATH."
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "[ERROR] Docker daemon is not running."
    exit 1
fi

echo "Running Docker container for target: $TARGET..."
case "$TARGET" in
    clean)
        ./clean.sh
        ;;
    unit)
        docker compose run --rm test-unit
        ;;
    build)
        docker compose run --rm build-apk
        ;;
    release)
        docker compose run --rm build-release
        ;;
    instrumented)
        docker compose run --rm test-instrumented
        ;;
    all)
        docker compose run --rm test-all
        ;;
    *)
        docker compose run --rm test-unit "$TARGET"
        ;;
esac
