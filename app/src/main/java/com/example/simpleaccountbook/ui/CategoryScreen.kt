package com.example.simpleaccountbook.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.simpleaccountbook.AppUiState
import com.example.simpleaccountbook.AppViewModel
import com.example.simpleaccountbook.data.CategoryEntity
import com.example.simpleaccountbook.data.EntryType

@Composable
fun CategoryScreen(state: AppUiState, viewModel: AppViewModel, padding: PaddingValues) {
    var typeName by rememberSaveable { mutableStateOf(EntryType.EXPENSE.name) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val categories = state.categories.filter { it.type == typeName }.sortedBy { it.sortOrder }

    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(EntryType.EXPENSE to "支出分類", EntryType.INCOME to "收入分類").forEach { (type, label) ->
                FilterChip(selected = typeName == type.name, onClick = { typeName = type.name }, label = { Text(label, fontWeight = if (typeName == type.name) FontWeight.Bold else FontWeight.Normal) })
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
            items(categories, key = { it.id }) { category ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(category.name, fontWeight = FontWeight.SemiBold)
                            if (!category.isActive) Text("已停用", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = { viewModel.moveCategory(category, -1) }, enabled = categories.firstOrNull()?.id != category.id) { Icon(Icons.Default.KeyboardArrowUp, "上移") }
                        IconButton(onClick = { viewModel.moveCategory(category, 1) }, enabled = categories.lastOrNull()?.id != category.id) { Icon(Icons.Default.KeyboardArrowDown, "下移") }
                        Switch(checked = category.isActive, onCheckedChange = { viewModel.toggleCategory(category) })
                        IconButton(onClick = { editingId = category.id }) { Icon(Icons.Default.Edit, "改名") }
                        IconButton(onClick = { deletingId = category.id }) { Icon(Icons.Default.Delete, "刪除") }
                    }
                }
            }
        }
        Button(onClick = { adding = true }, Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Icon(Icons.Default.Add, null)
            Text("新增分類", Modifier.padding(start = 8.dp))
        }
    }

    if (adding) {
        CategoryNameDialog("新增分類", "", onDismiss = { adding = false }) { name ->
            viewModel.saveCategory(CategoryEntity(type = typeName, name = name, iconKey = "other", sortOrder = (categories.maxOfOrNull { it.sortOrder } ?: -1) + 1))
            adding = false
        }
    }
    editingId?.let { id ->
        state.categories.firstOrNull { it.id == id }?.let { category ->
            CategoryNameDialog("編輯分類", category.name, onDismiss = { editingId = null }) { name ->
                viewModel.saveCategory(category.copy(name = name))
                editingId = null
            }
        }
    }
    deletingId?.let { id ->
        state.categories.firstOrNull { it.id == id }?.let { category ->
            AlertDialog(
                onDismissRequest = { deletingId = null },
                title = { Text("刪除「${category.name}」？") },
                text = { Text("若已有帳目使用此分類，分類會改為停用並保留歷史資料。") },
                confirmButton = { TextButton(onClick = { viewModel.deleteCategory(category); deletingId = null }) { Text("刪除") } },
                dismissButton = { TextButton(onClick = { deletingId = null }) { Text("取消") } },
            )
        }
    }
}

@Composable
private fun CategoryNameDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it.take(12) }, label = { Text("分類名稱") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text("儲存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
