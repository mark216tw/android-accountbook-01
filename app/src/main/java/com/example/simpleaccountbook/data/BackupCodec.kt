package com.example.simpleaccountbook.data

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupDocument(
    val formatVersion: Int = 2,
    val appVersion: String,
    val exportedAt: Long,
    val categories: List<BackupCategory>,
    val transactions: List<BackupTransaction>,
    val budgets: List<BackupBudget>,
)

@Serializable
data class BackupCategory(
    val id: Long,
    val type: String,
    val name: String,
    val iconKey: String,
    val sortOrder: Int,
    val isDefault: Boolean,
    val isActive: Boolean,
)

@Serializable
data class BackupTransaction(
    val id: Long,
    val type: String,
    val amount: Long,
    val categoryId: Long,
    val occurredEpochDay: Long,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupBudget(val yearMonth: String, val amount: Long)

object BackupCodec {
    const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
    private const val MAX_CATEGORIES = 500
    private const val MAX_TRANSACTIONS = 200_000
    private const val MAX_BUDGETS = 1_200
    private const val MAX_AMOUNT = 999_999_999L
    private const val MAX_ID = 1_000_000_000L
    private val minEpochDay = LocalDate.of(1900, 1, 1).toEpochDay()
    private val maxEpochDay = LocalDate.of(2200, 12, 31).toEpochDay()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun encode(document: BackupDocument): String = json.encodeToString(document)

    fun decodeAndValidate(content: String): BackupDocument {
        val document = try {
            json.decodeFromString<BackupDocument>(content)
        } catch (error: Exception) {
            throw IllegalArgumentException("無法讀取備份檔，請確認檔案格式", error)
        }
        require(document.formatVersion == 2) { "不支援的備份格式版本：${document.formatVersion}" }
        require(document.categories.isNotEmpty()) { "備份檔缺少分類資料" }
        require(document.categories.size <= MAX_CATEGORIES) { "分類數量超過上限" }
        require(document.transactions.size <= MAX_TRANSACTIONS) { "帳目數量超過上限" }
        require(document.budgets.size <= MAX_BUDGETS) { "預算月份數量超過上限" }

        val categoryIds = document.categories.map { category ->
            require(category.id in 1..MAX_ID) { "分類 ID 超過允許範圍" }
            require(category.name.isNotBlank()) { "分類名稱不可空白" }
            require(category.name.length <= 40) { "分類名稱過長" }
            require(category.iconKey.length <= 40) { "分類圖示代碼過長" }
            require(category.type in EntryType.entries.map { it.name }) { "分類收支類型無效" }
            category.id
        }
        require(categoryIds.size == categoryIds.toSet().size) { "備份檔包含重複的分類 ID" }
        val categoriesById = document.categories.associateBy { it.id }

        val transactionIds = document.transactions.map { transaction ->
            require(transaction.id in 1..MAX_ID) { "帳目 ID 超過允許範圍" }
            require(transaction.amount in 1..MAX_AMOUNT) { "帳目金額超過允許範圍" }
            require(transaction.type in EntryType.entries.map { it.name }) { "帳目收支類型無效" }
            require(transaction.note.length <= 100) { "帳目備註過長" }
            val category = requireNotNull(categoriesById[transaction.categoryId]) { "帳目參照不存在的分類" }
            require(category.type == transaction.type) { "帳目與分類的收支類型不一致" }
            try {
                LocalDate.ofEpochDay(transaction.occurredEpochDay)
                require(transaction.occurredEpochDay in minEpochDay..maxEpochDay) { "帳目日期超過允許範圍" }
            } catch (error: Exception) {
                throw IllegalArgumentException("帳目日期無效", error)
            }
            transaction.id
        }
        require(transactionIds.size == transactionIds.toSet().size) { "備份檔包含重複的帳目 ID" }

        val months = document.budgets.map { budget ->
            require(budget.amount in 0..MAX_AMOUNT) { "預算金額超過允許範圍" }
            try {
                YearMonth.parse(budget.yearMonth)
            } catch (error: Exception) {
                throw IllegalArgumentException("預算月份格式無效", error)
            }
            budget.yearMonth
        }
        require(months.size == months.toSet().size) { "備份檔包含重複的預算月份" }
        return document
    }

    fun readUtf8WithLimit(input: InputStream, maxBytes: Int = MAX_BACKUP_BYTES): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            total += read
            require(total <= maxBytes) { "備份檔不可超過 ${maxBytes / 1024 / 1024} MB" }
            output.write(buffer, 0, read)
        }
        return output.toString(Charsets.UTF_8.name())
    }
}
