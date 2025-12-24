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
import androidx.compose.foundation.text.KeyboardOptions
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
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Delete

import com.google.firebase.Timestamp
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import java.util.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState



class MainActivity : ComponentActivity() {

    private val medicineRepo by lazy { (application as MyPharmacyApp).medicineRepository }
    private val categoryRepo by lazy { (application as MyPharmacyApp).categoryRepository }

    private val history = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MyPharmacyAppUI() }
    }

    @Composable
    fun MyPharmacyAppUI() {
        val navController = rememberNavController()
        Scaffold(bottomBar = { BottomNavigationBar(navController) }) { padding ->
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
                    MedicineCard(
                        medicine,
                        onEdit = { editedMedicine = it; showDialog = true },
                        onDelete = { medicineToDelete ->
                            lifecycleScope.launch {
                                medicineRepo.delete(medicineToDelete)
                                addHistory("Удалено", medicineToDelete.name)
                            }
                        }
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
                                addHistory("Добавлено", med.name)
                            } else {
                                medicineRepo.update(med)
                                addHistory("Изменено", med.name)
                            }
                            showDialog = false
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun MedicineCard(
        medicine: Medicine,
        onEdit: (Medicine) -> Unit,
        onDelete: (Medicine) -> Unit
    ) {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val expirationDateStr = medicine.expirationDate?.toDate()?.let { sdf.format(it) } ?: "-"
        val daysLeft = medicine.expirationDate?.toDate()?.let {
            ((it.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
        } ?: -1

        Card(
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
                    Text(
                        "Срок годности: $expirationDateStr",
                        color = when {
                            daysLeft < 0 -> Color.Gray
                            daysLeft in 0..30 -> Color.Red
                            else -> Color.Black
                        },
                        fontWeight = if (daysLeft in 0..30) FontWeight.Bold else FontWeight.Normal
                    )
                }
                IconButton(onClick = { onDelete(medicine) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }
        }
    }

    /** ========== Medicine Dialog ========== */
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
        var expiration by remember { mutableStateOf(medicine?.expirationDate?.toDate()) }

        // Для управления DatePicker
        val context = LocalContext.current
        var showDatePicker by remember { mutableStateOf(false) }

        // Для Material3 DatePicker (более современный подход)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = expiration?.time ?: System.currentTimeMillis()
        )

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                expiration = Date(it)
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDatePicker = false }
                    ) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (medicine == null) "Добавить лекарство" else "Редактировать") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Дозировка") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val current = quantity.toIntOrNull() ?: 0
                            if (current > 0) quantity = (current - 1).toString()
                        }) { Icon(Icons.Default.Remove, contentDescription = "Минус") }

                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Количество") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        IconButton(onClick = {
                            val current = quantity.toIntOrNull() ?: 0
                            quantity = (current + 1).toString()
                        }) { Icon(Icons.Default.Add, contentDescription = "Плюс") }
                    }
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Категория") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    // Срок годности - кликабельное поле
                    OutlinedTextField(
                        value = expiration?.let {
                            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(it)
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Срок годности") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        trailingIcon = {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Выбрать дату",
                                modifier = Modifier.clickable { showDatePicker = true }
                            )
                        }
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
                                category = category,
                                expirationDate = expiration?.let { Timestamp(it) }
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
                items(medicines) { MedicineCard(it, {}, {}) }
            }
        }
    }

    /** ========== History Screen ========== */
    @Composable
    fun HistoryScreen() {
        Column(Modifier.padding(16.dp)) {
            Text("История", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LazyColumn {
                items(history) { h ->
                    Text(h)
                    Spacer(Modifier.height(4.dp))
                }
            }
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

    private fun addHistory(action: String, name: String) {
        val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        history.add(0, "$timestamp - $action - $name")
    }
}







