#!/usr/bin/env bash
set -e

if [ "$ENTRYPOINT_RELOADED" != "1" ] && [ -f /workspace/docker-entrypoint.sh ] && [ "$0" != "/workspace/docker-entrypoint.sh" ]; then
    export ENTRYPOINT_RELOADED=1
    sed -i 's/\r$//' /workspace/docker-entrypoint.sh 2>/dev/null || true
    chmod +x /workspace/docker-entrypoint.sh 2>/dev/null || true
    exec /bin/bash /workspace/docker-entrypoint.sh "$@"
fi

echo "====================================================="
echo "               Annytunes Test & Build Runner         "
echo "====================================================="

VERSION_TAG="${APP_VERSION_NAME:-1.3}"
if [[ "$VERSION_TAG" != v* ]]; then
    VERSION_TAG="v${VERSION_TAG}"
fi
RELEASE_DIR="${RELEASE_DIR:-/workspace/release/development}"
mkdir -p "$RELEASE_DIR/reports/unit-tests"

# Update version in AndroidManifest.xml or build.gradle.kts if provided
if [ -n "$APP_VERSION_NAME" ] && [ -f /workspace/app/src/main/AndroidManifest.xml ]; then
    CLEAN_VER="${APP_VERSION_NAME#v}"
    sed -i -E "s/android:versionName=\"[^\"]*\"/android:versionName=\"${CLEAN_VER}\"/g" /workspace/app/src/main/AndroidManifest.xml 2>/dev/null || true
    if [ -n "$APP_VERSION_CODE" ]; then
        sed -i -E "s/android:versionCode=\"[^\"]*\"/android:versionCode=\"${APP_VERSION_CODE}\"/g" /workspace/app/src/main/AndroidManifest.xml 2>/dev/null || true
    fi
fi
[ -f /workspace/gradlew ] && sed -i 's/\r$//' /workspace/gradlew 2>/dev/null || true
[ -f /workspace/gradlew ] && chmod +x /workspace/gradlew 2>/dev/null || true
rm -f /root/.gradle/caches/journal-1/*.lock 2>/dev/null || true
rm -f /root/.gradle/caches/*.lock 2>/dev/null || true

KEYSTORE_FILE=""
for kf in /workspace/release/key.jks /workspace/release/release.keystore /workspace/key.jks /workspace/release.keystore; do
    if [ -f "$kf" ]; then
        KEYSTORE_FILE="$kf"
        break
    fi
done

if [ -z "$KEYSTORE_PASSWORD" ]; then
    if [ -f /workspace/release/keystore-pass.txt ]; then
        export KEYSTORE_PASSWORD=$(cat /workspace/release/keystore-pass.txt | tr -d '\r\n')
    elif [ -f /workspace/keystore-pass.txt ]; then
        export KEYSTORE_PASSWORD=$(cat /workspace/keystore-pass.txt | tr -d '\r\n')
    fi
fi

if [ -n "$KEYSTORE_FILE" ] && [ -n "$KEYSTORE_PASSWORD" ]; then
    echo "✓ Keystore found at $KEYSTORE_FILE with password configured. Release will be SIGNED."
    if [ -z "$KEY_ALIAS" ]; then
        export KEY_ALIAS="key0"
    fi
    if [ -z "$KEY_PASSWORD" ]; then
        export KEY_PASSWORD="$KEYSTORE_PASSWORD"
    fi
else
    echo "ℹ Keystore or password not found. Release will be built UNSIGNED."
    unset KEYSTORE_PASSWORD
    unset KEY_ALIAS
    unset KEY_PASSWORD
fi

ACTION="${1:-unit}"

export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=\"-Xmx2048m -XX:MaxMetaspaceSize=512m\""

case "$ACTION" in
    dev|shell|bash|sh)
        export GRADLE_OPTS="-Dorg.gradle.daemon=true -Dorg.gradle.jvmargs=\"-Xmx2048m -XX:MaxMetaspaceSize=512m\""
        echo "====================================================="
        echo "        Annytunes - Development Environment         "
        echo "====================================================="
        echo "Java:        $(java -version 2>&1 | head -n 1)"
        echo "Android SDK: $ANDROID_HOME (API 36, Build-Tools 36.0.0)"
        echo "Workspace:   $(pwd)"
        echo "Available quick commands:"
        echo "  test                        -> Run unit tests"
        echo "  build                       -> Compile Debug APK"
        echo "  release                     -> Compile Release APK and AAB"
        echo "  lint                        -> Run Android Lint analysis"
        echo "  ./gradlew test --continuous -> Automatically re-test on edit"
        echo "  exit                        -> Exit container"
        echo "====================================================="
        exec /bin/bash
        ;;

    unit|test)
        echo ">>> Running Unit Tests for Annytunes..."
        ./gradlew test -PversionName="${APP_VERSION_NAME:-1.3}" -PversionCode="${APP_VERSION_CODE:-3}" --info --stacktrace
        echo ""
        echo ">>> Copying unit test reports to $RELEASE_DIR/reports/unit-tests..."
        if [ -d "app/build/reports/tests/testDebugUnitTest" ]; then
            cp -r app/build/reports/tests/testDebugUnitTest/* "$RELEASE_DIR/reports/unit-tests/"
            [ ! -f "$RELEASE_DIR/reports/index.html" ] && cp "$RELEASE_DIR/reports/unit-tests/index.html" "$RELEASE_DIR/reports/index.html" 2>/dev/null || true
            echo "✓ Unit test report saved to $RELEASE_DIR/reports/unit-tests/index.html"
        fi
        echo "✓ Unit tests completed successfully in $RELEASE_DIR!"
        ;;

    build|assemble)
        echo ">>> Building Debug APK (Version: ${APP_VERSION_NAME:-1.3}, Code: ${APP_VERSION_CODE:-3})..."
        ./gradlew assembleDebug -PversionName="${APP_VERSION_NAME:-1.3}" -PversionCode="${APP_VERSION_CODE:-3}" --info
        mkdir -p "$RELEASE_DIR"
        mkdir -p /workspace/release
        find app/build/outputs/apk/debug -name "*.apk" -exec cp {} "$RELEASE_DIR/Annytunes-debug.apk" \; 2>/dev/null || true
        cp -f "$RELEASE_DIR/Annytunes-debug.apk" "$RELEASE_DIR/Annytunes-development-debug.apk" 2>/dev/null || true
        cp -f "$RELEASE_DIR/Annytunes-debug.apk" /workspace/release/Annytunes-debug.apk 2>/dev/null || true
        echo "✓ Debug APK saved to $RELEASE_DIR/Annytunes-debug.apk and /workspace/release/Annytunes-debug.apk"
        ;;

    release)
        echo ">>> Building Release Deliverables (Version: ${APP_VERSION_NAME:-1.3}, Code: ${APP_VERSION_CODE:-3})..."
        KEY_ALIAS_PARAM=""
        if [ -n "$KEY_ALIAS" ]; then
            KEY_ALIAS_PARAM="-PkeyAlias=$KEY_ALIAS"
        fi
        KEY_PASS_PARAM=""
        if [ -n "$KEYSTORE_PASSWORD" ]; then
            KEY_PASS_PARAM="-PkeystorePassword=$KEYSTORE_PASSWORD"
        fi
        mkdir -p "$RELEASE_DIR"
        mkdir -p /workspace/release
        ./gradlew assembleRelease bundleRelease -PversionName="${APP_VERSION_NAME:-1.3}" -PversionCode="${APP_VERSION_CODE:-3}" $KEY_PASS_PARAM $KEY_ALIAS_PARAM --info
        find app/build/outputs/apk/release -name "*.apk" -exec cp {} "$RELEASE_DIR/Annytunes-release.apk" \; 2>/dev/null || true
        cp -f "$RELEASE_DIR/Annytunes-release.apk" "$RELEASE_DIR/Annytunes-development-release.apk" 2>/dev/null || true
        cp -f "$RELEASE_DIR/Annytunes-release.apk" /workspace/release/Annytunes-release.apk 2>/dev/null || true
        find app/build/outputs/bundle/release -name "*.aab" -exec cp {} "$RELEASE_DIR/Annytunes-release.aab" \; 2>/dev/null || true
        cp -f "$RELEASE_DIR/Annytunes-release.aab" /workspace/release/Annytunes-release.aab 2>/dev/null || true
        (cd "$RELEASE_DIR" && sha256sum *.apk *.aab > SHA256SUMS.txt 2>/dev/null || true)
        (cd /workspace/release && sha256sum Annytunes-release.apk Annytunes-release.aab > SHA256SUMS.txt 2>/dev/null || true)
        echo "✓ Release APK saved to $RELEASE_DIR/Annytunes-release.apk and /workspace/release/Annytunes-release.apk"
        [ -f "$RELEASE_DIR/Annytunes-release.aab" ] && echo "✓ Release AAB saved to $RELEASE_DIR/Annytunes-release.aab and /workspace/release/Annytunes-release.aab"
        ;;

    bundle|aab)
        echo ">>> Building Release AAB (Android App Bundle for Google Play Store)..."
        KEY_ALIAS_PARAM=""
        if [ -n "$KEY_ALIAS" ]; then
            KEY_ALIAS_PARAM="-PkeyAlias=$KEY_ALIAS"
        fi
        KEY_PASS_PARAM=""
        if [ -n "$KEYSTORE_PASSWORD" ]; then
            KEY_PASS_PARAM="-PkeystorePassword=$KEYSTORE_PASSWORD"
        fi
        ./gradlew bundleRelease -PversionName="${APP_VERSION_NAME:-1.3}" -PversionCode="${APP_VERSION_CODE:-3}" $KEY_PASS_PARAM $KEY_ALIAS_PARAM --info
        AAB_FILE=$(find app/build/outputs/bundle/release -name "*.aab" 2>/dev/null | head -n 1)
        if [ -n "$AAB_FILE" ] && [ -f "$AAB_FILE" ]; then
            TARGET_VER="${APP_VERSION_NAME:-1.3}"
            TARGET_VER="${TARGET_VER#v}"
            VERSION_DIR="/workspace/release/v${TARGET_VER}"
            mkdir -p "$VERSION_DIR"
            mkdir -p "/workspace/release/development"
            cp -f "$AAB_FILE" "$VERSION_DIR/Annytunes-release.aab"
            cp -f "$AAB_FILE" "/workspace/release/development/Annytunes-release.aab"
            (cd "$VERSION_DIR" && sha256sum Annytunes-release.aab > Annytunes-release.aab.sha256 2>/dev/null || true)
            echo "✓ Release AAB saved to $VERSION_DIR/Annytunes-release.aab"
            echo "✓ Development AAB saved to /workspace/release/development/Annytunes-release.aab"
        else
            echo "❌ Error: Release AAB file was not generated!"
            exit 1
        fi
        ;;

    lint)
        echo ">>> Running Android Lint..."
        ./gradlew lintDebug || true
        mkdir -p "$RELEASE_DIR/reports/lint"
        if [ -d "app/build/reports" ]; then
            find app/build/reports -name "lint-results*" -exec cp {} "$RELEASE_DIR/reports/lint/" \; 2>/dev/null || true
        fi
        ;;

    all)
        echo ">>> Running Full Build Suite (Unit Tests + Debug Build + Release APK & AAB)..."
        "$0" unit
        "$0" build
        "$0" release
        echo "✓ Full suite finished! All artifacts saved to $RELEASE_DIR/!"
        ;;

    *)
        echo ">>> Executing custom command: $@"
        exec "$@"
        ;;
esac
