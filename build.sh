#!/bin/bash

BUILD_MODE="Debug"
APK_NAME="xypasswordbook_debug"
AUTH_ISSUER="http...sh"
  -a) AUTH_ISSUER="$OPTARG" ;;
  -c) CLIENT_ID="$OPTARG" ;;
  -I) API_URI="$OPTARG" ;;
  h) echo "Usage: $0 -o <Release|Debug> -n <apk_name> -a <AUTH_ISSUER> -c <CLIENT_ID> -I <API_URI>"
     echo "  -o  Build mode (Release/Debug, default: Debug)"
     echo "  -n  APK filename without extension (default: xypasswordbook_debug)"
     echo "  -a  AUTH_ISSUER URL (default: https://auth-test.mksword.com)"
     echo "  -c  CLIENT_ID (default: password_book_app)"
     echo "  -I  API_URI URL (default: https://api-test.mksword.com)"
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

# Rename APK (glob pattern to match app-release-unsigned.apk etc.)
APK_DIR="$PROJECT_ROOT/app/build/outputs/apk/$BUILD_MODE"
BUILT_APK=$(ls "$APK_DIR"/*.apk 2>/dev/null | head -1)
DEST_APK="$APK_DIR/$APK_NAME.apk"

if [ -n "$BUILT_APK" ]; then
    mv -f "$BUILT_APK" "$DEST_APK"
    echo "Output: $DEST_APK"
else
    echo "APK not found in: $APK_DIR"
fi