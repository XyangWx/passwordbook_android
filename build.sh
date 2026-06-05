#!/bin/bash

# 默认值
BUILD_MODE="Debug"
APK_NAME="xypasswordbook_debug"
AUTH_ISSUER="https://auth-test.mksword.com"
CLIENT_ID="password_book_app"
API_URI="https://api-test.mksword.com"

# 解析参数
while getopts "o:n:a:c:I:h" opt; do
  case $opt in
    o) BUILD_MODE="$OPTARG" ;;
    n) APK_NAME="$OPTARG" ;;
    a) AUTH_ISSUER="$OPTARG" ;;
    c) CLIENT_ID="$OPTARG" ;;
    I) API_URI="$OPTARG" ;;
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

cd "$(dirname "$0")"

chmod +x gradlew

# 清理后构建
./gradlew clean assemble"$BUILD_MODE" \
  -PAUTH_ISSUER="$AUTH_ISSUER" \
  -PCLIENT_ID="$CLIENT_ID" \
  -PAPI_URI="$API_URI" \
  -PAPK_OUTPUT_NAME="$APK_NAME"