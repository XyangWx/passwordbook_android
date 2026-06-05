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
$wrapperJar = Join-Path $projectRoot 'gradle\\wrapper\\gradle-wrapper.jar'
$javaExe = if ($env:JAVA_HOME) { Join-Path $env.JAVA_HOME 'bin\\java.exe' } else { 'java.exe' }

$gradleArgs = @(
    '-ea', '-Xmx64m', '-Xms64m',
    '-Dorg.gradle.appname=gradlew',
    '-classpath', $wrapperJar,
    'org.gradle.wrapper.GradleWrapperMain',
    'clean', "assemble$mode",
    '--no-daemon',
    "-PAUTH_ISSUER=$env:AUTH_ISSUER -PCLIENT_ID=$c",
    "-PAPI_URI=$I"
)

Write-Host "Build mode: $mode"
Write-Host "APK name: $n"

& $javaExe $gradleArgs

$apkDir = Join-Path $projectRoot "app\\build\\outputs\\apk\\$mode"
$builtApk = Get-ChildItem -Path $apkDir -Filter "*.apk" -File | Select-Object -First 1
$destApk = Join-Path $apkDir "$n.apk"

if ($builtApk) {
    Rename-Item -Path $builtApk.FullName -NewName "$n.apk" -Force
    Write-Host "Output: $destApk"

    if ($CA -ne '') {
        $atIdx = $CA.IndexOf('@')
        if ($atIdx -gt 0) {
            $jksPath = $CA.Substring(0, $atIdx)
            $alias = $CA.Substring($atIdx + 1)
        } else {
            $jksPath = $null
            Write-Host "Invalid -CA format. Use: jks_path@ks-key-alias"
        }

        if ($jksPath) {
            $sdkDir = $null
            $localProps = Join-Path $projectRoot 'local.properties'
            if (Test-Path $localProps) {
                $props = Get-Content $localProps | Where-Object { $_ -match 'sdk\.dir\s*=\s*(.+)' }
                if ($props) { $sdkDir = ($props -replace '.*sdk\.dir\s*=\s*', '').Trim() }
            }
            if (-not $sdkDir -or -not (Test-Path $sdkDir)) {
                foreach ($base in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, "C:\Android\Sdk", "C:\Users\XuYang\AppData\Local\Android\Sdk", "C:\Program Files\Android\Sdk")) {
                    if ($base -and (Test-Path $base)) { $sdkDir = $base; break }
                }
            }

            $buildToolsDir = $null
            $buildToolsVersion = $null
            $preferredBtMajor = '36'

            $localProps = Join-Path $projectRoot 'local.properties'
            if (Test-Path $localProps) {
                $btLine = Get-Content $localProps | Where-Object { $_ -match 'build-tools\s*=\s*(.+)' }
                if ($btLine) { $buildToolsVersion = ($btLine -replace '.*build-tools\s*=\s*', '').Trim() }
            }

            if ($sdkDir -and (Test-Path $sdkDir)) {
                $btBase = Join-Path $sdkDir 'build-tools'
                if (Test-Path $btBase) {
                    $candidates = @()
                    if ($buildToolsVersion) {
                        $candidate = Join-Path $btBase $buildToolsVersion
                        if (Test-Path (Join-Path $candidate 'zipalign.exe')) { $candidates += $candidate }
                    }
                    $sdkMatched = Get-ChildItem $btBase -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -match "^$preferredBtMajor\." } | Sort-Object Name -Descending | Select-Object -First 1
                    if ($sdkMatched -and (Test-Path (Join-Path $sdkMatched.FullName 'zipalign.exe'))) { $candidates += $sdkMatched.FullName }
                    $latest = Get-ChildItem $btBase -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
                    if ($latest -and (Test-Path (Join-Path $latest.FullName 'zipalign.exe'))) { $candidates += $latest.FullName }
                    foreach ($c in $candidates) {
                        if (-not $buildToolsDir) { $buildToolsDir = $c; $buildToolsVersion = (Split-Path $c -Leaf) }
                    }
                }
            }

            Write-Host "jks path: $jksPath"
            Write-Host "alias: $alias"
            Write-Host "sdk dir: $sdkDir"
            Write-Host "build-tools: $buildToolsDir"

            if ($buildToolsDir) {
                $zipalign = Join-Path $buildToolsDir 'zipalign.exe'
                $apksigner = Join-Path $buildToolsDir 'apksigner.bat'
                $alignedApk = Join-Path $apkDir "$n-aligned.apk"

                Write-Host "zipalign: $zipalign"
                & $zipalign @('-v', '4', $destApk, $alignedApk)
                if ($LASTEXITCODE -ne 0) { Write-Host "zipalign failed." }
                else {
                    Write-Host "apksigner sign with $jksPath"
                    & $apksigner @('sign', '--ks', $jksPath, '--ks-key-alias', $alias, '--out', $destApk, $alignedApk)
                    if ($LASTEXITCODE -eq 0) {
                        Remove-Item $alignedApk -Force -ErrorAction SilentlyContinue
                        Write-Host "Signed successfully."
                    } else { Write-Host "apksigner sign failed." }
                }
            } else {
                Write-Host "Android SDK build-tools not found."
            }
        }
    }
} else {
    Write-Host "APK not found in: $apkDir"
}
