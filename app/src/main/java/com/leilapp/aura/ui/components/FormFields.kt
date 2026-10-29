package com.leilapp.aura.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.leilapp.aura.data.Priority
import com.leilapp.aura.domain.REMINDER_OPTIONS
import com.leilapp.aura.domain.minutesLabel
import com.leilapp.aura.domain.relativeLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun AuraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Selector de fecha. Material 3 trabaja en milisegundos UTC; aquí se convierte a LocalDate. */
@Composable
fun DateField(label: String, epochDay: Long?, onChange: (Long?) -> Unit, allowClear: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.DateRange, contentDescription = null)
            Text(
                "  $label: " + (epochDay?.let { LocalDate.ofEpochDay(it).relativeLabel() } ?: "sin fecha"),
            )
        }
        if (allowClear && epochDay != null) {
            IconButton(onClick = { onChange(null) }) { Icon(Icons.Filled.Clear, contentDescription = "Quitar $label") }
        }
    }
    if (open) {
        val initial = (epochDay ?: LocalDate.now().toEpochDay()) * 86_400_000L
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay())
                    }
                    open = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun TimeField(label: String, minutes: Int?, onChange: (Int?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.weight(1f)) {
            Text("$label: " + (minutes?.let { minutesLabel(it) } ?: "sin hora"))
        }
        if (minutes != null) {
            IconButton(onClick = { onChange(null) }) { Icon(Icons.Filled.Clear, contentDescription = "Quitar $label") }
        }
    }
    if (open) {
        val state = rememberTimePickerState(
            initialHour = (minutes ?: 9 * 60) / 60,
            initialMinute = (minutes ?: 0) % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = { onChange(state.hour * 60 + state.minute); open = false }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun <T> OptionDropdown(label: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selected }?.second ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(value); expanded = false })
            }
        }
    }
}

@Composable
fun ReminderDropdown(selected: Int?, onSelect: (Int?) -> Unit, hasTime: Boolean = true) {
    val options = if (hasTime) REMINDER_OPTIONS
    else REMINDER_OPTIONS.map { (v, l) -> v to if (v == 0) "El mismo día (9:00)" else l }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        OptionDropdown("Recordatorio", options, selected, onSelect)
    }
}

@Composable
fun PrioritySelector(priority: Int, onChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Priority.LOW, Priority.NORMAL, Priority.HIGH).forEach { value ->
            FilterChip(
                selected = priority == value,
                onClick = { onChange(value) },
                label = { Text(Priority.label(value)) },
            )
        }
    }
}

@Composable
fun <T> ChipSelector(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, text) ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(text) })
        }
    }
}
