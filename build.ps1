param([string]$o='Debug', [string]$n='xypasswordbook_debug', [string]$a='https://auth-test.mksword.com', [string]$c='password_book_app', [string]$I='https://api-test.mksword.com')

$mode = if ($o -eq 'Release') { 'Release' } else { 'Debug' }

Set-Location $PSScriptRoot
& '.\gradlew.bat' clean "assemble$mode" --no-daemon -PAUTH_ISSUER="$a" -PCLIENT_ID=$c -PAPI_URI=$I -PAPK_OUTPUT_NAME=$n
