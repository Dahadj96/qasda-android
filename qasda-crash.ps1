# Launch smoke test. See RELEASE.md, "Never ship a build you have not launched".
#
# A build that compiles, minifies and passes its tests can still be rejected
# by the dex verifier at launch. Nothing but a real Android runtime can tell
# you. Run this before handing an APK to anybody.
param(
  [string]$Device = 'emulator-5554',
  [string]$Adb = "$env:ANDROID_HOME\platform-tools\adb.exe",
  [string]$Apk = 'app\build\outputs\apk\staging\app-staging.apk',
  [string]$Package = 'pro.qasdatrip.app.staging'
)

if (-not (Test-Path $Adb)) { $Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" }

& $Adb -s $Device uninstall $Package 2>&1 | Out-Null
& $Adb -s $Device install -r $Apk 2>&1 | Select-Object -Last 1
& $Adb -s $Device logcat -c
& $Adb -s $Device shell monkey -p $Package -c android.intent.category.LAUNCHER 1 2>&1 | Out-Null
Start-Sleep -Seconds 8

Write-Output "==== CRASH ===="
$crash = & $Adb -s $Device logcat -d -b crash 2>&1 | Select-Object -First 60
$crash
if ($crash | Where-Object { $_ -match 'FATAL EXCEPTION' }) {
  Write-Output "FAILED: the app did not survive launch."
  exit 1
}
Write-Output "clean - the app reached its first frame."
