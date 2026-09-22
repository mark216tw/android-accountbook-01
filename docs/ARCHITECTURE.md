# 技術架構

## 概覽

專案採用單 Activity、單 Gradle module 的 Jetpack Compose 架構。介面狀態由 `AppViewModel` 統一管理，Room 與 DataStore 透過 Flow 提供反應式資料更新。

```text
Compose UI
    |
AppViewModel + StateFlow
    |
AppRepository -------- SettingsStore
    |                       |
Room / AppDao           DataStore Preferences
```

此 MVP 不使用依賴注入框架。`AccountBookApplication` 建立資料庫、Repository 與 SettingsStore，並由自訂 ViewModel Factory 注入。

## 目錄結構

```text
app/src/main/java/com/example/simpleaccountbook/
├── AccountBookApplication.kt
├── AppViewModel.kt
├── MainActivity.kt
├── Money.kt
├── data/
│   ├── AppDao.kt
│   ├── AppDatabase.kt
│   ├── AppRepository.kt
│   ├── BackupCodec.kt
│   ├── CsvExporter.kt
│   ├── Models.kt
│   ├── QueryModels.kt
│   └── SettingsStore.kt
└── ui/
    ├── AccountBookApp.kt
    ├── CategoryScreen.kt
    ├── DateAndMonthControls.kt
    ├── ExpenseDonutChart.kt
    ├── HistoryScreen.kt
    ├── HomeScreen.kt
    ├── SettingsScreen.kt
    └── Theme.kt
```

## 資料模型

### CategoryEntity

儲存收入或支出分類，包括名稱、圖示識別、排序、預設分類狀態與啟用狀態。

### TransactionEntity

儲存單筆帳目，包括收支類型、整數金額、分類 ID、備註、純日期及建立／更新時間。金額使用 `Long`，日期使用 `LocalDate.toEpochDay()`，避免浮點數精度與跨時區日期偏移。

### LedgerRow

Room 以月份半開區間查詢帳目並 JOIN 分類，直接提供分類名稱與啟用狀態。畫面只觀察選定月份，搜尋與篩選在該月份資料上執行。

### MonthlyBudgetEntity

使用 ISO `YYYY-MM` 作為主鍵，每個月份保存一筆預算。

### ThemeSettings

儲存顯示模式、預設主題 ID、自訂 Hue，以及收入／支出最近使用的分類。資料由 DataStore Preferences 持久化。

## 資料完整性

- `transactions.categoryId` 以外鍵連結分類
- 分類仍被帳目使用時採軟刪除，也就是設為停用
- 帳目刪除後可用原始 ID 重新插入以完成復原
- 預算使用月份主鍵與 Replace 策略更新
- 預設分類建立、分類排序、備份快照與還原使用 Room transaction
- JSON 還原先完成格式與外鍵驗證，成功後才取代資料

目前資料庫 schema 為 v3。依產品決策，不提供舊 prerelease schema 的資料轉換；舊開發資料庫更新後會重建。

## 備份

`BackupCodec` 使用 v2 JSON 格式。匯出先在 transaction 中取得一致快照；匯入限制為 10 MB，並限制分類、帳目、備註、預算數量與金額／日期範圍。外觀偏好不屬於帳務備份。`CsvExporter` 則逐列輸出選定月份資料供試算表分析，不作為還原格式。

## UI 導覽

底部導覽提供「記帳」與「流水帳」，並透過 Navigation Bar inset 保留系統導覽安全區。設定由主畫面右上角齒輪進入，分類管理由設定頁進入。畫面切換保存在 Compose saveable state，設定與分類頁另處理 Android 返回操作。

## 主題系統

Material 3 ColorScheme 由預設色或 HSL Hue 種子產生。顯示模式可強制淺色、深色或跟隨系統，並同步調整 Status Bar 與 Navigation Bar 圖示明暗。

## 測試

JVM 測試涵蓋預算門檻、流水帳篩選、甜甜圈資料、CSV 編碼、JSON round-trip 與輸入限制。Instrumentation 測試涵蓋 Room 月份邊界與排序、原子還原、rollback、分類 seed 與月份選擇 Compose semantics。建議發行前執行單元測試、Lint、Debug／Prerelease 建置及實機或 emulator 測試。
