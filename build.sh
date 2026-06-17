#!/bin/bash
#
# 统一的 APK 构建脚本（bash 版）。
#
# 功能与 build.ps1 对齐：
#   - 通过 -o 切换 Debug/Release
#   - Debug 注入 -PAUTH_ISSUER_DEBUG/-PAPI_URI_DEBUG
#   - Release 注入 -PAUTH_ISSUER/-PAPI_URI
#   - 可选 -CA 走 zipalign + apksigner 签名
#   - 优先解析 local.properties 中的 sdk.dir，再 fallback 到 $ANDROID_HOME 等
#   - build-tools 优先匹配 compileSdk (36.x.x)，再 fallback 到任意有 zipalign 的版本
#
# 用法:
#   ./build.sh [-o Release|Debug] [-n apk_name] [-a AUTH_ISSUER] [-c CLIENT_ID] [-I API_URI] [-CA jks@alias]
#
# 默认值:
#   -o Debug
#   -n xypasswordbook_debug
#   -a https://auth-test.mksword.com
#   -c password_book_app
#   -I https://api-test.mksword.com

set -u

# ============================================================
# 默认值
# ============================================================
BUILD_MODE="Debug"
APK_NAME="xypasswordbook_debug"
AUTH_ISSUER="https://auth-test.mksword.com"
CLIENT_ID="password_book_app"
API_URI="https://api-test.mksword.com"
CA_ARG=""

# ============================================================
# 参数解析
# ============================================================
while getopts "o:n:a:c:I:CA:h" opt; do
  case $opt in
    o) BUILD_MODE="$OPTARG" ;;
    n) APK_NAME="$OPTARG" ;;
    a) AUTH_ISSUER="$OPTARG" ;;
    c) CLIENT_ID="$OPTARG" ;;
    I) API_URI="$OPTARG" ;;
    CA) CA_ARG="$OPTARG" ;;
    h)
       echo "Usage: $0 -o <Release|Debug> -n <apk_name> -a <AUTH_ISSUER> -c <CLIENT_ID> -I <API_URI> -CA <jks_path@ks-key-alias>"
       echo "  -o   Build mode (Release/Debug, default: Debug)"
       echo "  -n   APK filename without extension (default: xypasswordbook_debug)"
       echo "  -a   AUTH_ISSUER URL"
       echo "  -c   CLIENT_ID"
       echo "  -I   API_URI URL"
       echo "  -CA  jks_path@ks-key-alias"
       exit 0 ;;
    *) echo "Invalid option: -$opt" >&2; exit 1 ;;
  esac
done

# 规范化 BUILD_MODE（与 build.ps1 的 $mode 逻辑一致）
if [ "$BUILD_MODE" = "Release" ]; then
    MODE="Release"
else
    MODE="Debug"
fi

# Debug/Release 参数名切换（与 build.ps1 的 $authIssuerProp/$apiUriProp 对齐）
if [ "$MODE" = "Release" ]; then
    AUTH_ISSUER_PROP="-PAUTH_ISSUER=$AUTH_ISSUER"
    API_URI_PROP="-PAPI_URI=$API_URI"
else
    AUTH_ISSUER_PROP="-PAUTH_ISSUER_DEBUG=$AUTH_ISSUER"
    API_URI_PROP="-PAPI_URI_DEBUG=$API_URI"
fi

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

# ============================================================
# Gradle 构建
# ============================================================
echo "Build mode: $MODE"
echo "APK name: $APK_NAME"

chmod +x gradlew
./gradlew clean "assemble$MODE" --no-daemon \
    "$AUTH_ISSUER_PROP" \
    "-PCLIENT_ID=$CLIENT_ID" \
    "$API_URI_PROP"

GRADLE_EXIT=$?
if [ $GRADLE_EXIT -ne 0 ]; then
    echo "Gradle build failed (exit: $GRADLE_EXIT)" >&2
    exit $GRADLE_EXIT
fi

# ============================================================
# APK 重命名
# ============================================================
APK_DIR="$PROJECT_ROOT/app/build/outputs/apk/$MODE"
BUILT_APK=$(ls "$APK_DIR"/*.apk 2>/dev/null | head -1)
DEST_APK="$APK_DIR/$APK_NAME-signed.apk"

if [ -z "$BUILT_APK" ]; then
    echo "APK not found in: $APK_DIR"
    exit 1
fi

mv -f "$BUILT_APK" "$DEST_APK"
echo "Output: $DEST_APK"

# ============================================================
# 签名流程（仅在指定 -CA 时执行，与 build.ps1 流程对齐）
# ============================================================
if [ -z "$CA_ARG" ]; then
    exit 0
fi

# 解析 jks_path@alias
if [[ "$CA_ARG" != *@* ]]; then
    echo "Invalid -CA format. Use: jks_path@ks-key-alias" >&2
    exit 1
fi
JKS_PATH="${CA_ARG%%@*}"
ALIAS="${CA_ARG#*@}"

# 解析 compileSdk（与 build.ps1 一致：从 app/build.gradle.kts 的 compileSdk { ... } 块内读 minSdk = N）
COMPILE_SDK="36"
BUILD_GRADLE="$PROJECT_ROOT/app/build.gradle.kts"
if [ -f "$BUILD_GRADLE" ]; then
    IN_BLOCK=0
    while IFS= read -r line; do
        if [[ "$line" =~ compileSdk[[:space:]]*\{ ]]; then
            IN_BLOCK=1
            continue
        fi
        if [ "$IN_BLOCK" -eq 1 ]; then
            if [[ "$line" =~ minSdk[[:space:]]*=[[:space:]]*([0-9]+) ]]; then
                COMPILE_SDK="${BASH_REMATCH[1]}"
                break
            fi
            # 块结束（单独的 }）
            TRIMMED=$(echo "$line" | tr -d '[:space:]')
            if [ "$TRIMMED" = "}" ]; then
                IN_BLOCK=0
            fi
        fi
    done < "$BUILD_GRADLE"
fi

# 解析 SDK 路径：local.properties > $ANDROID_HOME > $ANDROID_SDK_ROOT > 常见路径
SDK_DIR=""
LOCAL_PROPS="$PROJECT_ROOT/local.properties"
if [ -f "$LOCAL_PROPS" ]; then
    LINE=$(grep -E '^[[:space:]]*sdk\.dir' "$LOCAL_PROPS" | head -1 || true)
    if [ -n "$LINE" ]; then
        RAW="${LINE#*=}"
        RAW=$(echo "$RAW" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
        # 反转义：将 \\ → \（与 build.ps1 的 .Replace 顺序对齐）
        RAW="${RAW//\\\\/\\}"
        if [ -n "$RAW" ] && [ -d "$RAW" ]; then
            SDK_DIR="$RAW"
        fi
    fi
fi

if [ -z "$SDK_DIR" ]; then
    for base in "$ANDROID_HOME" "$ANDROID_SDK_ROOT" \
                "$HOME/Android/Sdk" \
                "/usr/local/android-sdk" \
                "/opt/android-sdk"; do
        if [ -n "$base" ] && [ -d "$base" ]; then
            SDK_DIR="$base"
            break
        fi
    done
fi

# 选择 build-tools：优先 ${COMPILE_SDK}.x.x，再 fallback 到任意有 zipalign 的版本
BUILD_TOOLS_DIR=""
BT_BASE="$SDK_DIR/build-tools"
if [ -n "$SDK_DIR" ] && [ -d "$BT_BASE" ]; then
    # 优先：与 compileSdk 同主版本
    CANDIDATES=$(ls -d "$BT_BASE/${COMPILE_SDK}."* 2>/dev/null | sort -V)
    for bt in $CANDIDATES; do
        if [ -x "$bt/zipalign" ]; then
            BUILD_TOOLS_DIR="$bt"
            break
        fi
    done
    # fallback：任意有 zipalign 的版本（取最新）
    if [ -z "$BUILD_TOOLS_DIR" ]; then
        ALL_BT=$(ls -d "$BT_BASE"/*/ 2>/dev/null | sort -Vr)
        for bt in $ALL_BT; do
            if [ -x "${bt}zipalign" ]; then
                BUILD_TOOLS_DIR="${bt%/}"
                break
            fi
        done
    fi
fi

echo "sdk dir: $SDK_DIR"
echo "compileSdk: $COMPILE_SDK"
echo "expected bt: $BT_BASE/${COMPILE_SDK}.x.x"
echo "jks path: $JKS_PATH"
echo "alias: $ALIAS"
echo "build-tools dir: $BUILD_TOOLS_DIR"
echo "zipalign: $BUILD_TOOLS_DIR/zipalign"
echo "apksigner: $BUILD_TOOLS_DIR/apksigner"

if [ -z "$BUILD_TOOLS_DIR" ]; then
    echo "Android SDK build-tools not found." >&2
    exit 1
fi

ZIPALIGN="$BUILD_TOOLS_DIR/zipalign"
APKSIGNER="$BUILD_TOOLS_DIR/apksigner"
ALIGNED_APK="$APK_DIR/$APK_NAME-aligned.apk"

echo "zipalign: $ZIPALIGN"
"$ZIPALIGN" -v 4 "$DEST_APK" "$ALIGNED_APK"
if [ $? -ne 0 ]; then
    echo "zipalign failed." >&2
    exit 1
fi

echo "apksigner sign with $JKS_PATH"
"$APKSIGNER" sign --ks "$JKS_PATH" --ks-key-alias "$ALIAS" --out "$DEST_APK" "$ALIGNED_APK"
if [ $? -ne 0 ]; then
    echo "apksigner sign failed." >&2
    rm -f "$ALIGNED_APK"
    exit 1
fi

rm -f "$ALIGNED_APK"
echo "Signed successfully."
echo "Output: $DEST_APK"