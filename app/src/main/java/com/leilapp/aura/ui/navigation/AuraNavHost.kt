package com.leilapp.aura.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.leilapp.aura.data.EventKind
import com.leilapp.aura.ui.agenda.AgendaScreen
import com.leilapp.aura.ui.agenda.EventEditScreen
import com.leilapp.aura.ui.child.ChildScreen
import com.leilapp.aura.ui.more.MoreScreen
import com.leilapp.aura.ui.payments.PaymentEditScreen
import com.leilapp.aura.ui.payments.PaymentsScreen
import com.leilapp.aura.ui.routines.RoutineEditScreen
import com.leilapp.aura.ui.routines.RoutinesScreen
import com.leilapp.aura.ui.settings.SettingsScreen
import com.leilapp.aura.ui.shopping.ShoppingScreen
import com.leilapp.aura.ui.tasks.TaskEditScreen
import com.leilapp.aura.ui.tasks.TasksScreen
import com.leilapp.aura.ui.today.TodayActions
import com.leilapp.aura.ui.today.TodayScreen
import android.net.Uri

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Mi día", Icons.Filled.Home),
    AGENDA("agenda", "Agenda", Icons.Filled.DateRange),
    CHILD("child", "Mi hijo", Icons.Filled.Face),
    TASKS("tasks", "Tareas", Icons.Filled.CheckCircle),
    MORE("more", "Más", Icons.Filled.Menu),
}

private object Routes {
    const val TASK = "task/{id}?category={category}"
    const val EVENT = "event/{id}?child={child}&kind={kind}&day={day}"
    const val ROUTINES = "routines"
    const val ROUTINE = "routine/{id}"
    const val SHOPPING = "shopping"
    const val PAYMENTS = "payments"
    const val PAYMENT = "payment/{id}"
    const val WEATHER = "weather"
    const val SETTINGS = "settings"

    fun task(id: Long = 0, category: String? = null) = "task/$id" + (category?.let { "?category=${Uri.encode(it)}" } ?: "")
    fun event(id: Long = 0, child: Boolean = false, kind: String? = null, day: Long? = null): String {
        val params = buildList {
            add("child=$child")
            kind?.let { add("kind=${Uri.encode(it)}") }
            day?.let { add("day=$it") }
        }
        return "event/$id?" + params.joinToString("&")
    }
    fun routine(id: Long = 0) = "routine/$id"
    fun payment(id: Long = 0) = "payment/$id"
}

@Composable
fun AuraNavHost(nav: NavHostController = rememberNavController()) {
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = Tab.entries.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.TODAY.route,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
                .consumeWindowInsets(PaddingValues(bottom = padding.calculateBottomPadding())),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            composable(Tab.TODAY.route) {
                TodayScreen(
                    TodayActions(
                        addTask = { nav.navigate(Routes.task()) },
                        addEvent = { nav.navigate(Routes.event()) },
                        addChild = { nav.navigate(Routes.event(child = true)) },
                        addReminder = { nav.navigate(Routes.event(kind = EventKind.REMINDER)) },
                        openTask = { nav.navigate(Routes.task(it)) },
                        openEvent = { nav.navigate(Routes.event(it)) },
                        openChild = { nav.navigate(Tab.CHILD.route) { launchSingleTop = true } },
                        openTasks = { nav.navigate(Tab.TASKS.route) { launchSingleTop = true } },
                        openRoutines = { nav.navigate(Routes.ROUTINES) },
                        openPayments = { nav.navigate(Routes.PAYMENTS) },
                        openWeather = { nav.navigate(Routes.WEATHER) },
                    ),
                )
            }
            composable(Tab.AGENDA.route) {
                AgendaScreen(
                    onAdd = { nav.navigate(Routes.event(day = it)) },
                    onOpen = { nav.navigate(Routes.event(it)) },
                    onOpenTask = { nav.navigate(Routes.task(it)) },
                )
            }
            composable(Tab.CHILD.route) {
                ChildScreen(onAdd = { nav.navigate(Routes.event(child = true, kind = it)) }, onOpen = { nav.navigate(Routes.event(it)) })
            }
            composable(Tab.TASKS.route) {
                TasksScreen(onAdd = { nav.navigate(Routes.task()) }, onOpen = { nav.navigate(Routes.task(it)) })
            }
            composable(Tab.MORE.route) {
                MoreScreen(
                    onRoutines = { nav.navigate(Routes.ROUTINES) },
                    onShopping = { nav.navigate(Routes.SHOPPING) },
                    onPayments = { nav.navigate(Routes.PAYMENTS) },
                    onWeather = { nav.navigate(Routes.WEATHER) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(
                Routes.TASK,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { entry ->
                TaskEditScreen(
                    taskId = entry.arguments?.getLong("id") ?: 0L,
                    presetCategoryName = entry.arguments?.getString("category"),
                    onDone = { nav.popBackStack() },
                )
            }
            composable(
                Routes.EVENT,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("child") { type = NavType.BoolType; defaultValue = false },
                    navArgument("kind") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("day") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) { entry ->
                val args = entry.arguments
                EventEditScreen(
                    eventId = args?.getLong("id") ?: 0L,
                    presetChild = args?.getBoolean("child") ?: false,
                    presetKind = args?.getString("kind"),
                    presetDay = args?.getLong("day")?.takeIf { it >= 0 },
                    onDone = { nav.popBackStack() },
                )
            }
            composable(Routes.ROUTINES) {
                RoutinesScreen(onBack = { nav.popBackStack() }, onAdd = { nav.navigate(Routes.routine()) }, onOpen = { nav.navigate(Routes.routine(it)) })
            }
            composable(Routes.ROUTINE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                RoutineEditScreen(routineId = entry.arguments?.getLong("id") ?: 0L, onDone = { nav.popBackStack() })
            }
            composable(Routes.SHOPPING) { ShoppingScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.PAYMENTS) {
                PaymentsScreen(onBack = { nav.popBackStack() }, onAdd = { nav.navigate(Routes.payment()) }, onOpen = { nav.navigate(Routes.payment(it)) })
            }
            composable(Routes.PAYMENT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                PaymentEditScreen(paymentId = entry.arguments?.getLong("id") ?: 0L, onDone = { nav.popBackStack() })
            }
            composable(Routes.WEATHER) { com.leilapp.aura.ui.weather.WeatherScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
