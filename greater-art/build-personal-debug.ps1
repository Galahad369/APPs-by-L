$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$profileRoot = $env:USERPROFILE
if ([string]::IsNullOrWhiteSpace($profileRoot)) {
    throw "USERPROFILE is unavailable; refusing to create an APK with an unknown signing identity."
}

$debugKeystore = Join-Path $profileRoot ".android\debug.keystore"
if (-not (Test-Path -LiteralPath $debugKeystore)) {
    throw "The established Android debug keystore was not found at $debugKeystore."
}

if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME) -or
    -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $jdk = Get-ChildItem "${env:ProgramFiles}\Android\openjdk" -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        Select-Object -First 1
    if ($null -eq $jdk) {
        throw "Set JAVA_HOME to Android Studio's JDK 17 or newer, then run this script again."
    }
    $env:JAVA_HOME = $jdk.FullName
}

$previousAndroidHome = $env:ANDROID_HOME
$previousAndroidSdkRoot = $env:ANDROID_SDK_ROOT
$androidSdk = $env:ANDROID_HOME
if ([string]::IsNullOrWhiteSpace($androidSdk) -or
    -not (Test-Path -LiteralPath (Join-Path $androidSdk "platforms\android-37.0\android.jar"))) {
    $androidSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
}
if (-not (Test-Path -LiteralPath (Join-Path $androidSdk "platforms\android-37.0\android.jar"))) {
    throw "Android SDK platform 37 was not found. Install it with Android Studio's SDK Manager."
}
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_SDK_ROOT = $androidSdk

Push-Location $projectRoot
try {
    $java = Join-Path $env:JAVA_HOME "bin\java.exe"
    $localGradleCli = Join-Path $projectRoot ".gradle\distributions\gradle-9.5.0\lib\gradle-gradle-cli-main-9.5.0.jar"
    if (Test-Path -LiteralPath $localGradleCli) {
        & $java `
            "-Duser.home=$profileRoot" `
            "-Djavax.net.ssl.trustStoreType=Windows-ROOT" `
            "-Dorg.gradle.appname=gradle" `
            -jar $localGradleCli `
            clean testDebugUnitTest lintDebug assembleDebug --no-daemon
    } else {
        $wrapperJar = Join-Path $projectRoot "gradle\wrapper\gradle-wrapper.jar"
        & $java `
            "-Duser.home=$profileRoot" `
            "-Djavax.net.ssl.trustStoreType=Windows-ROOT" `
            "-Dorg.gradle.appname=gradlew" `
            -classpath $wrapperJar `
            org.gradle.wrapper.GradleWrapperMain `
            clean testDebugUnitTest lintDebug assembleDebug --no-daemon
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle verification failed with exit code $LASTEXITCODE."
    }

    $metadataPath = Join-Path $projectRoot "app\build\outputs\apk\debug\output-metadata.json"
    $metadata = Get-Content -Raw -LiteralPath $metadataPath | ConvertFrom-Json
    $sourceApk = Join-Path $projectRoot "app\build\outputs\apk\debug\$($metadata.elements[0].outputFile)"
    $destination = Join-Path $projectRoot "GreaterArt-v$($metadata.elements[0].versionName)-debug.apk"
    Copy-Item -LiteralPath $sourceApk -Destination $destination -Force
    Write-Output "Built and copied $destination"
} finally {
    $env:ANDROID_HOME = $previousAndroidHome
    $env:ANDROID_SDK_ROOT = $previousAndroidSdkRoot
    Pop-Location
}
