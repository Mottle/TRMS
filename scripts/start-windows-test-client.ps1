[CmdletBinding()]
param(
    [switch]$Check
)

$ErrorActionPreference = 'Stop'

if ($env:OS -ne 'Windows_NT') {
    throw 'This launcher must be run from Windows PowerShell, not from WSL.'
}

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$gradleWrapper = Join-Path $repositoryRoot 'gradlew.bat'
$serverHost = '127.0.0.1'
$serverPort = 25565

if (-not (Test-Path -LiteralPath $gradleWrapper)) {
    throw "Gradle Wrapper was not found: $gradleWrapper"
}

if (-not (Get-Command wsl.exe -ErrorAction SilentlyContinue)) {
    throw 'WSL is required because this project resolves Horizon development artifacts from the WSL Maven repository.'
}

function ConvertFrom-WslText {
    param([string]$Value)

    # wsl.exe may emit UTF-16 text with embedded NULs when its output is piped.
    ($Value -replace '\x00', '').Trim()
}

$distribution = @(wsl.exe -l -q | ForEach-Object { ConvertFrom-WslText -Value $_ } | Where-Object { $_ }) | Select-Object -First 1
if (-not $distribution) {
    throw 'No WSL distribution is available.'
}

$wslUser = ConvertFrom-WslText -Value (wsl.exe -d $distribution -- sh -c 'id -un')
if (-not $wslUser) {
    throw "Could not determine the user for WSL distribution '$distribution'."
}

$wslMavenRepository = "\\wsl.localhost\$distribution\home\$wslUser\.m2\repository"
if (-not (Test-Path -LiteralPath $wslMavenRepository)) {
    throw "The WSL Maven repository was not found: $wslMavenRepository"
}

if (-not (Test-NetConnection -ComputerName $serverHost -Port $serverPort -InformationLevel Quiet)) {
    throw "The WSL test server is not reachable at ${serverHost}:$serverPort. Start the Extension server first."
}

if ($Check) {
    Write-Host "TRMS Windows client launcher checks passed for ${serverHost}:$serverPort."
    return
}

Write-Host "Starting the TRMS NeoForge client and quick-connecting to ${serverHost}:$serverPort..."
& $gradleWrapper "-Dmaven.repo.local=$wslMavenRepository" ':mod:runClientQuickPlay' '--console=plain'
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
