param([switch]$Revert)

if ($Revert) {
    git checkout -- *.java
    return
}

$replacementChar = [byte[]]@(0xEF, 0xBF, 0xBD) # U+FFFD
$totalFixed = 0
$fixedFiles = 0

Get-ChildItem -Recurse -Filter "*.java" | Where-Object { 
    !$_.FullName.Contains("\node_modules\") -and !$_.FullName.Contains("\.git\") 
} | ForEach-Object {
    $path = $_.FullName
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $changed = $false
    $i = 0
    
    while ($i -lt $bytes.Count - 2) {
        if ($bytes[$i] -ge 0xE0 -and $bytes[$i] -le 0xEF -and 
            $bytes[$i+1] -ge 0x80 -and $bytes[$i+1] -le 0xBF -and 
            $bytes[$i+2] -eq 0x3F) {
            $bytes[$i] = $replacementChar[0]
            $bytes[$i+1] = $replacementChar[1]
            $bytes[$i+2] = $replacementChar[2]
            $changed = $true
            $totalFixed++
            $i += 3
        } else {
            $i++
        }
    }
    
    if ($changed) {
        [System.IO.File]::WriteAllBytes($path, $bytes)
        $fixedFiles++
        Write-Host "Fixed: $(Resolve-Path -LiteralPath $path -Relative)"
    }
}

Write-Host "=== Summary ==="
Write-Host "Files fixed: $fixedFiles"
Write-Host "Sequences replaced: $totalFixed"
