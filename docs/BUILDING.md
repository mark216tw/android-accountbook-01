# 建置與發行

## 環境需求

- Windows、macOS 或 Linux
- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools 35.x

專案包含 Gradle Wrapper，不需要另外安裝 Gradle。

## 常用指令

Windows PowerShell：

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat compileDebugAndroidTestKotlin
```

macOS 或 Linux：

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew compileDebugAndroidTestKotlin
```

## Prerelease 測試發行版

此變體為 Pre-release 測試版本，不是正式上線版本。

`prerelease` Build Type 設定如下：

- 版本名稱：`1.0.0-prerelease`
- R8 程式碼壓縮：啟用
- Android 資源縮減：啟用
- 簽章：本機 Android Debug keystore
- 用途：內部測試與裝置安裝驗證

執行完整驗證與建置：

```powershell
.\gradlew.bat testPrereleaseUnitTest lintPrerelease assemblePrerelease
```

APK 輸出：

```text
app/build/outputs/apk/prerelease/app-prerelease.apk
```

## 驗證 APK

檢查簽章：

```powershell
& "$env:ANDROID_HOME\build-tools\35.0.1\apksigner.bat" verify --verbose --print-certs app\build\outputs\apk\prerelease\app-prerelease.apk
```

檢查套件及版本：

```powershell
& "$env:ANDROID_HOME\build-tools\35.0.1\aapt2.exe" dump badging app\build\outputs\apk\prerelease\app-prerelease.apk
```

## 正式發行注意事項

`prerelease` 使用 Debug 金鑰，不能作為 Google Play 正式版本。正式發行前應建立受保護的 release keystore，透過不納入版本控制的環境變數或本機設定提供簽章資訊，並更新 application ID、版本代碼與隱私政策。

任何 keystore、密碼、`local.properties` 或簽章設定檔都不得提交到 Git。

## 裝置測試

啟動 emulator 或連接實機後執行：

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

若 `adb devices` 沒有列出裝置，instrumentation 與 Compose UI 測試無法在本機執行，但仍可使用 `compileDebugAndroidTestKotlin` 驗證測試原始碼。

## 資料庫版本

目前 Room schema 為 v3，位於 `app/schemas`。本版不提供舊 prerelease 開發資料向下相容，從舊 schema 更新可能重建資料庫；更新前應先匯出 JSON 備份，測試時也可清除 App 資料或重新安裝。

## GitHub Release

Prerelease APK 發布到 GitHub Releases 時，必須勾選 **Set as a pre-release**，並在說明中標示「Pre-release 版本，不是正式上線版本」。Release asset 使用 `app-prerelease.apk`，不將 `app/build` 產物提交進 Git repository。
