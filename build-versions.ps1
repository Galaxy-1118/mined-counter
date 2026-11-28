<#
.SYNOPSIS
  Builds one Mined Counter jar for every Minecraft 26.x target in versions.json.

.DESCRIPTION
  Expected artifact layout under -ArtifactsRoot:

    <root>/<minecraft>/<minecraft>.jar       client jar (Mojang mappings)
    <root>/<minecraft>/fabric-loader.jar     matching Fabric Loader
    <root>/<minecraft>/fabric-api.jar        matching Fabric API bundle

  The script compiles the same sources for every target, writes a
  per-version fabric.mod.json, and places jars in dist/ with names such as
  mined-counter-1.2.0+26.1.2.jar.

  This is intentionally a plain javac build, matching the project's existing
  no-Gradle setup. ASCII-only comments keep Windows PowerShell 5.1 happy.
#>
[CmdletBinding()]
param(
    [string]$VersionsFile = (Join-Path $PSScriptRoot 'versions.json'),
    [string]$ArtifactsRoot = 'D:\Deepseek Harness\mc-versions',
    [string]$LibraryRoot = 'E:\MCPCL\PCL 2.12.7.3\.minecraft\libraries',
    [string]$JdkHome = 'D:\JDK25',
    [string]$ModVersion = '1.2.0',
    [string]$BuildRoot = (Join-Path $PSScriptRoot 'build-versions'),
    [string]$OutputDir = (Join-Path $PSScriptRoot 'dist')
)

$ErrorActionPreference = 'Stop'
$utf8 = New-Object Text.UTF8Encoding($false)

$javac = Join-Path $JdkHome 'bin\javac.exe'
$jarTool = Join-Path $JdkHome 'bin\jar.exe'
if (-not (Test-Path -LiteralPath $javac)) {
    throw "javac not found under JdkHome: $JdkHome"
}
if (-not (Test-Path -LiteralPath $VersionsFile)) {
    throw "Versions file not found: $VersionsFile"
}

function Get-LibraryVersion($file) {
    $match = [regex]::Match($file.Name, '\d+(\.\d+)+')
    if ($match.Success) { return [version]$match.Value }
    return [version]'0.0'
}

function Get-LatestLibrary {
    param([string]$Name)
    Get-ChildItem -LiteralPath $LibraryRoot -Recurse -File -Filter "$Name*.jar" -ErrorAction SilentlyContinue |
        Sort-Object { Get-LibraryVersion $_ } -Descending |
        Select-Object -First 1
}

$sources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src\main\java') -Recurse -File -Filter '*.java' |
    ForEach-Object { $_.FullName })
if ($sources.Count -eq 0) {
    throw 'No Java sources found.'
}

$buildBase = Join-Path $BuildRoot 'base-libs'
if (Test-Path -LiteralPath $BuildRoot) {
    Remove-Item -LiteralPath $BuildRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $buildBase, $OutputDir | Out-Null

# Compile-time libraries shared by every target. Minecraft itself is supplied
# per version; these are the annotation/serialization libraries javac needs.
$extra = @('slf4j-api', 'guava', 'fastutil', 'datafixerupper', 'brigadier',
    'commons-lang3', 'gson', 'authlib', 'jspecify', 'error_prone_annotations')
foreach ($name in $extra) {
    $hit = Get-LatestLibrary $name
    if (-not $hit) {
        throw "Required compile library not found: $name under $LibraryRoot"
    }
    Copy-Item -LiteralPath $hit.FullName -Destination (Join-Path $buildBase $hit.Name) -Force
}

$targets = Get-Content -LiteralPath $VersionsFile -Raw | ConvertFrom-Json
$template = [IO.File]::ReadAllText((Join-Path $PSScriptRoot 'src\main\resources\fabric.mod.json'), $utf8)
$built = New-Object System.Collections.Generic.List[string]

foreach ($target in $targets) {
    $mc = $target.minecraft
    $artifactDir = Join-Path $ArtifactsRoot $mc
    $gameJar = Join-Path $artifactDir "$mc.jar"
    $loaderJar = Join-Path $artifactDir 'fabric-loader.jar'
    $apiJar = Join-Path $artifactDir 'fabric-api.jar'

    foreach ($required in @($gameJar, $loaderJar, $apiJar)) {
        if (-not (Test-Path -LiteralPath $required)) {
            throw "Missing artifact for ${mc}: $required"
        }
    }

    $targetBuild = Join-Path $BuildRoot $mc
    $apiLibs = Join-Path $targetBuild 'api-libs'
    $classes = Join-Path $targetBuild 'classes'
    New-Item -ItemType Directory -Force -Path $apiLibs, $classes | Out-Null

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($apiJar)
    try {
        $nested = 0
        foreach ($entry in $zip.Entries) {
            if ($entry.FullName -like 'META-INF/jars/*.jar') {
                [IO.Compression.ZipFileExtensions]::ExtractToFile(
                    $entry,
                    (Join-Path $apiLibs (Split-Path $entry.FullName -Leaf)),
                    $true)
                $nested++
            }
        }
    } finally {
        $zip.Dispose()
    }
    if ($nested -eq 0) {
        throw "Fabric API bundle has no nested jars: $apiJar"
    }

    $classpath = @()
    $classpath += (Get-ChildItem -LiteralPath $apiLibs -File -Filter '*.jar' | ForEach-Object { $_.FullName })
    $classpath += (Get-ChildItem -LiteralPath $buildBase -File -Filter '*.jar' | ForEach-Object { $_.FullName })
    $classpath += $gameJar
    $classpath += $loaderJar

    "=== Minecraft $mc / Loader $($target.loader) / API $($target.api) ==="
    & $javac '--release' '21' '-encoding' 'UTF-8' '-nowarn' '-classpath' ($classpath -join ';') '-d' $classes @sources
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed for $mc with exit code $LASTEXITCODE"
    }

    Copy-Item -Path (Join-Path $PSScriptRoot 'src\main\resources\*') -Destination $classes -Recurse -Force

    $modVersion = "$ModVersion+$mc"
    $meta = $template
    $meta = $meta -replace '"version"\s*:\s*"[^"]*"', ('"version": "' + $modVersion + '"')
    $meta = $meta -replace '"minecraft"\s*:\s*"[^"]*"', ('"minecraft": "' + $target.minecraftRange + '"')
    $meta = $meta -replace '"fabricloader"\s*:\s*"[^"]*"', ('"fabricloader": "' + $target.loaderRange + '"')
    [IO.File]::WriteAllText((Join-Path $classes 'fabric.mod.json'), $meta, $utf8)

    $jarName = "mined-counter-$ModVersion+$mc.jar"
    $jarPath = Join-Path $OutputDir $jarName
    if (Test-Path -LiteralPath $jarPath) {
        Remove-Item -LiteralPath $jarPath -Force
    }
    & $jarTool '--create' '--file' $jarPath '-C' $classes '.'
    if ($LASTEXITCODE -ne 0) {
        throw "jar failed for $mc with exit code $LASTEXITCODE"
    }
    $built.Add("$jarName ($((Get-Item -LiteralPath $jarPath).Length) bytes)")
}

''
'Built jars:'
$built | ForEach-Object { '  ' + $_ }
