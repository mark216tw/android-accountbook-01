# Changelog

本文件記錄專案的重要版本變更。

## 1.0.0-prerelease - 2026-09-22

> 此版本為 Pre-release 測試版本，不是正式上線版本。APK 使用 Android Debug 金鑰簽署。

### Added

- 收入／支出快速記帳、日期選擇、備註與最近分類記憶
- 任意月份切換、月收入、月支出、結餘與每月預算
- 流水帳文字搜尋、收支類型與分類篩選
- 帳目編輯、刪除及刪除後復原
- 前五大支出分類甜甜圈圖與其他分類合計
- 選定月份 CSV 匯出
- JSON v2 帳務備份、驗證與原子全量還原
- 分類新增、改名、排序、停用與刪除
- 系統／淺色／深色模式、預設主題與自訂 Hue
- 面向右方的小豬撲滿 Adaptive Icon 與 Android 13 monochrome 圖示
- Room、備份、搜尋篩選、甜甜圈、CSV 與 Compose UI 測試

### Changed

- Room schema 升級為 v3，帳目日期使用 epoch day 並加入備註欄位
- Room 直接查詢選定月份並 JOIN 分類，避免載入全部帳目
- 首頁與設定標題改為靠左排列
- 流水帳控制列採緊湊排版
- 底部導覽列保留系統 Navigation Bar 安全區

### Known limitations

- Prerelease 使用 Debug 簽章，不適用於 Google Play 正式發行
- 不提供舊開發資料庫 schema 的升級相容，更新可能重建本機資料
- 尚未提供雲端同步、多幣別、CSV 匯入、定期帳目與跨月趨勢
