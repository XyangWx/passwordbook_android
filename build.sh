#!/bin/bash

BUILD_MODE="Debug"
APK_NAME="xypasswordbook_debug"
AUTH_ISSUER=*** getopts "o:n:a:c:I:CA:h" opt; do
  case $opt in
    o) BUILD_MODE="$OPTARG" ;;
    n) APK_NAME="$OPTARG" ;;
    a) AUTH_ISSUER="$OPTARG" ;;
    c) CLIENT_ID="$OPTARG" ;;
    I) API_URI="$OPTARG" ;;
    CA) CA_ARG="$OPTARG" ;;
    h) echo "Usage: $0 -o <Release|Debug> -n <apk_name> -a <AUTH_ISSUER> -c <CLIENT_ID> -I <API_URI> -CA <jks_path@ks-key-alias>"
       echo "  -o  Build mode (Release/Debug, default: Debug)"
       echo "  -n  APK filename without extension (default: xypasswordbook_debug)"
       echo "  -a  AUTH_ISSUER URL (default: https://auth-test.mksword.com)"
       echo "  -c  CLIENT_ID (default: password_book_app)"
       echo "  -I  API_URI URL (default: https://api-test.mksword.com)"
       echo "  -CA jks_path@ks-key-alias"
       exit 0 ;;
    *) echo "Invalid option: -$opt" >&2; exit 1 ;;
  esac
done

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

chmod +x gradlew

./gradlew clean assemble"$BUILD_MODE" \
  -PAUTH_ISSUER="$AUTH_ISSUER" \
  -PCLIENT_ID="$CLIENT_ID" \
  -PAPI_URI="$API_URI"

APK_DIR="$PROJECT_ROOT/app/build/outputs/apk/$BUILD_MODE"
BUILT_APK=$(ls "$APK_DIR"/*.apk 2>/dev/null | head -1)
DEST_APK="$APK_DIR/$APK_NAME.apk"

if [ -n "$BUILT_APK" ]; then
    mv -f "$BUILT_APK" "$DEST_APK"
    echo "Output: $DEST_APK"

    if [ -n "$CA_ARG" ]; then
        # Format: jks_path@ks-key-alias
        JKS_PATH="${CA_ARG%%@*}"
        ALIAS="${CA_ARG#*@}"

        # Find Android SDK build-tools
        SDK_BUILD_TOOLS=""
        for base in "$ANDROID_HOME" "$ANDROID_SDK_ROOT" "$HOME/Android/Sdk" "/usr/local/android-sdk"; do
            if [ -d "$base/build-tools" ]; then
                LATEST=$(ls -d "$base/build-tools/"* 2>/dev/null | sort -V | tail -1)
                if [ -f "$LATEST/zipalign" ]; then
                    SDK_BUILD_TOOLS="$LATEST"
                    break
                fi
            fi
        done

        echo "jks path: $JKS_PATH"
        echo "alias: $ALIAS"
        echo "build-tools: $SDK_BUILD_TOOLS"

        if [ -n "$SDK_BUILD_TOOLS" ]; then
            ZIPALIGN="$SDK_BUILD_TOOLS/zipalign"
            APKSIGNER="$SDK_BUILD_TOOLS/apksigner"
            ALIGNED_APK="$APK_DIR/${APK_NAME}-aligned.apk"

            echo "zipalign: $ZIPALIGN"
            "$ZIPALIGN" -v 4 "$DEST_APK" "$ALIGNED_APK"
            if [ $? -eq 0 ]; then
                echo "apksigner sign with $JKS_PATH"
                "$APKSIGNER" sign --ks "$JKS_PATH" --ks-key-alias "$ALIAS" --out "$DEST_APK" "$ALIGNED_APK"
                if [ $? -eq 0 ]; then
                    rm -f "$ALIGNED_APK"
                    echo "Signed successfully."
                else
                    echo "apksigner sign failed."
                fi
            else
                echo "zipalign failed."
            fi
        else
            echo "Android SDK build-tools not found. Set ANDROID_HOME or ANDROID_SDK_ROOT."
        fi
    fi
else
    echo "APK not found in: $APK_DIR"
fi
