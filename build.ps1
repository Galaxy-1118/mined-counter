<#
.SYNOPSIS
  Builds Mined Counter for Minecraft 26.1.2 + Fabric Loader 0.19.2.

.DESCRIPTION
  Compiles the sources against the Mojang-mapped 26.1.2 version jar and the
  Fabric API bundle found in the given version folder, then packages
  dist/mined-counter-<Version>.jar.

  The script is ASCII-only on purpose: Windows PowerShell 5.1 mis-decodes
  BOM-less non-ASCII scripts.

  This project does not use Gradle/Loom. The 26.x production jar ships with
  Mojang class names (for example net/minecraft/server/level/ServerPlayer),
  so plain javac can compile against it.

.EXAMPLE
  .\build.ps1 -GameDir 'C:\Users\me\AppData\Roaming\.minecraft\versions\26.1.2-Fabric 0.19.2'

.EXAMPLE
  .\build.ps1 -GameDir 'E:\MCPCL\PCL 2.12.7.3\.minecraft\versions\26.1.2-Fabric 0.19.2' -JdkHome 'D:\JDK25'
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$GameDir,

    [string]$JdkHome = $(if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'D:\JDK25' }),

    [string]$Version = '1.2.0'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root = $PSScriptRoot
$buildDir = Join-Path $root 'build'
$libsDir = Join-Path $buildDir 'libs'
$classesDir = Join-Path $buildDir 'classes'
$distDir = Join-Path $root 'dist'

if (-not (Test-Path -LiteralPath $GameDir)) {
    throw "GameDir not found: $GameDir. Point it at the Minecraft version folder that contains the 26.1.2 jar."
}

$javac = Join-Path $JdkHome 'bin\javac.exe'
$jarTool = Join-Path $JdkHome 'bin\jar.exe'
if (-not (Test-Path -LiteralPath $javac)) {
    throw "javac not found under JdkHome: $JdkHome. Install JDK 21+ or pass -JdkHome explicitly."
}

# --- locate the game, loader and Fabric API jars -------------------------

$versionName = Split-Path -Leaf $GameDir
$gameJar = Join-Path $GameDir ($versionName + '.jar')
if (-not (Test-Path -LiteralPath $gameJar)) {
    $gameJar = Get-ChildItem -LiteralPath $GameDir -Filter '*.jar' -File |
        Where-Object { $_.Name -notmatch '(sources|javadoc|natives)' } |
        Sort-Object Length -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $gameJar) { throw "No game jar found in $GameDir" }

$mcDir = Split-Path (Split-Path $GameDir -Parent) -Parent
$loaderRoot = Join-Path $mcDir 'libraries\net\fabricmc\fabric-loader'
$loaderJar = Get-ChildItem -LiteralPath $loaderRoot -Recurse -File -Filter 'fabric-loader-*.jar' -ErrorAction SilentlyContinue |
    Sort-Object { try { [version]$_.Directory.Name } catch { [version]'0.0' } } -Descending |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $loaderJar) { throw "Fabric Loader jar not found under $loaderRoot" }

$modsDir = Join-Path $GameDir 'mods'
$apiJar = Get-ChildItem -LiteralPath $modsDir -Filter 'fabric-api-*.jar' -File -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $apiJar) { throw "Fabric API jar not found in $modsDir. Put fabric-api-<version>+26.1.2.jar there first." }

"game jar   : $gameJar"
"loader jar : $loaderJar"
"api jar    : $apiJar"

# --- collect compile-time libraries --------------------------------------

if (Test-Path -LiteralPath $buildDir) { Remove-Item -LiteralPath $buildDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $libsDir, $classesDir, $distDir | Out-Null

$zip = [System.IO.Compression.ZipFile]::OpenRead($apiJar)
try {
    $nested = 0
    foreach ($entry in $zip.Entries) {
        if ($entry.FullName -like 'META-INF/jars/*.jar') {
            $dest = Join-Path $libsDir (Split-Path $entry.FullName -Leaf)
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $dest, $true)
            $nested++
        }
    }
} finally {
    $zip.Dispose()
}
if ($nested -eq 0) { throw "Fabric API bundle has no META-INF/jars entries: $apiJar" }
"nested api jars: $nested"

$extra = @('slf4j-api', 'guava', 'fastutil', 'datafixerupper', 'brigadier', 'commons-lang3', 'gson', 'authlib', 'jspecify', 'error_prone_annotations')
$libRoot = Join-Path $mcDir 'libraries'
function Get-LibraryVersion($file) {
    $match = [regex]::Match($file.Name, '\d+(\.\d+)+')
    if ($match.Success) { return [version]$match.Value }
    return [version]'0.0'
}
foreach ($name in $extra) {
    $hit = Get-ChildItem -LiteralPath $libRoot -Recurse -File -Filter "$name*.jar" -ErrorAction SilentlyContinue |
        Sort-Object { Get-LibraryVersion $_ } -Descending |
        Select-Object -First 1
    if ($hit) {
        Copy-Item -LiteralPath $hit.FullName -Destination (Join-Path $libsDir $hit.Name) -Force
    }
}

# --- compile and package -------------------------------------------------

$classpath = (Join-Path $libsDir '*') + ';' + $gameJar + ';' + $loaderJar
$srcRoot = Join-Path $root 'src\main\java'
$sources = @(Get-ChildItem -LiteralPath $srcRoot -Recurse -File -Filter '*.java' | ForEach-Object { $_.FullName })
if ($sources.Count -eq 0) { throw "No Java sources found under $srcRoot" }

& $javac '--release' '21' '-encoding' 'UTF-8' '-nowarn' '-classpath' $classpath '-d' $classesDir @sources
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

$resRoot = Join-Path $root 'src\main\resources'
if (Test-Path -LiteralPath $resRoot) {
    Copy-Item -Path (Join-Path $resRoot '*') -Destination $classesDir -Recurse -Force
}

$jarPath = Join-Path $distDir ("mined-counter-$Version.jar")
if (Test-Path -LiteralPath $jarPath) { Remove-Item -LiteralPath $jarPath -Force }
& $jarTool '--create' '--file' $jarPath '-C' $classesDir '.'
if ($LASTEXITCODE -ne 0) { throw "jar failed with exit code $LASTEXITCODE" }

$size = (Get-Item -LiteralPath $jarPath).Length
"built: $jarPath ($size bytes)"
