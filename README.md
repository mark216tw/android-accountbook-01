# 簡單記帳單機版

一款以快速、直覺、離線使用為核心的 Android 記帳 App。使用者可以直接透過畫面內建數字鍵盤輸入金額、選擇分類並儲存，不需要開啟系統鍵盤或登入帳號。

> [!WARNING]
> 目前提供的是 `1.0.0-prerelease` 測試版本，尚非正式上線版本。APK 使用 Android Debug 金鑰簽署，僅供測試與功能預覽。

## 主要功能

- 快速記帳：收入／支出切換、內建數字鍵盤、日期、備註與最近分類
- 流水帳：按月份及日期群組，支援文字、收支類型與分類篩選
- 帳目管理：修改金額、分類與備註、刪除及復原
- 月份切換：可查看過去或未來月份的收入、支出與結餘
- 月預算：顯示選定月份已花費、剩餘額度與警示進度條
- 支出摘要：甜甜圈圖顯示選定月份前五大支出分類、其餘合計與比例
- CSV 匯出：透過系統檔案選擇器匯出選定月份的完整帳目
- JSON 備份：透過系統檔案選擇器匯出及完整還原帳務資料
- 分類管理：新增、改名、排序、停用或刪除分類
- 外觀設定：系統／淺色／深色模式
- 主題色彩：6 組預設色與 Hue 自訂色滑桿
- 完全離線：帳目保存在裝置本機，不需要網路權限
- Adaptive Icon：支援 Android 13 monochrome 主題圖示

## 技術規格

- Kotlin 2.1.20
- Jetpack Compose + Material 3
- Room
- DataStore Preferences
- Coroutines + StateFlow
- Min SDK 26
- Target SDK 35
- Java 17

## 開始建置

環境需要 Android SDK 35 與 JDK 17。

```powershell
.\gradlew.bat assembleDebug
```

建立 R8 壓縮、資源縮減並以 Debug 金鑰簽署的測試發行版：

```powershell
.\gradlew.bat testPrereleaseUnitTest lintPrerelease assemblePrerelease
```

產物位置：

```text
app/build/outputs/apk/prerelease/app-prerelease.apk
```

目前 prerelease 版本為 `1.0.0-prerelease`。此變體使用 Debug 金鑰，只適合測試安裝，不應作為正式商店發行簽章。

## 下載 Pre-release

可從 [GitHub Releases](https://github.com/mark216tw/android-accountbook-01/releases) 下載 `app-prerelease.apk`。

- 套件名稱：`com.example.simpleaccountbook`
- 最低版本：Android 8.0（API 26）
- Release 類型：Pre-release
- 簽章：Android Debug Key

## 文件

- [產品規格](docs/PRODUCT_SPEC.md)
- [技術架構](docs/ARCHITECTURE.md)
- [建置與發行](docs/BUILDING.md)
- [備份格式](docs/BACKUP_FORMAT.md)
- [文件索引](docs/README.md)
- [Pre-release 說明](RELEASE_NOTES.md)
- [版本紀錄](CHANGELOG.md)

## 規劃中功能

- 跨月份趨勢圖
- 定期帳目

## 授權

本專案採用 [MIT License](LICENSE)。
