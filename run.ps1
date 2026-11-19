$ErrorActionPreference = 'Stop'

# Resolve paths from this script so SQLite always opens the project's database.
Push-Location -LiteralPath $PSScriptRoot
try {
    if (-not (Get-Command javac -ErrorAction SilentlyContinue) -or
        -not (Get-Command java -ErrorAction SilentlyContinue)) {
        throw 'Install a JDK and make sure java and javac are available on PATH.'
    }
    if (-not (Test-Path -LiteralPath 'lib/sqlite-jdbc-3.53.4.0.jar')) {
        throw 'Missing SQLite driver: lib/sqlite-jdbc-3.53.4.0.jar. Pull the complete repository.'
    }

    New-Item -ItemType Directory -Path 'bin' -Force | Out-Null
    $sources = @(Get-ChildItem -LiteralPath . -Filter '*.java' -File |
        ForEach-Object { $_.FullName })
    & javac -encoding UTF-8 -cp 'lib/*' -d bin @sources
    if ($LASTEXITCODE -ne 0) {
        throw 'Compilation failed. Fix the errors above before running the application.'
    }

    $classPath = 'bin' + [System.IO.Path]::PathSeparator + 'lib/*'
    & java -cp $classPath Main
    if ($LASTEXITCODE -ne 0) {
        throw 'The application exited with an error. See the output above.'
    }
} finally {
    Pop-Location
}
