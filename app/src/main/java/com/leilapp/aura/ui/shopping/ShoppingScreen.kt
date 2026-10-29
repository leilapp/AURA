package com.leilapp.aura.ui.shopping

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.leilapp.aura.data.AuraRepository
import com.leilapp.aura.data.ShoppingItem
import com.leilapp.aura.data.ShoppingList
import com.leilapp.aura.ui.auraViewModel
import com.leilapp.aura.ui.components.AuraCard
import com.leilapp.aura.ui.components.AuraScaffold
import com.leilapp.aura.ui.components.AuraTextField
import com.leilapp.aura.ui.components.CenteredContent
import com.leilapp.aura.ui.components.ConfirmDeleteDialog
import com.leilapp.aura.ui.components.EmptyState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingState(val loaded: Boolean = false, val lists: List<ShoppingList> = emptyList(), val items: List<ShoppingItem> = emptyList())

class ShoppingViewModel(private val repository: AuraRepository) : ViewModel() {
    val state = combine(repository.shoppingLists, repository.shoppingItems) { l, i -> ShoppingState(true, l, i) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingState())

    fun addList(name: String, onCreated: (Long) -> Unit) = viewModelScope.launch { onCreated(repository.addShoppingList(name)) }
    fun renameList(list: ShoppingList, name: String) = viewModelScope.launch { repository.renameShoppingList(list, name) }
    fun deleteList(list: ShoppingList) = viewModelScope.launch { repository.deleteShoppingList(list) }
    fun saveItem(item: ShoppingItem) = viewModelScope.launch { repository.saveShoppingItem(item) }
    fun deleteItem(item: ShoppingItem) = viewModelScope.launch { repository.deleteShoppingItem(item) }
    fun clearPurchased(listId: Long) = viewModelScope.launch { repository.clearPurchased(listId) }
}

private sealed interface ShoppingDialog {
    data class ListName(val list: ShoppingList?) : ShoppingDialog
    data class Item(val item: ShoppingItem) : ShoppingDialog
    data class DeleteList(val list: ShoppingList) : ShoppingDialog
    data class DeleteItem(val item: ShoppingItem) : ShoppingDialog
}

@Composable
fun ShoppingScreen(onBack: () -> Unit) {
    val vm = auraViewModel { ShoppingViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dialog by remember { mutableStateOf<ShoppingDialog?>(null) }

    LaunchedEffect(state.lists) {
        if (state.lists.none { it.id == selectedId }) selectedId = state.lists.firstOrNull()?.id
    }
    val selected = state.lists.firstOrNull { it.id == selectedId }
    val items = state.items.filter { it.listId == selectedId }

    AuraScaffold(
        title = "Compras",
        onBack = onBack,
        actions = {
            if (selected != null) {
                IconButton(onClick = { dialog = ShoppingDialog.ListName(selected) }) { Icon(Icons.Filled.Edit, contentDescription = "Renombrar lista") }
                IconButton(onClick = { dialog = ShoppingDialog.DeleteList(selected) }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar lista") }
            }
        },
        floatingActionButton = {
            if (selected != null) {
                FloatingActionButton(onClick = { dialog = ShoppingDialog.Item(ShoppingItem(listId = selected.id, name = "")) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar artículo")
                }
            }
        },
    ) { padding ->
        CenteredContent(padding) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.lists.forEach { l ->
                        FilterChip(selected = l.id == selectedId, onClick = { selectedId = l.id }, label = { Text(l.name) })
                    }
                    TextButton(onClick = { dialog = ShoppingDialog.ListName(null) }) { Text("+ Nueva lista") }
                }
                if (state.loaded && state.lists.isEmpty()) {
                    EmptyState(
                        Icons.Filled.ShoppingCart, "Sin listas de compras",
                        "Crea listas como «Súper», «Farmacia» o «Escuela».",
                        actionLabel = "Crear lista", onAction = { dialog = ShoppingDialog.ListName(null) },
                    )
                } else if (selected != null) {
                    val pending = items.count { !it.purchased }
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$pending por comprar · ${items.size - pending} comprados", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (items.any { it.purchased }) TextButton(onClick = { vm.clearPurchased(selected.id) }) { Text("Quitar comprados") }
                            }
                        }
                        if (items.isEmpty()) {
                            item { Text("Lista vacía. Usa el botón + para agregar artículos.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        items(items, key = { it.id }) { item ->
                            AuraCard(onClick = { dialog = ShoppingDialog.Item(item) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = item.purchased, onCheckedChange = { vm.saveItem(item.copy(purchased = it)) })
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            item.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            textDecoration = if (item.purchased) TextDecoration.LineThrough else null,
                                            color = if (item.purchased) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (item.quantity.isNotBlank()) Text("Cantidad: ${item.quantity}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { dialog = ShoppingDialog.DeleteItem(item) }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${item.name}") }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }

    when (val d = dialog) {
        is ShoppingDialog.ListName -> NameDialog(
            title = if (d.list == null) "Nueva lista" else "Renombrar lista",
            initial = d.list?.name ?: "",
            onDismiss = { dialog = null },
            onConfirm = { name ->
                if (d.list == null) vm.addList(name) { selectedId = it } else vm.renameList(d.list, name)
                dialog = null
            },
        )
        is ShoppingDialog.Item -> ItemDialog(d.item, onDismiss = { dialog = null }) { vm.saveItem(it); dialog = null }
        is ShoppingDialog.DeleteList -> ConfirmDeleteDialog("${d.list.name} y todos sus artículos", onConfirm = { vm.deleteList(d.list) }, onDismiss = { dialog = null })
        is ShoppingDialog.DeleteItem -> ConfirmDeleteDialog(d.item.name, onConfirm = { vm.deleteItem(d.item) }, onDismiss = { dialog = null })
        null -> {}
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { AuraTextField(name, { name = it }, "Nombre") },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name.trim()) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun ItemDialog(item: ShoppingItem, onDismiss: () -> Unit, onSave: (ShoppingItem) -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var quantity by remember { mutableStateOf(item.quantity) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.id == 0L) "Agregar artículo" else "Editar artículo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AuraTextField(name, { name = it }, "Artículo")
                AuraTextField(quantity, { quantity = it }, "Cantidad (ej. 2, 1 kg)")
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(item.copy(name = name.trim(), quantity = quantity.trim())) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
