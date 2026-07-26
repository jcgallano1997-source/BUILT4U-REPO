# Built4U POS — local backend runner.
# Boots Spring Boot against the BUILT4U schema on the Oracle ADB (via wallet mTLS).
#
# Usage (from your own terminal, in the backend/ folder):
#   .\run-local.ps1
#
# It will prompt for the BUILT4U DB password (or reuse $env:DB_PASSWORD if set).
# The app serves on http://localhost:8083  ->  smoke test: http://localhost:8083/api/ping

$ErrorActionPreference = 'Stop'

# --- Pin JDK 21 (the project's target; avoids JDK-25 quirks) ---
$jdk21 = 'C:\Program Files\Java\jdk-21.0.11'
if (Test-Path $jdk21) { $env:JAVA_HOME = $jdk21 }
Write-Host "JAVA_HOME = $env:JAVA_HOME"

# --- Wallet (mTLS) lives alongside this script ---
$env:TNS_ADMIN = Join-Path $PSScriptRoot 'wallet'
if (-not (Test-Path (Join-Path $env:TNS_ADMIN 'tnsnames.ora'))) {
    throw "Wallet not found at $env:TNS_ADMIN (expected tnsnames.ora + cwallet.sso)."
}
Write-Host "TNS_ADMIN = $env:TNS_ADMIN"

# --- DB credentials ---
$env:DB_USERNAME = 'built4u'
if (-not $env:DB_PASSWORD) {
    $sec = Read-Host "BUILT4U DB password" -AsSecureString
    $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))
}

# --- Run ---
Set-Location $PSScriptRoot
& .\mvnw.cmd spring-boot:run
