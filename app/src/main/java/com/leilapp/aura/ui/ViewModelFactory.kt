package com.leilapp.aura.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leilapp.aura.AppContainer
import com.leilapp.aura.AuraApp

/** Crea un ViewModel con acceso al contenedor de dependencias de la app. */
@Composable
inline fun <reified VM : ViewModel> auraViewModel(crossinline create: (AppContainer) -> VM): VM =
    viewModel(
        factory = viewModelFactory {
            initializer { create((this[APPLICATION_KEY] as AuraApp).container) }
        },
    )
