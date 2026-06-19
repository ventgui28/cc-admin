# Cantanhede Cycling HUB - Automatizacao de Auto-Update (PowerShell Edition)
$ErrorActionPreference = "Stop"

# Configurar encoding para UTF-8 de forma a suportar acentos portugueses
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8

try {
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host "  Cantanhede Cycling HUB - Automatizacao de Auto-Update" -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor Cyan

    $SupabaseUrl = "https://clewwpvkqfcogwdayjpl.supabase.co"
    $StorageBucket = "app-releases"
    $DestFileName = "app-update.apk"

    # Obter chave de servico
    $SupabaseServiceKey = $env:SUPABASE_SERVICE_KEY
    if ([string]::IsNullOrEmpty($SupabaseServiceKey)) {
        Write-Host "A variavel de ambiente SUPABASE_SERVICE_KEY nao esta definida." -ForegroundColor Yellow
        $SupabaseServiceKey = Read-Host "Introduz a tua Supabase service_role key"
        $SupabaseServiceKey = $SupabaseServiceKey.Trim()
    }

    if ([string]::IsNullOrEmpty($SupabaseServiceKey)) {
        throw "A service_role key e obrigatoria para enviar atualizacoes."
    }

    # Detetar versao no build.gradle.kts
    Write-Host "Detetando versao no build.gradle.kts..." -ForegroundColor Gray
    $GradlePath = "app/build.gradle.kts"
    if (-not (Test-Path $GradlePath)) {
        throw "Nao foi possivel encontrar o ficheiro $GradlePath"
    }

    $GradleContent = Get-Content $GradlePath -Raw
    $VersionCodeMatch = [regex]::Match($GradleContent, 'versionCode\s*=\s*(\d+)')
    $VersionNameMatch = [regex]::Match($GradleContent, 'versionName\s*=\s*"([^"]+)"')

    if (-not $VersionCodeMatch.Success) {
        throw "Nao foi possivel detetar o versionCode no build.gradle.kts"
    }

    $VersionCode = [int]$VersionCodeMatch.Groups[1].Value
    $VersionName = $VersionNameMatch.Groups[1].Value

    Write-Host "Versao encontrada: v$VersionName (Code: $VersionCode)" -ForegroundColor Green

    $Increment = Read-Host "Queres incrementar o versionCode automaticamente? (s/n)"
    if ($Increment -eq 's' -or $Increment -eq 'S') {
        $NewVersionCode = $VersionCode + 1
        Write-Host "Incrementando versionCode para $NewVersionCode..." -ForegroundColor Gray
        
        $GradleContent = $GradleContent -replace "versionCode\s*=\s*$VersionCode", "versionCode = $NewVersionCode"
        $VersionCode = $NewVersionCode
        
        $NewVersionName = Read-Host "Introduz o novo versionName (ex: 1.1) [Enter para manter $VersionName]"
        if (-not [string]::IsNullOrEmpty($NewVersionName)) {
            $GradleContent = $GradleContent -replace "versionName\s*=\s*`"$VersionName`"", "versionName = `"$NewVersionName`""
            $VersionName = $NewVersionName
        }
        
        [System.IO.File]::WriteAllText($GradlePath, $GradleContent, [System.Text.Encoding]::UTF8)
    }

    # Compilar APK
    Write-Host "Compilando APK..." -ForegroundColor Gray
    if ($IsWindows) {
        cmd.exe /c ".\gradlew.bat assembleDebug"
    } else {
        ./gradlew assembleDebug
    }

    $ApkFile = "app/build/outputs/apk/debug/app-debug.apk"
    if (-not (Test-Path $ApkFile)) {
        throw "APK nao encontrado em $ApkFile"
    }

    # Obter notas de lancamento
    Write-Host ""
    $NotesFile = "release_notes.txt"
    if (Test-Path $NotesFile) {
        Write-Host "Lendo notas de lancamento de $NotesFile (UTF-8)..." -ForegroundColor Gray
        $ReleaseNotes = [System.IO.File]::ReadAllText((Resolve-Path $NotesFile), [System.Text.Encoding]::UTF8)
        Remove-Item $NotesFile -ErrorAction SilentlyContinue
    } else {
        $ReleaseNotes = Read-Host "Introduz as notas de lancamento (Release Notes)"
    }

    # Upload para o Supabase Storage
    Write-Host "Enviando APK para o Supabase Storage..." -ForegroundColor Gray
    $UploadUrl = "$SupabaseUrl/storage/v1/object/$StorageBucket/$DestFileName"
    $Headers = @{
        "Authorization" = "Bearer $SupabaseServiceKey"
        "apikey" = $SupabaseServiceKey
        "x-upsert" = "true"
    }

    $ApkBytes = [System.IO.File]::ReadAllBytes($ApkFile)
    $UploadResponse = Invoke-RestMethod -Uri $UploadUrl -Method Post -Headers $Headers -Body $ApkBytes -ContentType "application/octet-stream"
    Write-Host "Upload do APK concluido com sucesso!" -ForegroundColor Green

    # Atualizar base de dados
    Write-Host "Atualizando tabela de atualizacoes (app_updates)..." -ForegroundColor Gray
    $ApkPublicUrl = "$SupabaseUrl/storage/v1/object/public/$StorageBucket/$DestFileName"
    $DbUrl = "$SupabaseUrl/rest/v1/app_updates"
    $DbHeaders = @{
        "Authorization" = "Bearer $SupabaseServiceKey"
        "apikey" = $SupabaseServiceKey
        "Content-Type" = "application/json; charset=utf-8"
    }

    $DbBody = @{
        version_code = $VersionCode
        version_name = $VersionName
        apk_url = $ApkPublicUrl
        release_notes = $ReleaseNotes
    } | ConvertTo-Json -Compress

    $DbBodyBytes = [System.Text.Encoding]::UTF8.GetBytes($DbBody)

    Invoke-RestMethod -Uri $DbUrl -Method Post -Headers $DbHeaders -Body $DbBodyBytes
    Write-Host "Tabela de atualizacoes atualizada com sucesso!" -ForegroundColor Green

    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "  PROCESSO CONCLUIDO COM SUCESSO!" -ForegroundColor Green
    Write-Host "  Versao: v$VersionName (Code: $VersionCode)" -ForegroundColor Green
    Write-Host "  Ficheiro: $DestFileName" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
    Read-Host "Pressione Enter para fechar"

} catch {
    Write-Host ""
    Write-Host "------------------------------------------------------------" -ForegroundColor Red
    Write-Host "ERRO NA EXECUCAO:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host "------------------------------------------------------------" -ForegroundColor Red
    Write-Host ""
    Read-Host "Pressione Enter para fechar o terminal..."
    exit 1
}
