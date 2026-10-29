package com.leilapp.aura.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.leilapp.aura.ui.components.AuraCard
import com.leilapp.aura.ui.components.AuraScaffold
import com.leilapp.aura.ui.components.CenteredContent

@Composable
fun MoreScreen(onRoutines: () -> Unit, onShopping: () -> Unit, onPayments: () -> Unit, onWeather: () -> Unit, onSettings: () -> Unit) {
    AuraScaffold(title = "Más") { padding ->
        CenteredContent(padding) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Entry(Icons.Filled.Refresh, "Rutinas", "Hábitos por día de la semana y progreso diario", onRoutines) }
                item { Entry(Icons.Filled.ShoppingCart, "Compras", "Listas con cantidades y artículos comprados", onShopping) }
                item { Entry(Icons.Filled.Info, "Pagos", "Vencimientos, importes y recordatorios", onPayments) }
                item { Entry(Icons.Filled.Star, "Clima y vestimenta", "Pronóstico y sugerencias de qué ponerte", onWeather) }
                item { Entry(Icons.Filled.Settings, "Ajustes", "Tema, notificaciones, categorías y datos de ejemplo", onSettings) }
            }
        }
    }
}

@Composable
private fun Entry(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    AuraCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
