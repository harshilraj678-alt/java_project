$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    mvn clean package
    if ($LASTEXITCODE -ne 0) { throw "Maven package failed with exit code $LASTEXITCODE" }
    $jar = Join-Path $root "target\earthquakeready-1.0.0.jar"
    if (-not (Test-Path $jar)) { throw "Executable JAR not found: $jar" }
    $dest = Join-Path $root "dist"
    $stage = Join-Path $root "target\jpackage-input"
    New-Item -ItemType Directory -Force $dest | Out-Null
    New-Item -ItemType Directory -Force $stage | Out-Null
    Copy-Item -LiteralPath $jar -Destination $stage -Force
    if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
        throw "jpackage is not on PATH. Use a JDK installation and add its bin directory to PATH."
    }
    jpackage --type app-image --name EarthquakeReady --input $stage `
        --dest $dest --main-jar "earthquakeready-1.0.0.jar" `
        --main-class "com.earthquakeready.app.EarthquakeReadyApp"
    if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE" }
}
finally {
    Pop-Location
}
