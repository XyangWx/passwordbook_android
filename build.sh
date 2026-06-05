#!/bin/bash

BUILD_MODE="Debug"
APK_NAME="xypasswordbook_debug"
AUTH_ISSUER="https://auth-test.mksword.com"
CLIENT_ID="password_book_app"
API_URI="https://api-test.mksword.com"
JKS_ARG=""

while getopts "o:n:a:c:I:C:h" opt; do
  case $opt in
    o) BUILD_MODE="$OPTARG" ;;
    n) APK_NAME="$OPTARG" ;;
    a) AUTH_ISSUER="$OPTARG" ;;
    c) CLIENT_ID="$OPTARG" ;;
    I) API_URI="$OPTARG" ;;
    C) JKS_ARG="$OPTARG" ;;
    h) echo "Usage: $0 -o <Release|Debug> -n <apk_name> -a <AUTH_ISSUER> -c <CLIENT_ID> -I <API_URI> -C <jks_path@password>"
       echo "  -o  Build mode (Release/Debug, default: Debug)"
       echo "  -n  APK filename without extension (default: xypasswordbook_debug)"
       echo "  -a  AUTH_ISSUER URL (default: https://auth-test.mksword.com)"
       echo "  -c  CLIENT_ID (default: password_book_app)"
       echo "  -I  API_URI URL (default: https://api-test.mksword.com)"
       echo "  -C  JKS path and password (format: path@password or path@'password with @')"
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

    if [ -n "$JKS_ARG" ]; then
        # Split at last @ (password can contain @ only if quoted with ')
        if [[ "$JKS_ARG" == *@* ]]; then
            JKS_PATH="${JKS_ARG%%@*}"
            REST="${JKS_ARG#*@}"
            if [[ "$REST" == "'"* ]]; then
                JKS_PWD="${REST:1:${#REST}-2}"
            else
                JKS_PWD="$REST"
            fi
            echo "Signing: $DEST_APK with $JKS_PATH"
            jarsigner -keystore "$JKS_PATH" -storepass "$JKS_PWD" -signedjar "$DEST_APK" "$DEST_APK" "$JKS_PATH" 2>&1
            if [ $? -eq 0 ]; then
                echo "Signed successfully."
            else
                echo "Signing failed."
            fi
        else
            echo "Invalid -C format. Use: path@password or path@'password with @'"
        fi
    fi
else
    echo "APK not found in: $APK_DIR"
fi