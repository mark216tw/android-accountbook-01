# JSON 備份格式

## 用途

JSON 備份用於完整保存及還原帳務資料。外觀模式、主題色與最近分類屬於裝置偏好，不包含在備份中。

## 根物件

```json
{
  "formatVersion": 2,
  "appVersion": "1.0.0-prerelease",
  "exportedAt": 1789992000000,
  "categories": [],
  "transactions": [],
  "budgets": []
}
```

### categories

- `id`：大於零且不可重複
- `type`：`EXPENSE` 或 `INCOME`
- `name`：分類名稱
- `iconKey`：圖示識別字串
- `sortOrder`：顯示順序
- `isDefault`：是否為預設分類
- `isActive`：是否啟用

### transactions

- `id`：大於零且不可重複
- `type`：`EXPENSE` 或 `INCOME`
- `amount`：1 至 999,999,999 的新台幣整數
- `categoryId`：必須參照相同收支類型的分類
- `occurredEpochDay`：`LocalDate` epoch day，允許 1900-01-01 至 2200-12-31
- `note`：帳目備註，最多 100 個字元
- `createdAt`、`updatedAt`：Unix epoch milliseconds

### budgets

- `yearMonth`：ISO `YYYY-MM`
- `amount`：0 至 999,999,999

## 限制

- 檔案上限：10 MB
- 分類上限：500
- 帳目上限：200,000
- 預算月份上限：1,200
- 未知 JSON 欄位會忽略，以利同一格式版本擴充
- 不支援的 `formatVersion` 會拒絕匯入

## 還原語意

還原不是合併。檔案會先完成解析、欄位驗證、主鍵檢查與分類外鍵檢查，之後才在單一 Room transaction 中清除並取代分類、帳目和預算。任一步驟失敗時 transaction rollback，原資料保持不變。
