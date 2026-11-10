param([int]$Port = 8081)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
# Read the same broker configuration that is uploaded to the ESP32.
$firmware = Get-Content -Raw -LiteralPath "$PSScriptRoot/Firmware/IOT_Light_Sensor.ino"
function Read-FirmwareString([string]$name) {
    $match = [regex]::Match($firmware, ('const\s+char\s*\*\s*' + $name + '\s*=\s*"([^"]*)"'))
    if (-not $match.Success) { throw "Cannot read $name from firmware" }
    return $match.Groups[1].Value
}
$portMatch = [regex]::Match($firmware, 'const\s+int\s+mqtt_port\s*=\s*(\d+)')
if (-not $portMatch.Success) { throw 'Cannot read mqtt_port from firmware' }
$portProbe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
try { $portProbe.Start() } catch { throw "Port $Port is occupied. Stop the previous hardware backend or use -Port 8082." } finally { $portProbe.Stop() }
if (Test-Path 'C:/Program Files/Java/jdk-21.0.10') { $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10' }
$env:MAVEN_OPTS = '-Xms32m -Xmx256m'
$env:SERVER_PORT = "$Port"
$env:MQTT_ENABLED = 'true'
$env:MQTT_BROKER_URI = 'tcp://' + (Read-FirmwareString 'mqtt_server') + ':' + $portMatch.Groups[1].Value
$env:MQTT_USERNAME = Read-FirmwareString 'mqtt_user'
$env:MQTT_PASSWORD = Read-FirmwareString 'mqtt_pass'
# Separate local database from any older backend still running on 8080.
$env:DB_URL = 'jdbc:h2:file:./data/hardware'
if (-not $env:JWT_SECRET) { $env:JWT_SECRET = [guid]::NewGuid().ToString() + [guid]::NewGuid().ToString() }
if (-not $env:APP_EMAIL) { $env:APP_EMAIL = 'iot@example.com' }
if (-not $env:APP_PASSWORD) { $env:APP_PASSWORD = 'MatKhauCuaBan' }
$frontendEnv = "VITE_API_BASE_URL=http://localhost:$Port/api`nVITE_ENABLE_DEMO=false`n"
[System.IO.File]::WriteAllText("$PSScriptRoot/Frontend/.env.local", $frontendEnv, [System.Text.UTF8Encoding]::new($false))
Write-Host "Backend: http://localhost:$Port | MQTT: $env:MQTT_BROKER_URI"
Write-Host "Login email: $env:APP_EMAIL (password: APP_PASSWORD; default MatKhauCuaBan)"
Write-Host 'Restart npm run dev in Frontend after starting this script.'
Set-Location "$PSScriptRoot/Backend"
try {
    & .\mvnw.cmd spring-boot:run '-Dspring-boot.run.jvmArguments=-Xms64m -Xmx384m -XX:+UseSerialGC'
    if ($LASTEXITCODE -ne 0) { throw "Backend stopped with exit code $LASTEXITCODE" }
} finally { Set-Location $PSScriptRoot }
