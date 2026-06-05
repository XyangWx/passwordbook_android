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
        # Format: --ks <path> --ks-key-alias <alias>@<password>  or  --ks <path> --ks-key-alias <alias>@'<password with @>'
        $ksIdx = $CA.IndexOf('--ks')
        $aliasIdx = $CA.IndexOf('--ks-key-alias')
        if ($ksIdx -ge 0 -and $aliasIdx -gt $ksIdx) {
            $ksPart = $CA.Substring($ksIdx + 4, $aliasIdx - $ksIdx - 4).Trim()
            $aliasPart = $CA.Substring($aliasIdx + 16).Trim()

            $aliasAtIdx = $aliasPart.IndexOf('@')
            if ($aliasAtIdx -gt 0) {
                $jksPath = $ksPart
                $alias = $aliasPart.Substring(0, $aliasAtIdx)
                $jksPwdRaw = $aliasPart.Substring($aliasAtIdx + 1)
                if ($jksPwdRaw.StartsWith("'") -and $jksPwdRaw.EndsWith("'")) {
                    $jksPwd = $jksPwdRaw.Substring(1, $jksPwdRaw.Length - 2)
                } else {
                    $jksPwd = $jksPwdRaw
                }
            } else {
                $jksPath = $null
                Write-Host "Invalid -CA format. Use: --ks <path> --ks-key-alias <alias>@<password>"
            }
        } else {
            $jksPath = $null
            Write-Host "Invalid -CA format. Use: --ks <path> --ks-key-alias <alias>@<password>"
        }

        if ($jksPath) {
            # Find Android SDK build-tools (zipalign + apksigner)
            $sdkBuildTools = $null
            $searchBases = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, "C:\\Android\\Sdk", "C:\\Users\\XuYang\\AppData\\Local\\Android\\Sdk", "C:\\Program Files\\Android\\Sdk")
            foreach ($base in $searchBases) {
                if ($base -and (Test-Path $base)) {
                    $btDir = Join-Path $base 'build-tools'
                    if (Test-Path $btDir) {
                        $latest = Get-ChildItem $btDir -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
                        if ($latest) {
                            $candidate = Join-Path $latest.FullName 'zipalign.exe'
                            if (Test-Path $candidate) { $sdkBuildTools = $latest.FullName; break }
                        }
                    }
                }
            }

            Write-Host "jks path: $jksPath"
            Write-Host "alias: $alias"
            Write-Host "password: $jksPwd"
            Write-Host "build-tools: $sdkBuildTools"

            if ($sdkBuildTools) {
                $zipalign = Join-Path $sdkBuildTools 'zipalign.exe'
                $apksigner = Join-Path $sdkBuildTools 'apksigner.bat'
                $alignedApk = Join-Path $apkDir "$n-aligned.apk"

                Write-Host "zipalign: $zipalign"
                & $zipalign @('-v', '4', $destApk, $alignedApk)
                if ($LASTEXITCODE -ne 0) { Write-Host "zipalign failed." }
                else {
                    Write-Host "apksigner sign with $jksPath"
                    & $apksigner @('sign', '--ks', $jksPath, '--ks-key-alias', $alias, '--ks-pass', "pass:$jksPwd", '--out', $destApk, $alignedApk)
                    if ($LASTEXITCODE -eq 0) {
                        Remove-Item $alignedApk -Force -ErrorAction SilentlyContinue
                        Write-Host "Signed successfully."
                    } else { Write-Host "apksigner sign failed." }
                }
            } else {
                Write-Host "Android SDK build-tools not found. Set ANDROID_HOME or ANDROID_SDK_ROOT."
            }
        }
    }
} else {
    Write-Host "APK not found in: $apkDir"
}
