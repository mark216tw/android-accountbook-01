package com.example.simpleaccountbook.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.simpleaccountbook.data.ExpenseSlice
import com.example.simpleaccountbook.displayMonth
import com.example.simpleaccountbook.formatMoney
import java.time.YearMonth
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val chartColors = listOf(
    Color(0xFFE05A47), Color(0xFF1976A8), Color(0xFF168568),
    Color(0xFF9A7410), Color(0xFFB33F72), Color(0xFF7856A8),
)
private val otherColor = Color(0xFF777279)

private fun sliceColor(slice: ExpenseSlice): Color = if (slice.isOther) otherColor else {
    chartColors[((slice.categoryId ?: 0L) % chartColors.size).toInt().absoluteValue]
}

@Composable
fun ExpenseDonutChart(month: YearMonth, total: Long, slices: List<ExpenseSlice>) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("支出分類", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (slices.isEmpty() || total <= 0) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("這個月還沒有支出", fontWeight = FontWeight.SemiBold)
                    Text("新增支出後會在這裡顯示分類占比", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val description = buildString {
                    append(month.displayMonth()).append("總支出").append(formatMoney(total)).append("，共")
                        .append(slices.size).append("個分類。")
                    slices.forEach { append(it.categoryName).append(formatMoney(it.amount)).append("；") }
                }
                Box(
                    Modifier.fillMaxWidth().semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(190.dp)) {
                        var start = -90f
                        slices.forEach { slice ->
                            drawArc(
                                color = sliceColor(slice),
                                startAngle = start,
                                sweepAngle = slice.sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Butt),
                            )
                            start += slice.sweepAngle
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("本月支出", style = MaterialTheme.typography.labelMedium)
                        Text(formatMoney(total), fontWeight = FontWeight.Bold)
                    }
                }
                slices.forEach { slice ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(sliceColor(slice), CircleShape))
                        Text(slice.categoryName, Modifier.padding(start = 8.dp).weight(1f))
                        Text(
                            "${(slice.amount.toDouble() * 100 / total).roundToInt()}% · ${formatMoney(slice.amount)}",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
