$ErrorActionPreference = 'Stop'
$repoRoot = $PSScriptRoot
Push-Location $repoRoot
try {
    $files = Get-ChildItem -Recurse -Filter *.java | Where-Object { $_.FullName.Substring($repoRoot.Length) -notmatch '[\\/](android-app|build|out|releases|\.gradle)[\\/]' } | Select-Object -ExpandProperty FullName
    New-Item -ItemType Directory -Force out | Out-Null
    $argsFile = Join-Path $repoRoot 'out/sources.txt'
    $files | ForEach-Object { '"' + $_.Replace('\', '/') + '"' } | Set-Content -Encoding utf8 $argsFile
    # Windows PowerShell writes a BOM for utf8; javac argument files must omit it.
    [System.IO.File]::WriteAllLines($argsFile, @($files | ForEach-Object { '"' + $_.Replace('\', '/') + '"' }), (New-Object System.Text.UTF8Encoding($false)))
    & javac --release 17 -encoding UTF-8 -d out "@$argsFile"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally { Pop-Location }
