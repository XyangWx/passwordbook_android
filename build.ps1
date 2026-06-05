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
$javaExe = if ($env:JAVA_HOME) { Join-Path $env.JAVA_HOME 'bin\java.exe' } else { 'java.exe' }

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
                $props = Get-Content $localProps | Where-Object { $_ -match 'sdk.dir' }
                if ($props) {
                    $parts = $props.Split('=', 2)
                    if ($parts.Length -ge 2) {
                        $raw = $parts[1].Trim()
                        $raw = $raw.Replace("\:", ":")
                        $raw = $raw.Replace("\\", "\")
                        if ($raw -and (Test-Path $raw)) { $sdkDir = $raw }
                    }
                }
            }
            if (-not $sdkDir) {
                foreach ($base in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, "C:\Android\Sdk", "C:\Users\XuYang\AppData\Local\Android\Sdk", "C:\Program Files\Android\Sdk")) {
                    if ($base -and (Test-Path $base)) { $sdkDir = $base; break }
                }
            }

            $compileSdk = '36'
            $buildGradle = Join-Path $projectRoot 'app\build.gradle.kts'
            if (Test-Path $buildGradle) {
                $lines = Get-Content $buildGradle
                $inBlock = $false
                foreach ($line in $lines) {
                    if ($line -imatch 'compileSdk\s*\{') { $inBlock = $true; continue }
                    if ($inBlock) {
                        if ($line -imatch 'minSdk\s*=\s*(\d+)') {
                            $compileSdk = $matches[1]; break
                        }
                        if ($line.Trim() -eq '}') { $inBlock = $false }
                    }
                }
            }

            $buildToolsDir = $null
            $buildToolsVersion = $null
            $btBase = Join-Path $sdkDir 'build-tools'

            if ($sdkDir -and (Test-Path $btBase)) {
                $candidates = @()
                $allBt = Get-ChildItem $btBase -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending
                foreach ($bt in $allBt) {
                    if ($bt.Name -match "^$compileSdk\.") {
                        $z = Join-Path $bt.FullName 'zipalign.exe'
                        if (Test-Path $z) { $candidates += $bt.FullName }
                    }
                }
                if (-not $candidates.Count) {
                    foreach ($bt in $allBt) {
                        $z = Join-Path $bt.FullName 'zipalign.exe'
                        if (Test-Path $z) { $candidates += $bt.FullName; break }
                    }
                }
                if ($candidates.Count) { $buildToolsDir = $candidates[0]; $buildToolsVersion = (Split-Path $buildToolsDir -Leaf) }
            }

            Write-Host "sdk dir: $sdkDir"
            Write-Host "compileSdk: $compileSdk"
            Write-Host "expected bt: $btBase\$compileSdk.x.x"
            Write-Host "jks path: $jksPath"
            Write-Host "alias: $alias"
            Write-Host "build-tools dir: $buildToolsDir"
            Write-Host "zipalign: $(Join-Path $buildToolsDir 'zipalign.exe')"
            Write-Host "apksigner: $(Join-Path $buildToolsDir 'apksigner.bat')"

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
                        Write-Host "Output: $destApk"
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