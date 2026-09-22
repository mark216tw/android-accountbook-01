# 流水帳查詢、甜甜圈圖與 CSV 匯出實作計畫

> 狀態：已於 `1.0.0-prerelease` 完成。本文保留作為設計決策與驗證項目的技術紀錄。

## 目標

本階段完成以下功能：

1. Room 按月份查詢帳目。
2. 流水帳搜尋與收支／分類篩選。
3. 新增帳目備註。
4. 單月支出甜甜圈圖。
5. 選定月份 CSV 匯出。

## 已確認決策

- 搜尋範圍包含分類名稱、帳目備註與金額。
- 快速記帳頁的備註採點擊後展開，不影響一般快速記帳流程。
- 流水帳提供全部／支出／收入及分類篩選。
- 甜甜圈顯示前五大支出分類，其餘合併為「其他分類合計」。
- CSV 匯出選定月份的全部帳目，不受畫面搜尋與篩選影響。
- CSV 匯出入口放在流水帳頁。
- 不加入第三方圖表或 CSV 套件。
- 依既有產品決策，不處理舊開發資料庫的向下相容。

## 1. Room 月份查詢

目前 App 會載入全部帳目，再於 Kotlin 端篩選選定月份。這會隨帳目數量增加而造成不必要的記憶體與重組成本。

### 查詢方式

Room 改為直接查詢選定月份，使用 epoch day 半開區間：

```text
[選定月份月初, 下一個月月初)
```

此方式可正確處理：

- 不同月份天數
- 閏年二月
- 12 月跨年至次年 1 月
- 不受時區影響

DAO 查詢將 JOIN `categories`，一次取得帳目與分類名稱，避免 Compose 每列使用 `firstOrNull` 搜尋分類。

### LedgerRow

新增 UI 查詢模型 `LedgerRow`，包含：

- `id`
- `type`
- `amount`
- `categoryId`
- `categoryName`
- `categoryActive`
- `note`
- `occurredEpochDay`
- `createdAt`
- `updatedAt`

### Repository

新增：

- `observeMonthLedger(startEpochDay, endEpochDayExclusive)`
- 單次月份查詢，供 CSV 匯出使用
- 依 ID 讀取帳目，供編輯時保留原始 `createdAt`

畫面不再觀察全部帳目。JSON 完整備份仍保留全表 snapshot 查詢。

### 索引

現有 `occurredEpochDay` 索引已能支援月份範圍查詢，本階段不額外建立複合索引。

## 2. 帳目備註

`TransactionEntity` 新增：

```kotlin
val note: String
```

### 規則

- 備註選填。
- 最多 100 個字元。
- 快速記帳頁平時只顯示「＋備註」。
- 點擊後展開單行文字欄。
- 儲存成功後清空並收合。
- 編輯帳目時固定顯示備註欄。
- 流水帳列在備註非空白時顯示內容。

### Schema 與備份

- Room schema 升級為 v3。
- 不提供 v2 到 v3 的舊開發資料相容。
- 舊開發資料庫更新後會重建。
- Room schema export 更新為 v3。
- JSON 備份格式升級為 v2。
- `BackupTransaction` 新增 `note`。
- 備份驗證限制備註長度為 100。

## 3. 流水帳搜尋與篩選

### 頁面結構

流水帳頁由上至下固定顯示：

1. 月份選擇器與 CSV 匯出按鈕。
2. 搜尋欄。
3. 全部／支出／收入 FilterChip。
4. 分類篩選選單。
5. 篩選結果筆數與收支合計。
6. 按日期群組的帳目清單。

控制列在月份沒有帳目時仍保持可見，讓使用者可以切換月份。

### 搜尋範圍

- 分類名稱
- 帳目備註
- 金額數字

搜尋不宣稱支援日期文字、帳目 ID 或其他尚未存在的欄位。

### 篩選範圍

- 全部
- 支出
- 收入
- 特定分類
- 選定月份

### 行為規則

- Room 只載入選定月份資料。
- 搜尋與篩選在該月份資料上執行。
- 月總額、預算與甜甜圈不受搜尋條件影響。
- 每日群組合計依目前篩選結果計算。
- 切換收支類型時，若分類不符合新類型則清除分類篩選。
- 停用分類只要在當月有歷史帳目，仍可被選擇與篩選。
- 切換月份時保留文字與收支類型篩選，但清除在新月份無效的分類篩選。

### 空狀態

區分兩種狀態：

- 選定月份沒有任何帳目：「這個月還沒有帳目」。
- 月份有帳目但無符合結果：「沒有符合搜尋或篩選條件的帳目」。

## 4. ViewModel 狀態重構

新增狀態：

- `selectedMonth`
- `monthRows`
- `visibleRows`
- `ledgerQuery`
- `ledgerTypeFilter`
- `ledgerCategoryFilter`
- `income`
- `expense`
- `balance`
- `expenseSlices`
- `isExportingCsv`

### Flow 管線

月份切換使用：

```text
selectedMonth
    -> 月初與下月月初
    -> flatMapLatest
    -> Room 月份 Flow
```

搜尋輸入使用短暫 debounce，減少快速輸入時的重複計算。

### 資料來源原則

- `monthRows` 用於月收入、月支出、結餘與甜甜圈。
- `visibleRows` 只用於流水帳顯示及篩選結果合計。
- 甜甜圈不可從 `visibleRows` 計算，避免搜尋造成整體統計改變。
- 編輯帳目不再依賴全量 `uiState.transactions`。

## 5. 單月支出甜甜圈圖

### 放置位置

甜甜圈卡片放在首頁月摘要下方，取代目前單純的前三名清單。

### 顯示內容

- 中央顯示選定月份總支出。
- 顯示前五大支出分類。
- 其餘分類合併為「其他分類合計」。
- 圖例顯示分類名稱、金額及百分比。
- 無支出時顯示清楚的空狀態。

使用者自己的「其他」分類與系統合併的「其他分類合計」必須使用不同名稱。

### 資料排序

- 依金額由高至低排列。
- 金額相同時依分類 ID 排序，確保結果穩定。
- 各分類金額總和必須等於月總支出。

### Canvas

使用 Jetpack Compose `Canvas`：

- 從 12 點鐘方向開始繪製。
- 比例使用 `Double` 計算。
- 最後一段補足浮點誤差，總角度維持 360 度。
- 單一分類繪製完整圓環。
- 第一版不加入扇區間隙，避免小分類完全消失。
- 中央文字由外層 `Box` 疊加，不直接在 Canvas 畫文字。

### 色彩

- 使用固定色盤。
- 顏色依分類 ID 穩定選取，不因排序改變而換色。
- 「其他分類合計」使用固定中性色。
- 深色及淺色模式都需維持足夠對比。

### 無障礙

- Canvas 提供月份、總支出及分類數摘要。
- 圖例以文字提供分類、金額及百分比。
- 色塊本身不重複播報。
- 不將不可點擊的扇區標示為按鈕。
- 不只依靠顏色傳達資料。

## 6. CSV 匯出

### 功能範圍

CSV 用於試算表分析，不作為可還原備份。

匯出內容為選定月份的全部帳目，不受搜尋與篩選影響。

### 入口

CSV 匯出按鈕放在流水帳月份選擇器旁。

開啟系統檔案選擇器前先保存當下月份，確保檔名與實際內容一致。

### 檔名

```text
accountbook-2026-09.csv
```

### 欄位

```csv
date,type,category,note,amount,currency,transaction_id,category_id,created_at,updated_at
```

| 欄位 | 格式 |
| --- | --- |
| `date` | ISO `yyyy-MM-dd` |
| `type` | `EXPENSE` 或 `INCOME` |
| `category` | 匯出當下的分類名稱 |
| `note` | 帳目備註 |
| `amount` | 無千分位的整數 |
| `currency` | 固定 `TWD` |
| `transaction_id` | 帳目 ID |
| `category_id` | 分類 ID |
| `created_at` | ISO-8601 UTC |
| `updated_at` | ISO-8601 UTC |

### 編碼與格式

- UTF-8
- 檔案開頭加入 BOM，改善 Windows Excel 的繁體中文辨識
- CRLF 換行
- MIME type 使用 `text/csv`
- 所有欄位一律使用雙引號
- 欄位中的 `"` 轉義為 `""`
- 不輸出 `NT$` 或千分位符號

### 試算表公式注入

分類與備註等文字若以以下字元開頭，前方加入單引號：

```text
= + - @ tab carriage-return
```

避免 Excel 或 Google Sheets 將使用者內容當成公式執行。

### 寫入方式

- 使用 `ActivityResultContracts.CreateDocument("text/csv")`。
- 不要求儲存空間權限。
- 使用 UTF-8 `Writer` 逐列輸出。
- 不建立完整大型 CSV 字串。
- 匯出期間停用重複操作。
- 寫入失敗時提示檔案可能不完整。

## 7. 預計新增檔案

```text
app/src/main/java/com/example/simpleaccountbook/data/QueryModels.kt
app/src/main/java/com/example/simpleaccountbook/data/CsvExporter.kt
app/src/main/java/com/example/simpleaccountbook/ui/ExpenseDonutChart.kt
app/src/test/java/com/example/simpleaccountbook/LedgerFilterTest.kt
app/src/test/java/com/example/simpleaccountbook/ExpenseSlicesTest.kt
app/src/test/java/com/example/simpleaccountbook/CsvExporterTest.kt
```

## 8. 預計修改檔案

```text
app/src/main/java/com/example/simpleaccountbook/data/Models.kt
app/src/main/java/com/example/simpleaccountbook/data/AppDao.kt
app/src/main/java/com/example/simpleaccountbook/data/AppDatabase.kt
app/src/main/java/com/example/simpleaccountbook/data/AppRepository.kt
app/src/main/java/com/example/simpleaccountbook/data/BackupCodec.kt
app/src/main/java/com/example/simpleaccountbook/AppViewModel.kt
app/src/main/java/com/example/simpleaccountbook/ui/HomeScreen.kt
app/src/main/java/com/example/simpleaccountbook/ui/HistoryScreen.kt
README.md
CHANGELOG.md
docs/PRODUCT_SPEC.md
docs/ARCHITECTURE.md
docs/BACKUP_FORMAT.md
```

## 9. 測試計畫

### Room

- 月初帳目包含在查詢結果。
- 下個月月初帳目不包含在前一月份。
- 12 月與次年 1 月正確分離。
- 閏年 2 月 29 日正確包含。
- 結果按日期 DESC、同日期 ID DESC 排序。
- 停用分類仍能顯示歷史分類名稱。
- 編輯帳目仍保留原始 `createdAt`。

### 搜尋與篩選

- 中文分類名稱搜尋。
- 中文備註搜尋。
- 金額搜尋。
- 全部／支出／收入篩選。
- 分類篩選。
- 切換類型後清除無效分類。
- 搜尋結果為空時顯示正確空狀態。
- 篩選不影響月總額與甜甜圈。

### 甜甜圈

- 無資料回傳空 slices。
- 單一分類總角度為 360 度。
- 多分類比例正確。
- 前五名與其他分類合計不改變總金額。
- 同金額排序穩定。
- 所有 sweep angle 總和為 360 度。
- 無 NaN 或 Infinity。
- Compose semantics 包含月份、總額及分類資訊。

### CSV

- UTF-8 BOM。
- CRLF 換行。
- 穩定 header。
- 中文內容正確。
- 逗號與雙引號正確 escaping。
- 內嵌換行不破壞欄位。
- 公式開頭內容被中和。
- 日期與 UTC 時間格式正確。
- 金額不含貨幣符號或千分位。
- 空月份仍輸出 header。
- Writer 錯誤會向上回報。

### JSON

- 備註可完整 round-trip。
- 備註長度限制。
- 新備份格式版本正確。
- 還原後帳目備註與分類關聯正確。

## 10. 文件更新

完成後更新：

- `README.md`
- `CHANGELOG.md`
- `docs/PRODUCT_SPEC.md`
- `docs/ARCHITECTURE.md`
- `docs/BACKUP_FORMAT.md`

將甜甜圈圖及 CSV 匯出從規劃功能移至已實作功能。

## 11. 實作順序

1. 新增帳目備註與 Room v3 schema。
2. 新增 `LedgerRow` 與月份 JOIN query。
3. 將 ViewModel 改為月份 `flatMapLatest`。
4. 修正新增與編輯帳目流程。
5. 加入搜尋及收支／分類篩選。
6. 加入甜甜圈資料計算與 Canvas UI。
7. 加入 CSV encoder 與 SAF 匯出。
8. 補齊 Room、搜尋、圖表、CSV 與 JSON 測試。
9. 更新文件。
10. 執行完整驗證。

## 12. 驗證指令

```powershell
.\gradlew.bat testDebugUnitTest compileDebugAndroidTestKotlin lintDebug assembleDebug
.\gradlew.bat testPrereleaseUnitTest lintPrerelease assemblePrerelease
```

若有實機或 Emulator：

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

完成後驗證 prerelease APK 版本、R8、Debug 金鑰簽章及 SHA-256。
