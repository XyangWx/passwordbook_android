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
    "-PAUTH_ISSUER=*** -PCLIENT_ID=$c",
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
        # Format: jks_path@ks-key-alias
        $atIdx = $CA.IndexOf('@')
        if ($atIdx -gt 0) {
            $jksPath = $CA.Substring(0, $atIdx)
            $alias = $CA.Substring($atIdx + 1)
        } else {
            $jksPath = $null
            Write-Host "Invalid -CA format. Use: jks_path@ks-key-alias"
        }

        if ($jksPath) {
            # Find Android SDK: check env vars first, then local.properties
            $sdkDir = $null
            if ($env:ANDROID_HOME) { $sdkDir = $env:ANDROID_HOME }
            elseif ($env:ANDROID_SDK_ROOT) { $sdkDir = $env:ANDROID_SDK_ROOT }
            else {
                $lp = Join-Path $projectRoot 'local.properties'
                if (Test-Path $lp) {
                    $content = Get-Content $lp -Raw
                    if ($content -match 'sdk\.dir\s*=\s*(.+)') {
                        $sdkDir = $Matches[1].Trim()
                    }
                }
            }

            # Find build-tools version from project
            $buildToolsVersion = $null
            $btSearch = @(
                (Join-Path $projectRoot 'build.gradle.kts'),
                (Join-Path $projectRoot 'app\build.gradle.kts'),
                (Join-Path $projectRoot 'gradle\libs.versions.toml')
            )
            foreach ($f in $btSearch) {
                if (Test-Path $f) {
                    $content = Get-Content $f -Raw
                    if ($content -match 'buildToolsVersion["']?\s*["']?([0-9]+\.[0-9]+)') {
                        $buildToolsVersion = $Matches[1].Trim()
                        break
                    }
                    if ($content -match '"android\.build\.tools"\s*["']:\s*["']([^"']+)"') {
                        $buildToolsVersion = $Matches[1].Trim()
                        break
                    }
                }
            }

            # Find zipalign/apksigner
            $zipalign = $null
            $apksignerBat = $null
            if ($sdkDir) {
                if ($buildToolsVersion) {
                    $btDir = Join-Path $sdkDir "build-tools\$buildToolsVersion"
                    if (Test-Path $btDir) {
                        $zipalign = Join-Path $btDir 'zipalign.exe'
                        $apksignerBat = Join-Path $btDir 'apksigner.bat'
                        if (-not (Test-Path $zipalign)) { $zipalign = $null }
                        if (-not (Test-Path $apksignerBat)) { $apksignerBat = $null }
                    }
                }
                if (-not $zipalign) {
                    $btBase = Join-Path $sdkDir 'build-tools'
                    if (Test-Path $btBase) {
                        $latest = Get-ChildItem $btBase -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
                        if ($latest) {
                            $candidate = Join-Path $latest.FullName 'zipalign.exe'
                            if (Test-Path $candidate) {
                                $zipalign = $candidate
                                $apksignerBat = Join-Path $latest.FullName 'apksigner.bat'
                            }
                        }
                    }
                }
            }

            Write-Host "jks path: $jksPath"
            Write-Host "alias: $alias"
            Write-Host "sdk dir: $sdkDir"
            Write-Host "build-tools: $buildToolsVersion"
            Write-Host "zipalign: $zipalign"
            Write-Host "apksigner: $apksignerBat"

            if ($zipalign -and $apksignerBat) {
                $alignedApk = Join-Path $apkDir "$n-aligned.apk"

                Write-Host "Running zipalign..."
                & $zipalign @('-v', '4', $destApk, $alignedApk)
                if ($LASTEXITCODE -ne 0) { Write-Host "zipalign failed." }
                else {
                    Write-Host "Running apksigner..."
                    & $apksignerBat @('sign', '--ks', $jksPath, '--ks-key-alias', $alias, '--out', $destApk, $alignedApk)
                    if ($LASTEXITCODE -eq 0) {
                        Remove-Item $alignedApk -Force -ErrorAction SilentlyContinue
                        Write-Host "Signed successfully."
                    } else { Write-Host "apksigner sign failed." }
                }
            } else {
                Write-Host "Android SDK build-tools not found. Set ANDROID_HOME or add sdk.dir to local.properties."
            }
        }
    }
} else {
    Write-Host "APK not found in: $apkDir"
}
