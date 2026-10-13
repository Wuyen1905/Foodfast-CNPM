param(
    [switch]$SkipFrontend
)

$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$frontendDir = Join-Path $projectRoot 'frontend-web'
$backendDir = Join-Path $projectRoot 'backend'
$vnpayEnvFile = Join-Path $backendDir '.env.vnpay.local'
$backendLauncher = Join-Path $backendDir 'start-vnpay-sandbox.ps1'

function Assert-CommandAvailable {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [string]$InstallHint
    )

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Khong tim thay '$Name'. $InstallHint"
    }
}

function Test-ListeningPort {
    param([int]$Port)

    return [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
}

function Start-FoodFastTerminal {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Title,
        [Parameter(Mandatory = $true)]
        [string]$Command
    )

    $terminalCommand = @"
`$Host.UI.RawUI.WindowTitle = '$Title'
`$ErrorActionPreference = 'Stop'
$Command
"@
    $encodedCommand = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($terminalCommand))

    return Start-Process -FilePath 'powershell.exe' `
        -ArgumentList @('-NoExit', '-EncodedCommand', $encodedCommand) `
        -PassThru
}

Write-Host '==================================================' -ForegroundColor Cyan
Write-Host ' FoodFast + VNPay Sandbox - khoi dong tu dong' -ForegroundColor Cyan
Write-Host '==================================================' -ForegroundColor Cyan

Assert-CommandAvailable -Name 'cloudflared' -InstallHint 'Hay cai Cloudflare Tunnel truoc.'
Assert-CommandAvailable -Name 'npm' -InstallHint 'Hay cai Node.js/npm truoc.'
Assert-CommandAvailable -Name 'mvn' -InstallHint 'Hay cai Maven va them vao PATH truoc.'

if (-not (Test-Path -LiteralPath $vnpayEnvFile)) {
    throw "Khong tim thay $vnpayEnvFile. Hay tao file nay tu .env.vnpay.local.example va dien credential VNPay mot lan."
}

if (-not (Test-Path -LiteralPath $backendLauncher)) {
    throw "Khong tim thay script backend: $backendLauncher"
}

if (-not $SkipFrontend) {
    if (Test-ListeningPort -Port 5173) {
        Write-Host '[1/3] Frontend da chay tai cong 5173.' -ForegroundColor Green
    } else {
        Write-Host '[1/3] Dang mo frontend...' -ForegroundColor Yellow
        $frontendCommand = @"
Set-Location -LiteralPath '$frontendDir'
if (-not (Test-Path -LiteralPath 'node_modules')) {
    npm install --legacy-peer-deps
    if (`$LASTEXITCODE -ne 0) { throw 'Khong the cai dependency frontend.' }
}
npm run dev:fast
"@
        $null = Start-FoodFastTerminal -Title 'FoodFast Frontend - 5173' -Command $frontendCommand
    }
} else {
    Write-Host '[1/3] Bo qua khoi dong frontend theo tuy chon -SkipFrontend.' -ForegroundColor DarkYellow
}

Write-Host '[2/3] Dang tao Cloudflare Quick Tunnel...' -ForegroundColor Yellow
$runId = "foodfast-cloudflared-$PID"
$tunnelOutLog = Join-Path $env:TEMP "$runId.out.log"
$tunnelErrorLog = Join-Path $env:TEMP "$runId.err.log"

Remove-Item -LiteralPath $tunnelOutLog, $tunnelErrorLog -Force -ErrorAction SilentlyContinue

$tunnelProcess = Start-Process -FilePath (Get-Command cloudflared).Source `
    -ArgumentList @('tunnel', '--url', 'http://localhost:8080', '--no-autoupdate') `
    -RedirectStandardOutput $tunnelOutLog `
    -RedirectStandardError $tunnelErrorLog `
    -WindowStyle Hidden `
    -PassThru

$tunnelUrl = $null
$deadline = (Get-Date).AddSeconds(60)

while (-not $tunnelUrl -and (Get-Date) -lt $deadline) {
    if ($tunnelProcess.HasExited) {
        $details = @(
            (Get-Content -LiteralPath $tunnelOutLog -Raw -ErrorAction SilentlyContinue),
            (Get-Content -LiteralPath $tunnelErrorLog -Raw -ErrorAction SilentlyContinue)
        ) -join [Environment]::NewLine
        throw "Cloudflare Tunnel da dung truoc khi tao duoc URL.`n$details"
    }

    $tunnelOutput = @(
        (Get-Content -LiteralPath $tunnelOutLog -Raw -ErrorAction SilentlyContinue),
        (Get-Content -LiteralPath $tunnelErrorLog -Raw -ErrorAction SilentlyContinue)
    ) -join [Environment]::NewLine

    $urlMatch = [regex]::Match($tunnelOutput, 'https://[a-z0-9-]+\.trycloudflare\.com')
    if ($urlMatch.Success) {
        $tunnelUrl = $urlMatch.Value.TrimEnd('/')
        break
    }

    Start-Sleep -Milliseconds 300
}

if (-not $tunnelUrl) {
    Stop-Process -Id $tunnelProcess.Id -Force -ErrorAction SilentlyContinue
    throw 'Qua 60 giay nhung khong lay duoc URL Quick Tunnel. Hay kiem tra mang va thu lai.'
}

$returnUrl = "$tunnelUrl/api/payment/vnpay/browser-return"
$ipnUrl = "$tunnelUrl/api/payment/vnpay/ipn"

Write-Host "      Tunnel: $tunnelUrl" -ForegroundColor Green
Write-Host "      Return: $returnUrl" -ForegroundColor Cyan
Write-Host "      IPN:    $ipnUrl" -ForegroundColor Cyan

Write-Host '[3/3] Dang mo backend voi VNPay da bat...' -ForegroundColor Yellow
$backendCommand = @"
Set-Location -LiteralPath '$backendDir'
& '.\start-vnpay-sandbox.ps1' -ReturnUrl '$returnUrl'
"@
$null = Start-FoodFastTerminal -Title 'FoodFast Backend + VNPay - 8080' -Command $backendCommand

Write-Host ''
Write-Host 'Da khoi dong FoodFast:' -ForegroundColor Green
Write-Host '  Website:      http://localhost:5173' -ForegroundColor White
Write-Host '  Backend:      http://localhost:8080/api/health' -ForegroundColor White
Write-Host '  VNPay status: http://localhost:8080/api/payment/vnpay/status' -ForegroundColor White
Write-Host ''
Write-Host 'Hay GIU terminal nay mo de tunnel VNPay tiep tuc hoat dong.' -ForegroundColor Yellow
Write-Host 'Nhan Ctrl+C de dung tunnel khi khong con test VNPay.' -ForegroundColor Yellow
Write-Host 'Neu VNPay Merchant yeu cau khai bao IPN, dung URL IPN duoc in o tren.' -ForegroundColor DarkYellow

try {
    Wait-Process -Id $tunnelProcess.Id
} finally {
    if (-not $tunnelProcess.HasExited) {
        Stop-Process -Id $tunnelProcess.Id -Force -ErrorAction SilentlyContinue
    }
    Remove-Item -LiteralPath $tunnelOutLog, $tunnelErrorLog -Force -ErrorAction SilentlyContinue
}
