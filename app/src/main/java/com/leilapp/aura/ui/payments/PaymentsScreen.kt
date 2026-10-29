package com.leilapp.aura.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.leilapp.aura.data.AuraRepository
import com.leilapp.aura.data.PaymentEntity
import com.leilapp.aura.domain.formatAmount
import com.leilapp.aura.domain.parseAmountToCents
import com.leilapp.aura.domain.relativeLabel
import com.leilapp.aura.ui.auraViewModel
import com.leilapp.aura.ui.components.AuraCard
import com.leilapp.aura.ui.components.AuraScaffold
import com.leilapp.aura.ui.components.AuraTextField
import com.leilapp.aura.ui.components.CenteredContent
import com.leilapp.aura.ui.components.ConfirmDeleteDialog
import com.leilapp.aura.ui.components.DateField
import com.leilapp.aura.ui.components.EmptyState
import com.leilapp.aura.ui.components.OptionDropdown
import com.leilapp.aura.ui.components.ReminderDropdown
import com.leilapp.aura.ui.components.SectionHeader
import com.leilapp.aura.ui.theme.AuraColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

val PAYMENT_CATEGORIES = listOf("Servicios", "Vivienda", "Escuela", "Tarjeta", "Préstamo", "Suscripción", "Salud", "Seguro", "Otro")

class PaymentsViewModel(private val repository: AuraRepository) : ViewModel() {
    val payments = repository.payments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun togglePaid(p: PaymentEntity) = viewModelScope.launch { repository.setPaymentPaid(p, !p.paid) }
    fun delete(p: PaymentEntity, after: () -> Unit = {}) = viewModelScope.launch { repository.deletePayment(p.id); after() }
    suspend fun load(id: Long) = repository.getPayment(id)
    fun save(p: PaymentEntity, after: () -> Unit) = viewModelScope.launch { repository.savePayment(p); after() }
}

@Composable
fun PaymentsScreen(onBack: () -> Unit, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val vm = auraViewModel { PaymentsViewModel(it.repository) }
    val payments by vm.payments.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val pending = payments.orEmpty().filter { !it.paid }
    val paid = payments.orEmpty().filter { it.paid }

    AuraScaffold(
        title = "Pagos",
        onBack = onBack,
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Nuevo pago") } },
    ) { padding ->
        CenteredContent(padding) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        "Aura solo registra fechas e importes para recordarte. No se conecta a bancos ni realiza pagos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (payments?.isEmpty() == true) {
                    item {
                        EmptyState(Icons.Filled.Info, "Sin pagos registrados", "Registra servicios, colegiaturas o tarjetas con su fecha de vencimiento.", actionLabel = "Nuevo pago", onAction = onAdd)
                    }
                }
                if (pending.isNotEmpty()) {
                    item {
                        AuraCard {
                            Text("Pendiente total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatAmount(pending.sumOf { it.amountCents }), style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    item { SectionHeader("Pendientes") }
                    items(pending, key = { it.id }) { p -> PaymentRow(p, today, { onOpen(p.id) }, { vm.togglePaid(p) }) }
                }
                if (paid.isNotEmpty()) {
                    item { SectionHeader("Pagados") }
                    items(paid, key = { it.id }) { p -> PaymentRow(p, today, { onOpen(p.id) }, { vm.togglePaid(p) }) }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun PaymentRow(p: PaymentEntity, today: LocalDate, onOpen: () -> Unit, onToggle: () -> Unit) {
    val due = LocalDate.ofEpochDay(p.dueEpochDay)
    val overdue = !p.paid && due.isBefore(today)
    AuraCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = p.paid, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium)
                val cat = p.category.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                Text(
                    (if (overdue) "Vencido · " else "Vence: ") + due.relativeLabel(today) + cat,
                    color = if (overdue) AuraColors.Error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(formatAmount(p.amountCents), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun PaymentEditScreen(paymentId: Long, onDone: () -> Unit) {
    val vm = auraViewModel { PaymentsViewModel(it.repository) }
    var original by remember { mutableStateOf<PaymentEntity?>(null) }
    var loaded by rememberSaveable { mutableStateOf(paymentId == 0L) }
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var due by rememberSaveable { mutableStateOf<Long?>(LocalDate.now().toEpochDay()) }
    var category by rememberSaveable { mutableStateOf("") }
    var paid by rememberSaveable { mutableStateOf(false) }
    var reminder by rememberSaveable { mutableStateOf<Int?>(1440) }
    var notes by rememberSaveable { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(paymentId) {
        if (paymentId != 0L) {
            vm.load(paymentId)?.let { p ->
                original = p
                if (!loaded) {
                    name = p.name; amount = formatAmount(p.amountCents).removePrefix("$"); due = p.dueEpochDay
                    category = p.category; paid = p.paid; reminder = p.reminderMinutesBefore; notes = p.notes
                }
            }
            loaded = true
        }
    }

    AuraScaffold(
        title = if (paymentId == 0L) "Nuevo pago" else "Editar pago",
        onBack = onDone,
        actions = { if (original != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar pago") } },
    ) { padding ->
        CenteredContent(padding) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AuraTextField(name, { name = it; nameError = null }, "Nombre del pago *", error = nameError)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; amountError = null },
                    label = { Text("Importe *") },
                    prefix = { Text("$") },
                    singleLine = true,
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                DateField("Vence", due, { due = it }, allowClear = false)
                OptionDropdown("Categoría", listOf("" to "Sin categoría") + PAYMENT_CATEGORIES.map { it to it }, category) { category = it }
                ReminderDropdown(reminder, { reminder = it }, hasTime = false)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = paid, onCheckedChange = { paid = it })
                    Text("Pagado")
                }
                AuraTextField(notes, { notes = it }, "Notas", singleLine = false)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDone) { Text("Cancelar") }
                    Button(enabled = loaded, onClick = {
                        val cents = parseAmountToCents(amount)
                        when {
                            name.isBlank() -> nameError = "Escribe un nombre"
                            cents == null -> amountError = "Escribe un importe válido, por ejemplo 1250.50"
                            else -> vm.save(
                                (original ?: PaymentEntity(name = "", amountCents = 0, dueEpochDay = 0)).copy(
                                    name = name.trim(), amountCents = cents, dueEpochDay = due ?: LocalDate.now().toEpochDay(),
                                    category = category, paid = paid, reminderMinutesBefore = reminder, notes = notes.trim(),
                                ),
                                onDone,
                            )
                        }
                    }) { Text("Guardar") }
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDeleteDialog(name, onConfirm = { original?.let { vm.delete(it, onDone) } }, onDismiss = { confirmDelete = false })
    }
}
