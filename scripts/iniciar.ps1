$ErrorActionPreference = 'Stop'
$projeto = Split-Path -Parent $PSScriptRoot
$arquivoEnv = Join-Path $projeto '.env'

if (-not (Test-Path -LiteralPath $arquivoEnv)) {
    Copy-Item -LiteralPath (Join-Path $projeto '.env.example') -Destination $arquivoEnv
    throw 'Criado .env na pasta do projeto. Edite as credenciais do MySQL e execute novamente.'
}

$permitidas = @('DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASSWORD')
foreach ($linha in [System.IO.File]::ReadAllLines($arquivoEnv)) {
    if ($linha.Trim().Length -eq 0 -or $linha.TrimStart().StartsWith('#')) { continue }
    $partes = $linha.Split(@('='), 2)
    if ($partes.Length -ne 2 -or $partes[0].Trim() -notin $permitidas) {
        throw 'O arquivo .env possui uma configuração inválida.'
    }
    $nome = $partes[0].Trim()
    $valor = $partes[1].Trim()
    [Environment]::SetEnvironmentVariable($nome, $valor, 'Process')
}
if ($env:DB_PASSWORD -eq 'ALTERE_AQUI') {
    throw 'Informe a senha real do MySQL no arquivo .env antes de iniciar.'
}

Push-Location -LiteralPath $projeto
try {
    & mvn liberty:dev
    if ($LASTEXITCODE -ne 0) { throw "O servidor encerrou com código $LASTEXITCODE." }
} finally {
    Pop-Location
    [Environment]::SetEnvironmentVariable('DB_PASSWORD', $null, 'Process')
}
