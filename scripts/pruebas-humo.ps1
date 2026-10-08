param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Continue"
$resultados = @()

function Registrar($nombre, $ok, $detalle) {
    $estado = if ($ok) { "PASA" } else { "FALLA" }
    $script:resultados += [pscustomobject]@{ Prueba = $nombre; Estado = $estado; Detalle = $detalle }
    Write-Output "$estado | $nombre | $detalle"
}

try {
    $version = Invoke-WebRequest -Uri "$BaseUrl/version" -UseBasicParsing -TimeoutSec 20
    $json = $version.Content
    Registrar "Disponibilidad" ($version.StatusCode -eq 200 -and $json -match "version") $json
} catch {
    Registrar "Disponibilidad" $false $_.Exception.Message
}

try {
    $login = Invoke-WebRequest -Uri "$BaseUrl/login" -UseBasicParsing -TimeoutSec 20
    $tieneFormulario = $login.Content -match 'name="usuario"' -and $login.Content -match 'name="password"'
    Registrar "Flujo critico (pantalla de acceso)" ($login.StatusCode -eq 200 -and $tieneFormulario) "login HTTP $($login.StatusCode)"
} catch {
    Registrar "Flujo critico (pantalla de acceso)" $false $_.Exception.Message
}

try {
    $salud = Invoke-WebRequest -Uri "$BaseUrl/salud" -UseBasicParsing -TimeoutSec 20
    Registrar "Conexion con base de datos" ($salud.StatusCode -eq 200 -and $salud.Content -match '"conectada"') $salud.Content
} catch {
    Registrar "Conexion con base de datos" $false $_.Exception.Message
}

$codigoPrivado = curl.exe -s -o NUL -w "%{http_code}" "$BaseUrl/"
Registrar "Autenticacion" ($codigoPrivado -eq "401" -or $codigoPrivado -eq "302") "Sin sesion la ruta privada responde $codigoPrivado"

try {
    $sesion = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    Invoke-WebRequest -Uri "$BaseUrl/login" -WebSession $sesion -UseBasicParsing -TimeoutSec 20 | Out-Null
    $malo = Invoke-WebRequest -Uri "$BaseUrl/login" -Method Post -WebSession $sesion -UseBasicParsing -TimeoutSec 20 -Body @{
        usuario = "no-existe"
        password = "clave-incorrecta"
    }
    $muestraError = $malo.Content -match "error" -or $malo.BaseResponse.ResponseUri.AbsoluteUri -match "error=true"
    Registrar "Manejo de error de acceso" ($malo.StatusCode -eq 200 -and $muestraError) $malo.BaseResponse.ResponseUri.AbsoluteUri
} catch {
    Registrar "Manejo de error de acceso" $false $_.Exception.Message
}

$fallas = @($resultados | Where-Object { $_.Estado -eq "FALLA" }).Count
Write-Output "RESUMEN fallas=$fallas total=$($resultados.Count)"
if ($fallas -gt 0) { exit 1 }
