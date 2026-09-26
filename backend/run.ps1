# Carga backend/.env al entorno y arranca el backend con Maven.
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$envFile = Join-Path $PSScriptRoot ".env"
if (-not (Test-Path $envFile)) {
    Write-Error "No existe backend/.env. Copia los valores de ejemplo y completa tu configuracion de base de datos."
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $index = $line.IndexOf("=")
        $name = $line.Substring(0, $index).Trim()
        $value = $line.Substring($index + 1).Trim()
        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

mvn spring-boot:run
