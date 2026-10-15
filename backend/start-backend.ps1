# FoodFast Backend Startup Script
# Automatically frees port 8080 and starts Spring Boot backend

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "FoodFast Backend Startup Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Verify MySQL
Write-Host "[1/4] Checking MySQL84..." -ForegroundColor Yellow
$mysqlService = Get-Service -Name MySQL84 -ErrorAction SilentlyContinue
if (-not $mysqlService) {
    Write-Host "MySQL84 is not installed. Install and configure MySQL before starting FoodFast." -ForegroundColor Red
    exit 1
}
if ($mysqlService.Status -ne 'Running') {
    try {
        Start-Service -Name MySQL84 -ErrorAction Stop
        Write-Host "MySQL84 started." -ForegroundColor Green
    } catch {
        Write-Host "Cannot start MySQL84. Open PowerShell as Administrator once or start it in Windows Services." -ForegroundColor Red
        exit 1
    }
}
Write-Host "MySQL84 is running." -ForegroundColor Green
Write-Host ""

# Step 2: Kill any process using port 8080
Write-Host "[2/4] Checking for processes using port 8080..." -ForegroundColor Yellow
$processes = netstat -ano | findstr :8080 | findstr LISTENING

if ($processes) {
    foreach ($line in $processes) {
        $processId = ($line -split '\s+')[-1]
        if ($processId -match '^\d+$') {
            Write-Host "Killing process $processId on port 8080..." -ForegroundColor Red
            taskkill /PID $processId /F 2>$null
        }
    }
    Write-Host "Port 8080 is now free." -ForegroundColor Green
} else {
    Write-Host "Port 8080 is already free." -ForegroundColor Green
}
Write-Host ""

# Step 3: Compile project
Write-Host "[3/4] Compiling project..." -ForegroundColor Yellow
& mvn clean compile -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!" -ForegroundColor Red
    exit 1
}
Write-Host "Compilation successful." -ForegroundColor Green
Write-Host ""

# Step 4: Start Spring Boot
Write-Host "[4/4] Starting Spring Boot backend on port 8080 with MySQL..." -ForegroundColor Yellow
& mvn spring-boot:run

