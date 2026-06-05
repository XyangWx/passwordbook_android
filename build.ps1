param(
    [string]$o = 'Debug',
    [string]$n = 'xypasswordbook_debug',
    [string]$a = 'https://auth-test.mksword.com',
    [string]$c = 'password_book_app',
    [string]$I = 'https://api-test.mksword.com',
    [string]$CA = ''
)

$mode = if ($o -eq 'Release') { 'Release' } else { 'Debug' }
$projectRoot = $PSScriptRoot
$wrapperJar = Join-Path $projectRoot 'gradle\wrapper\gradle-wrapper.jar'
$javaExe = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java.exe' }

$gradleArgs = @(
    '-ea', '-Xmx64m', '-Xms64m',
    '-Dorg.gradle.appname=gradlew',
    '-classpath', $wrapperJar,
    'org.gradle.wrapper.GradleWrapperMain',
    'clean', "assemble$mode",
    '--no-daemon',
    "-PAUTH_ISSUER=*** -PCLIENT_ID=$c",
    "-PAPI_URI=$I"
)

Write-Host "Build mode: $mode"
Write-Host "APK name: $n"

& $javaExe $gradleArgs

$apkDir = Join-Path $projectRoot "app\build\outputs\apk\$mode"
$builtApk = Get-ChildItem -Path $apkDir -Filter "*.apk" -File | Select-Object -First 1
$destApk = Join-Path $apkDir "$n.apk"

if ($builtApk) {
    Rename-Item -Path $builtApk.FullName -NewName "$n.apk" -Force
    Write-Host "Output: $destApk"

    if ($CA -ne '') {
        # Split at last @ (password may contain @)
        $atIndex = $CA.LastIndexOf('@')
        if ($atIndex -gt 0) {
            $jksPath = $CA.Substring(0, $atIndex)
            $jksPwdRaw = $CA.Substring($atIndex + 1)
            # Unquote if wrapped in single quotes
            if ($jksPwdRaw.StartsWith("'") -and $jksPwdRaw.EndsWith("'")) {
                $jksPwd = $jksPwdRaw.Substring(1, $jksPwdRaw.Length - 2)
            } else {
                $jksPwd = $jksPwdRaw
            }
            $signedApk = $destApk
            # Find jarsigner: try JAVA_HOME first, then search PATH
            if ($env:JAVA_HOME) {
                $jarsigner = Join-Path $env:JAVA_HOME 'bin\jarsigner.exe'
            } else {
                $jarsigner = Get-Command jarsigner.exe -ErrorAction SilentlyContinue | Select-Object -First 1
                if ($jarsigner) {
                    $jarsigner = $jarsigner.Source
                } else {
                    $jarsigner = 'jarsigner.exe'
                }
            }
            Write-Host "Signing: $signedApk with $jksPath"
            & $jarsigner @('-keystore', $jksPath, '-storepass', $jksPwd, '-signedjar', $signedApk, $signedApk, $jksPath)
            if ($LASTEXITCODE -eq 0) {
                Write-Host "Signed successfully."
            } else {
                Write-Host "Signing failed."
            }
        } else {
            Write-Host "Invalid -CA format. Use: path@password or path@'password with @'"
        }
    }
} else {
    Write-Host "APK not found in: $apkDir"
}
