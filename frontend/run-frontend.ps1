# Built4U POS — local frontend runner (Vite dev server on http://localhost:5173).
#
# Usage (from your own terminal, in the frontend/ folder):
#   .\run-frontend.ps1
#
# Requires the backend running on :8083 (see ..\backend\run-local.ps1) — the dev
# server proxies /api there. Log in with:  admin / admin123 / site MAIN.

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (-not (Test-Path 'node_modules')) {
    # @hookform/resolvers lists optional peers (valibot etc.) that trip npm's
    # strict peer resolver — --legacy-peer-deps is the expected install flag.
    Write-Host "Installing dependencies (first run)…"
    npm install --legacy-peer-deps
}

npm run dev
