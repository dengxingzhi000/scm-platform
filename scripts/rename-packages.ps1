#Requires -Version 5.1
$ErrorActionPreference = "Stop"

$PROJECT_ROOT = Split-Path -Parent $PSScriptRoot

function Update-FileContent {
    param($FilePath, $OldPattern, $NewPattern)
    $content = Get-Content -LiteralPath $FilePath -Raw
    if ($content -match $OldPattern) {
        $newContent = $content -replace $OldPattern, $NewPattern
        Set-Content -LiteralPath $FilePath -Value $newContent -NoNewline
        Write-Host "  Updated: $FilePath"
    }
}

# === PHASE 1: Update Java file content ===
Write-Host "=== Phase 1: Updating Java package/import declarations ==="
Get-ChildItem -Path $PROJECT_ROOT -Filter "*.java" -Recurse -File | ForEach-Object {
    Update-FileContent -FilePath $_.FullName -OldPattern '(?m)^package com\.frog\.' -NewPattern 'package com.scmcloud.'
    Update-FileContent -FilePath $_.FullName -OldPattern '(?m)^package scm\.' -NewPattern 'package com.scmcloud.'
    Update-FileContent -FilePath $_.FullName -OldPattern '(?m)^import com\.frog\.' -NewPattern 'import com.scmcloud.'
    Update-FileContent -FilePath $_.FullName -OldPattern '(?m)^import scm\.' -NewPattern 'import com.scmcloud.'
}

# === PHASE 2: Update MyBatis XML namespace ===
Write-Host "=== Phase 2: Updating MyBatis XML namespace ==="
Get-ChildItem -Path $PROJECT_ROOT -Filter "*.xml" -Recurse -File | Where-Object {
    $content = Get-Content -LiteralPath $_.FullName -Raw
    $content -match 'namespace="com\.frog\.' -or $content -match 'namespace="scm\.'
} | ForEach-Object {
    Update-FileContent -FilePath $_.FullName -OldPattern 'namespace="com\.frog\.' -NewPattern 'namespace="com.scmcloud.'
    Update-FileContent -FilePath $_.FullName -OldPattern 'namespace="scm\.' -NewPattern 'namespace="com.scmcloud.'
}

# === PHASE 3: Move directories ===
Write-Host "=== Phase 3: Moving Java source directories ==="
Get-ChildItem -Path $PROJECT_ROOT -Directory -Recurse | Where-Object {
    $_.FullName -match '\\src\\main\\java\\scm\\' -or $_.FullName -match '\\src\\main\\java\\com\\frog\\'
} | Sort-Object FullName -Descending | ForEach-Object {
    $relative = [System.IO.Path]::GetRelativePath($PROJECT_ROOT, $_.FullName)
    $newRelative = $relative -replace 'src\\main\\java\\scm\\', 'src\main\java\com\scmcloud\'
    $newRelative = $newRelative -replace 'src\\main\\java\\com\\frog\\', 'src\main\java\com\scmcloud\'
    $newPath = Join-Path $PROJECT_ROOT $newRelative
    $parent = Split-Path $newPath -Parent
    if (-not (Test-Path $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
    if (-not (Test-Path $newPath) -and (Test-Path $_.FullName)) {
        Move-Item -LiteralPath $_.FullName -Destination $newPath -Force
        Write-Host "  Moved: $relative -> $newRelative"
    }
}

# === PHASE 4: Update application config files ===
Write-Host "=== Phase 4: Updating application config files ==="
Get-ChildItem -Path $PROJECT_ROOT -Filter "application*.yml" -Recurse -File | ForEach-Object {
    Update-FileContent -FilePath $_.FullName -OldPattern 'com\.frog\.' -NewPattern 'com.scmcloud.'
    Update-FileContent -FilePath $_.FullName -OldPattern '(?<=type-aliases-package: )scm\.' -NewPattern 'com.scmcloud.'
}
Get-ChildItem -Path $PROJECT_ROOT -Filter "application*.yaml" -Recurse -File | ForEach-Object {
    Update-FileContent -FilePath $_.FullName -OldPattern 'com\.frog\.' -NewPattern 'com.scmcloud.'
    Update-FileContent -FilePath $_.FullName -OldPattern '(?<=type-aliases-package: )scm\.' -NewPattern 'com.scmcloud.'
}
Get-ChildItem -Path $PROJECT_ROOT -Filter "application*.properties" -Recurse -File | ForEach-Object {
    Update-FileContent -FilePath $_.FullName -OldPattern 'com\.frog\.' -NewPattern 'com.scmcloud.'
    Update-FileContent -FilePath $_.FullName -OldPattern 'scm\.' -NewPattern 'com.scmcloud.'
}

# === PHASE 5: Update pom.xml references ===
Write-Host "=== Phase 5: Updating pom.xml references ==="
Get-ChildItem -Path $PROJECT_ROOT -Filter "pom.xml" -Recurse -File | ForEach-Object {
    $content = Get-Content -LiteralPath $_.FullName -Raw
    if ($content -match 'com\.frog') {
        $newContent = $content -replace 'com\.frog', 'com.scmcloud'
        Set-Content -LiteralPath $_.FullName -Value $newContent -NoNewline
        Write-Host "  Updated POM: $($_.FullName)"
    }
}

Write-Host "=== Package rename complete! ==="
