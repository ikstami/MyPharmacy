package com.example.mypharmacy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.mypharmacy.data.model.Medicine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.navigation.navOptions

import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState


class MainActivity : ComponentActivity() {

    private val medicineRepo by lazy { (application as MyPharmacyApp).medicineRepository }
    private val categoryRepo by lazy { (application as MyPharmacyApp).categoryRepository }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyPharmacyAppUI()
        }
    }

    @Composable
    fun MyPharmacyAppUI() {
        val navController = rememberNavController()

        Scaffold(
            bottomBar = { BottomNavigationBar(navController) }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "medicines",
                modifier = Modifier.padding(padding)
            ) {
                composable("medicines") { MedicinesScreen() }
                composable("search") { SearchScreen() }
                composable("history") { HistoryScreen() }
                composable("settings") { SettingsScreen() }
            }
        }
    }

    @Composable
    fun BottomNavigationBar(navController: NavHostController) {
        val items = listOf(
            NavItem("medicines", "Лекарства", Icons.Default.Edit),
            NavItem("search", "Поиск", Icons.Default.Search),
            NavItem("history", "История", Icons.Default.List),
            NavItem("settings", "Настройки", Icons.Default.Settings)
        )

        NavigationBar {
            val currentRoute = currentRoute(navController)
            items.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, null) },
                    label = { Text(item.title, fontSize = 11.sp) },
                    selected = currentRoute == item.route,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }

                    }
                )
            }
        }
    }

    data class NavItem(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

    @Composable
    fun currentRoute(navController: NavHostController): String? {
        return navController.currentBackStackEntryAsState().value?.destination?.route
    }

    /** ========== Medicines Screen ========== */
    @Composable
    fun MedicinesScreen() {
        val medicines by medicineRepo.allMedicines.collectAsState(initial = emptyList())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF7ECFF))))
                .padding(16.dp)
        ) {
            Text("Моя аптечка", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LazyColumn {
                items(medicines) { medicine ->
                    MedicineCard(medicine)
                }
            }
        }
    }

    @Composable
    fun MedicineCard(medicine: Medicine) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(medicine.name, fontWeight = FontWeight.Bold)
                Text(medicine.dosage, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text("⏰ ${medicine.expirationDate?.toDate()?.toString() ?: "-"}")
                Text("📦 ${medicine.quantity}")
                AssistChip(onClick = {}, label = { Text(medicine.category) })
            }
        }
    }

    /** ========== Search Screen ========== */
    @Composable
    fun SearchScreen() {
        var query by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("Все") }
        var categories by remember { mutableStateOf(listOf("Все")) }
        var medicines by remember { mutableStateOf(listOf<Medicine>()) }

        // Загружаем категории из Firebase один раз
        LaunchedEffect(Unit) {
            categories = categoryRepo.getAllCategories().toMutableList().apply { add(0, "Все") }
        }

        // Подписка на лекарства с фильтром
        LaunchedEffect(query, selectedCategory) {
            medicineRepo.allMedicines.collectLatest { list ->
                medicines = list.filter {
                    it.name.contains(query, ignoreCase = true) &&
                            (selectedCategory == "Все" || it.category == selectedCategory)
                }
            }
        }

        Column(Modifier.padding(16.dp)) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Поиск по названию...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            Spacer(Modifier.height(12.dp))

            Row {
                categories.forEach {
                    FilterChip(
                        selected = selectedCategory == it,
                        onClick = { selectedCategory = it },
                        label = { Text(it) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn {
                items(medicines) { MedicineCard(it) }
            }
        }
    }

    /** ========== History Screen ========== */
    @Composable
    fun HistoryScreen() {
        Column(Modifier.padding(16.dp)) {
            Text("История", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            // Здесь можно реализовать историю добавлений/удалений
            Text("Здесь будет история операций")
        }
    }

    /** ========== Settings Screen ========== */
    @Composable
    fun SettingsScreen() {
        Column(Modifier.padding(16.dp)) {
            Text("Настройки", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            SettingSwitch("Напоминания об истечении срока")
            SettingSwitch("Низкий запас")
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Моя аптечка\nПриложение для управления домашней аптекой",
                color = Color(0xFF7A3EFF)
            )
        }
    }

    @Composable
    fun SettingSwitch(title: String) {
        var checked by remember { mutableStateOf(true) }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = { checked = it })
        }
    }
}
