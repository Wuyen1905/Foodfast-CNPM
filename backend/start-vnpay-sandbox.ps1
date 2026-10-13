param(
    [string]$ReturnUrl
)

$ErrorActionPreference = 'Stop'

$secretFile = Join-Path $PSScriptRoot '.env.vnpay.local'
if (-not (Test-Path -LiteralPath $secretFile)) {
    throw "Khong tim thay $secretFile. Hay sao chep .env.vnpay.local.example va dien credential VNPAY."
}

$loaded = @{}
foreach ($rawLine in Get-Content -LiteralPath $secretFile) {
    $line = $rawLine.Trim()
    if (-not $line -or $line.StartsWith('#')) {
        continue
    }

    $parts = $line.Split('=', 2)
    if ($parts.Count -ne 2) {
        throw "Dong cau hinh khong hop le trong .env.vnpay.local: $line"
    }

    $name = $parts[0].Trim()
    $value = $parts[1].Trim()
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    $loaded[$name] = $value
}

# Quick Tunnel thay doi URL sau moi lan chay. Cho phep script khoi dong tong
# truyen URL moi vao bo nho ma khong sua file chua credential.
if ($ReturnUrl) {
    $ReturnUrl = $ReturnUrl.Trim()
    if ($ReturnUrl -notmatch '^https://') {
        throw 'ReturnUrl truyen vao phai la dia chi HTTPS.'
    }

    $loaded['VNPAY_RETURN_URL'] = $ReturnUrl
    [Environment]::SetEnvironmentVariable('VNPAY_RETURN_URL', $ReturnUrl, 'Process')
}

$required = @('VNPAY_TMN_CODE', 'VNPAY_HASH_SECRET', 'VNPAY_PAY_URL', 'VNPAY_RETURN_URL')
foreach ($name in $required) {
    $value = $loaded[$name]
    if (-not $value -or $value -match '^(DIEN_|MA_WEBSITE|CHUOI_BI_MAT)') {
        throw "Bien $name chua duoc dien trong .env.vnpay.local."
    }
}

if ($loaded['VNPAY_RETURN_URL'] -notmatch '^https://') {
    throw 'VNPAY_RETURN_URL phai la dia chi HTTPS de VNPAY co the chuyen huong an toan.'
}

Write-Host 'Da nap cau hinh VNPAY Sandbox. Credential khong duoc in ra man hinh.' -ForegroundColor Green
Write-Host "Return URL: $($loaded['VNPAY_RETURN_URL'])" -ForegroundColor Cyan

& (Join-Path $PSScriptRoot 'start-backend.ps1')
