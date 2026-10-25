param([string]$Maven = 'mvn.cmd', [switch]$UseWindowsTrustStore)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$kitPath = Join-Path $projectRoot 'build/editor-kit/LevelEditorKit-Java17.jar'
$expectedHash = '5b75ada82ad71d128150b58f0ab74e0f0e18866968b216968c66940209e1252d'
$releaseUrl = 'https://github.com/mrcalzon02/The-Beckenburg-Project/releases/download/beckenburg-v0.1.31/LevelEditorKit-Java17.jar'
New-Item -ItemType Directory -Force -Path (Split-Path $kitPath) | Out-Null
if (!(Test-Path -LiteralPath $kitPath) -or (Get-FileHash -LiteralPath $kitPath -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedHash) {
    & curl.exe --fail --location --retry 2 --silent --show-error $releaseUrl --output $kitPath
    if ($LASTEXITCODE -ne 0) { throw 'Pinned editor-kit download failed' }
}
if ((Get-FileHash -LiteralPath $kitPath -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedHash) { throw 'Pinned editor-kit SHA-256 mismatch' }
$previousMavenOptions = $env:MAVEN_OPTS
try {
    if ($UseWindowsTrustStore) { $env:MAVEN_OPTS = "$previousMavenOptions -Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE" }
    & $Maven -q -f (Join-Path $projectRoot 'gpu-client/pom.xml') org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file "-Dfile=$kitPath" '-DgroupId=org.beckenburg' '-DartifactId=level-editor-kit' '-Dversion=0.1.31' '-Dpackaging=jar' '-DgeneratePom=true'
    if ($LASTEXITCODE -ne 0) { throw 'Pinned editor-kit installation failed' }
} finally { $env:MAVEN_OPTS = $previousMavenOptions }
Write-Output 'Pinned standalone editor kit verified and installed.'
