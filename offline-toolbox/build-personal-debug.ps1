$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$profileRoot = $env:USERPROFILE
if ([string]::IsNullOrWhiteSpace($profileRoot)) {
    throw "USERPROFILE is unavailable."
}

if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME) -or
    -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $jdk = Get-ChildItem "${env:ProgramFiles}\Android\openjdk" -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        Select-Object -First 1
    if ($null -eq $jdk) { throw "Install Android Studio or set JAVA_HOME to JDK 17+." }
    $env:JAVA_HOME = $jdk.FullName
}

$androidSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
if (-not (Test-Path -LiteralPath (Join-Path $androidSdk "platforms\android-37.0\android.jar"))) {
    throw "Android SDK platform 37 was not found. Install it from Android Studio's SDK Manager."
}
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_SDK_ROOT = $androidSdk

Push-Location $projectRoot
try {
    $java = Join-Path $env:JAVA_HOME "bin\java.exe"
    & $java `
        "-Duser.home=$profileRoot" `
        "-Djavax.net.ssl.trustStoreType=Windows-ROOT" `
        "-Dorg.gradle.appname=gradlew" `
        -classpath "gradle\wrapper\gradle-wrapper.jar" `
        org.gradle.wrapper.GradleWrapperMain `
        clean testDebugUnitTest lintDebug assembleDebug --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "Gradle verification failed with exit code $LASTEXITCODE." }

    $metadata = Get-Content -Raw "app\build\outputs\apk\debug\output-metadata.json" | ConvertFrom-Json
    $source = Join-Path $projectRoot "app\build\outputs\apk\debug\$($metadata.elements[0].outputFile)"
    $destination = Join-Path $projectRoot "LocalKit-v$($metadata.elements[0].versionName)-final-debug.apk"
    Copy-Item -LiteralPath $source -Destination $destination -Force
    Write-Output "Built $destination"
} finally {
    Pop-Location
}
