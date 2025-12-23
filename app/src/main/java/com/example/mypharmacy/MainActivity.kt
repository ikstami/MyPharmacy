package com.example.mypharmacy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
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

    /** ========== Medicines Screen ========== */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MedicinesScreen() {
        val medicines by medicineRepo.allMedicines.collectAsState(initial = emptyList())
        var showDialog by remember { mutableStateOf(false) }
        var editedMedicine by remember { mutableStateOf<Medicine?>(null) }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    editedMedicine = null
                    showDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(medicines) { medicine ->
                    MedicineCardWithActions(
                        medicine,
                        onEdit = { editedMedicine = it; showDialog = true },
                        onDelete = { lifecycleScope.launch { medicineRepo.delete(it) } }
                    )
                }
            }

            if (showDialog) {
                MedicineDialog(
                    medicine = editedMedicine,
                    onDismiss = { showDialog = false },
                    onSave = { med ->
                        lifecycleScope.launch {
                            if (editedMedicine == null) {
                                medicineRepo.insert(med)
                            } else {
                                medicineRepo.update(med)
                            }
                            showDialog = false
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun MedicineCardWithActions(
        medicine: Medicine,
        onEdit: (Medicine) -> Unit,
        onDelete: (Medicine) -> Unit
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { onEdit(medicine) }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(medicine.name, fontWeight = FontWeight.Bold)
                    Text("Дозировка: ${medicine.dosage}", fontSize = 14.sp)
                    Text("Количество: ${medicine.quantity}", fontSize = 14.sp)
                    Text("Категория: ${medicine.category}", fontSize = 14.sp)
                }
                IconButton(onClick = { onDelete(medicine) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MedicineDialog(
        medicine: Medicine?,
        onDismiss: () -> Unit,
        onSave: (Medicine) -> Unit
    ) {
        var name by remember { mutableStateOf(medicine?.name ?: "") }
        var dosage by remember { mutableStateOf(medicine?.dosage ?: "") }
        var quantity by remember { mutableStateOf(medicine?.quantity?.toString() ?: "") }
        var category by remember { mutableStateOf(medicine?.category ?: "") }
        var categories by remember { mutableStateOf(listOf<String>()) }

        // Загружаем категории из Firebase один раз
        LaunchedEffect(Unit) {
            categories = categoryRepo.getAllCategories()
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (medicine == null) "Добавить лекарство" else "Редактировать") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Название") }
                    )
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Дозировка") }
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Количество") }
                    )
                    Spacer(Modifier.height(8.dp))
                    DropdownMenuBox(
                        selected = category,
                        options = categories,
                        onSelected = { category = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onSave(
                            Medicine(
                                id = medicine?.id ?: "",
                                name = name,
                                dosage = dosage,
                                quantity = quantity.toIntOrNull() ?: 0,
                                category = category
                            )
                        )
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        )
    }

    @Composable
    fun DropdownMenuBox(selected: String, options: List<String>, onSelected: (String) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                label = { Text("Категория") },
                readOnly = true,
                modifier = Modifier.clickable { expanded = true }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
                        onClick = {
                            onSelected(opt)
                            expanded = false
                        }
                    )
                }
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

        LaunchedEffect(Unit) {
            categories = categoryRepo.getAllCategories().toMutableList().apply { add(0, "Все") }
        }

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
                items(medicines) { MedicineCardWithActions(it, {}, {}) }
            }
        }
    }

    /** ========== History Screen ========== */
    @Composable
    fun HistoryScreen() {
        Column(Modifier.padding(16.dp)) {
            Text("История", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
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
