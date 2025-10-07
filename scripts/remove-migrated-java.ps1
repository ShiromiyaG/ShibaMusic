param(
    [switch]$DryRun
)

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaRoot = Join-Path $projectRoot "app\src\main\java"
$marker = "// Migrated to Kotlin. Legacy Java implementation removed."

if (-not (Test-Path $javaRoot)) {
    Write-Error "Java source directory not found at '$javaRoot'."
    exit 1
}

$files = Get-ChildItem -Path $javaRoot -Filter *.java -Recurse |
    Where-Object {
        (Select-String -Path $_.FullName -Pattern [regex]::Escape($marker) -SimpleMatch -Quiet)
    }

if (-not $files) {
    Write-Host "No migrated Java files found."
    exit 0
}

Write-Host "Found $($files.Count) migrated Java file(s):" -ForegroundColor Cyan
$files | ForEach-Object { Write-Host " - " $_.FullName }

if ($DryRun) {
    Write-Host "Dry run enabled. No files were deleted." -ForegroundColor Yellow
    exit 0
}

$files | ForEach-Object {
    Remove-Item -Path $_.FullName -Force
    Write-Host "Deleted" $_.FullName -ForegroundColor Green
}

Write-Host "Cleanup complete." -ForegroundColor Green
