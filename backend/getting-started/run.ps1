$envFile = Join-Path $PSScriptRoot "config/.env"

if (!(Test-Path $envFile)) {
    Write-Error "Missing config/.env. Copy config/.env.example first."
    exit 1
}

Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()

        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

& "$PSScriptRoot/target/server/bin/standalone.bat"