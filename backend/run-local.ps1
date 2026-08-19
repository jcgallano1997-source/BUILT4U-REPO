# Built4U POS — local backend runner.
# Boots Spring Boot against the BUILT4U schema on the Oracle ADB (via wallet mTLS).
#
# Usage (from your own terminal, in the backend/ folder):
#   .\run-local.ps1
#
# It reads the BUILT4U DB password from (in order): an existing $env:DB_PASSWORD,
# a gitignored backend/.env file (DB_PASSWORD=...), otherwise it prompts.
# The app serves on http://localhost:8083  ->  smoke test: http://localhost:8083/actuator/health

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

# --- Optional gitignored .env (so secrets aren't typed each run) ---
# Format: KEY=VALUE per line (# comments allowed). Only fills vars not already set.
$envFile = Join-Path $PSScriptRoot '.env'
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
            $idx = $line.IndexOf('=')
            $key = $line.Substring(0, $idx).Trim()
            $val = $line.Substring($idx + 1).Trim()
            if ($val.Length -ge 2 -and
                (($val[0] -eq '"' -and $val[-1] -eq '"') -or ($val[0] -eq "'" -and $val[-1] -eq "'"))) {
                $val = $val.Substring(1, $val.Length - 2)
            }
            if ($key -and -not (Test-Path "Env:$key")) { Set-Item -Path "Env:$key" -Value $val }
        }
    }
    Write-Host "Loaded secrets from $envFile"
}

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
