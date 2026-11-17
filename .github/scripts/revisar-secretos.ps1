$ErrorActionPreference = 'Stop'
$reglas = @{
    'Clave de proveedor' = '(?i)(?:sk-(?:proj-|svcacct-)?[a-z0-9_-]{20,}|gh[pousr]_[a-z0-9]{20,}|github_pat_[a-z0-9_]{20,}|cfat_[a-z0-9_-]{20,}|AIza[a-z0-9_-]{30,})'
    'JWT' = 'eyJ[a-zA-Z0-9_-]{15,}\.[a-zA-Z0-9_-]{15,}\.[a-zA-Z0-9_-]{15,}'
    'Clave privada' = '-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'
}
$fallos = 0
$revisados = 0
$objetos = git rev-list --objects --all
if ($LASTEXITCODE -ne 0) { throw 'No se pudo leer el historial.' }
foreach ($fila in $objetos) {
    $partes = $fila -split ' ', 2
    if ($partes.Length -lt 2) { continue }
    $tipo = git cat-file -t $partes[0]
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo identificar un objeto.' }
    if ($tipo -ne 'blob') { continue }
    $revisados++
    if ($partes[1] -match '(?i)(\.jks$|\.keystore$|\.p12$|\.dpapi$|(^|/)auth\.json$|(^|/)\.env($|\.)|pulse\.cifrado$)') {
        Write-Output "Nombre de archivo sensible: $($partes[1])"
        $fallos++
    }
    $contenido = (git cat-file blob $partes[0] | Out-String)
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo leer un archivo histórico.' }
    foreach ($regla in $reglas.Keys) {
        if ($contenido -match $reglas[$regla]) {
            # No imprimir el valor detectado.
            Write-Output "$regla en $($partes[1]) ($($partes[0]))"
            $fallos++
        }
    }
}
Write-Output "Archivos históricos revisados: $revisados. Coincidencias: $fallos."
if ($fallos -gt 0) { exit 1 }
