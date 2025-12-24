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

import androidx.compose.ui.draw.clip

import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.CheckCircle

import java.util.Date

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.SquareFoot


import androidx.compose.ui.text.style.TextOverflow


class MainActivity : ComponentActivity() {

    private val medicineRepo by lazy { (application as MyPharmacyApp).medicineRepository }
    private val categoryRepo by lazy { (application as MyPharmacyApp).categoryRepository }

    private val history = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyPharmacyAppUI()
        }
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
            NavItem("medicines", "Лекарства", Icons.Default.Medication),
            NavItem("search", "Поиск", Icons.Default.Search),
            NavItem("history", "История", Icons.Default.History),
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
        var selectedFilter by remember { mutableStateOf("Все") }
        val filters = listOf("Все", "Срок истекает", "Просрочено", "Норма")

        // Получаем категории для фильтра
        val categories = remember { mutableStateListOf<String>() }
        LaunchedEffect(Unit) {
            categories.addAll(categoryRepo.getAllCategories())
        }

        // Фильтрация лекарств
        val filteredMedicines = remember(medicines, selectedFilter) {
            when (selectedFilter) {
                "Срок истекает" -> {
                    medicines.filter { medicine ->
                        medicine.expirationDate?.toDate()?.let { expDate ->
                            val expCalendar = Calendar.getInstance().apply { time = expDate }
                            val now = Calendar.getInstance()
                            val oneMonthLater = Calendar.getInstance().apply {
                                add(Calendar.MONTH, 1)
                            }
                            expCalendar.after(now) && expCalendar.before(oneMonthLater)
                        } ?: false
                    }
                }
                "Просрочено" -> {
                    medicines.filter { medicine ->
                        medicine.expirationDate?.toDate()?.let { expDate ->
                            val expCalendar = Calendar.getInstance().apply { time = expDate }
                            val now = Calendar.getInstance()
                            expCalendar.before(now)
                        } ?: false
                    }
                }
                "Норма" -> {
                    medicines.filter { medicine ->
                        medicine.expirationDate?.toDate()?.let { expDate ->
                            val expCalendar = Calendar.getInstance().apply { time = expDate }
                            val now = Calendar.getInstance()
                            val oneMonthLater = Calendar.getInstance().apply {
                                add(Calendar.MONTH, 1)
                            }
                            expCalendar.after(oneMonthLater)
                        } ?: true
                    }
                }
                else -> medicines
            }.sortedBy { it.expirationDate?.toDate()?.time ?: Long.MAX_VALUE }
        }

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Упрощенные фильтры
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(filter, fontSize = 12.sp)
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredMedicines) { medicine ->
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
            }

            if (showDialog) {
                MedicineDialog(
                    medicine = editedMedicine,
                    categories = categories,
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

    // Функция для определения цвета срока годности
    fun getExpirationColor(medicine: Medicine): Color {
        return medicine.expirationDate?.toDate()?.let { expDate ->
            val calendar = Calendar.getInstance()
            val expCalendar = Calendar.getInstance().apply { time = expDate }
            val now = Calendar.getInstance()
            val oneMonthLater = Calendar.getInstance().apply {
                add(Calendar.MONTH, 1)
            }

            if (expCalendar.before(now)) {
                Color.Red // Просрочено
            } else if (expCalendar.before(oneMonthLater)) {
                Color(0xFFFFA500) // Желтый/оранжевый - скоро истекает
            } else {
                Color(0xFF4CAF50) // Зеленый - всё хорошо
            }
        } ?: Color.Gray
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

        val expirationColor = getExpirationColor(medicine)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { onEdit(medicine) }
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Верхняя строка с названием и кнопкой удаления
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        medicine.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(
                        onClick = { onDelete(medicine) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }

                // Описание лекарства (если есть)
                if (!medicine.description.isNullOrEmpty()) {
                    Text(
                        text = medicine.description,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Детали лекарства
                Column {
                    Text("Дозировка: ${medicine.dosage}", fontSize = 14.sp)
                    Text("Количество: ${medicine.quantity}", fontSize = 14.sp)
                    Text("Категория: ${medicine.category}", fontSize = 14.sp)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Срок годности",
                            tint = expirationColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            " Срок годности: $expirationDateStr",
                            color = expirationColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    if (daysLeft >= 0) {
                        Text(
                            text = when {
                                daysLeft == 0 -> "Истекает сегодня!"
                                daysLeft < 0 -> "Просрочено на ${-daysLeft} дней"
                                daysLeft <= 30 -> "Осталось $daysLeft дней"
                                else -> "Осталось $daysLeft дней"
                            },
                            fontSize = 12.sp,
                            color = expirationColor
                        )
                    }
                }
            }
        }
    }

    /** ========== Medicine Dialog ========== */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MedicineDialog(
        medicine: Medicine?,
        categories: List<String>,
        onDismiss: () -> Unit,
        onSave: (Medicine) -> Unit
    ) {
        var name by remember { mutableStateOf(medicine?.name ?: "") }
        var dosageValue by remember { mutableStateOf(medicine?.dosage?.split(" ")?.firstOrNull() ?: "") }
        var dosageUnit by remember { mutableStateOf(medicine?.dosage?.split(" ")?.lastOrNull() ?: "мг") }
        var quantityValue by remember { mutableStateOf(medicine?.quantity?.split(" ")?.firstOrNull() ?: "") }
        var quantityUnit by remember { mutableStateOf(medicine?.quantity?.split(" ")?.lastOrNull() ?: "шт") }
        var selectedCategory by remember { mutableStateOf(medicine?.category ?: "") }
        var description by remember { mutableStateOf(medicine?.description ?: "") }
        var expiration by remember { mutableStateOf(medicine?.expirationDate?.toDate()) }

        // Для управления DatePicker и выпадающими списками
        var showDatePicker by remember { mutableStateOf(false) }
        var showCategoryDropdown by remember { mutableStateOf(false) }
        var showDosageUnitDropdown by remember { mutableStateOf(false) }
        var showQuantityUnitDropdown by remember { mutableStateOf(false) }

        // Список единиц измерения для дозировки
        val dosageUnits = listOf("мкг", "мг", "г", "МЕ", "ед", "%", "мл", "л")

        // Список единиц измерения для количества
        val quantityUnits = listOf("шт", "г", "мг", "мкг", "мл", "л", "пак", "уп")

        // Для Material3 DatePicker
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
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    // Дозировка с единицами измерения
                    Text("Дозировка:", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = dosageValue,
                            onValueChange = {
                                // Разрешаем цифры, точку и запятую для дробных значений
                                val filtered = it.filter { ch ->
                                    ch.isDigit() || ch == '.' || ch == ','
                                }
                                dosageValue = filtered
                            },
                            //label = { Text("Количество") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Spacer(Modifier.width(8.dp))

                        // Выбор единицы измерения дозировки
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = dosageUnit,
                                onValueChange = { dosageUnit = it },
                                label = { Text("Единица") },
                                readOnly = true,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDosageUnitDropdown = true },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Выбрать единицу",
                                        modifier = Modifier.clickable { showDosageUnitDropdown = true }
                                    )
                                }
                            )

                            DropdownMenu(
                                expanded = showDosageUnitDropdown,
                                onDismissRequest = { showDosageUnitDropdown = false }
                            ) {
                                dosageUnits.forEach { unit ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(unit)
                                        },
                                        onClick = {
                                            dosageUnit = unit
                                            showDosageUnitDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Подсказка по единицам измерения дозировки


                    Spacer(Modifier.height(8.dp))

                    // Количество в упаковке с единицами измерения
                    Text("Количество в упаковке:", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Кнопки +/-
                        IconButton(
                            onClick = {
                                val current = quantityValue.toIntOrNull() ?: 0
                                if (current > 0) quantityValue = (current - 1).toString()
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Минус")
                        }

                        OutlinedTextField(
                            value = quantityValue,
                            onValueChange = {
                                // Разрешаем только цифры для количества
                                val filtered = it.filter { ch -> ch.isDigit() }
                                quantityValue = filtered
                            },
                            //label = { Text("Количество") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                val current = quantityValue.toIntOrNull() ?: 0
                                quantityValue = (current + 1).toString()
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Плюс")
                        }

                        Spacer(Modifier.width(8.dp))

                        // Выбор единицы измерения количества
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = quantityUnit,
                                onValueChange = { quantityUnit = it },
                                label = { Text("Тип") },
                                readOnly = true,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showQuantityUnitDropdown = true },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Выбрать тип",
                                        modifier = Modifier.clickable { showQuantityUnitDropdown = true }
                                    )
                                }
                            )

                            DropdownMenu(
                                expanded = showQuantityUnitDropdown,
                                onDismissRequest = { showQuantityUnitDropdown = false }
                            ) {
                                quantityUnits.forEach { unit ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                when (unit) {
                                                    "шт" -> "шт (штуки)"
                                                    "г" -> "г (граммы)"
                                                    "мг" -> "мг (миллиграммы)"
                                                    "мкг" -> "мкг (микрограммы)"
                                                    "мл" -> "мл (миллилитры)"
                                                    "л" -> "л (литры)"
                                                    "пак" -> "пак (пакеты)"
                                                    "уп" -> "уп (упаковки)"
                                                    else -> unit
                                                }
                                            )
                                        },
                                        onClick = {
                                            quantityUnit = unit
                                            showQuantityUnitDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }



                    Spacer(Modifier.height(8.dp))

                    // Категория
                    Text("Категория:", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Box {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = { selectedCategory = it },
                            label = { Text("Категория") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCategoryDropdown = true },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Выбрать категорию",
                                    modifier = Modifier.clickable { showCategoryDropdown = true }
                                )
                            }
                        )

                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false }
                        ) {
                            if (categories.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Категорий пока нет",
                                            color = Color.Gray
                                        )
                                    },
                                    onClick = {}
                                )
                            } else {
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = {
                                            selectedCategory = category
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Описание
                    Text("Описание:", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Описание лекарства") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4,
                        singleLine = false
                    )

                    Text(
                        "Укажите назначение, противопоказания или особенности приема",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Spacer(Modifier.height(8.dp))

                    // Срок годности
                    Text("Срок годности:", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                    OutlinedTextField(
                        value = expiration?.let {
                            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(it)
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Дата окончания срока") },
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
                    enabled = name.isNotBlank() && dosageValue.isNotBlank() && quantityValue.isNotBlank(),
                    onClick = {
                        // Формируем строку дозировки для БД
                        val dosageForDB = "$dosageValue $dosageUnit"
                        // Формируем строку количества для БД
                        val quantityForDB = "$quantityValue $quantityUnit"

                        onSave(
                            Medicine(
                                id = medicine?.id ?: "",
                                name = name,
                                dosage = dosageForDB,
                                quantity = quantityForDB,
                                category = selectedCategory,
                                description = description,
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

        // Загружаем категории
        LaunchedEffect(Unit) {
            val cat = categoryRepo.getAllCategories()
            categories = listOf("Все") + cat
        }

        // Получаем все лекарства
        val allMedicines by medicineRepo.allMedicines.collectAsState(initial = emptyList())

        // Фильтруем лекарства при изменении запроса или категории
        LaunchedEffect(query, selectedCategory, allMedicines) {
            medicines = allMedicines.filter { medicine ->
                val matchesSearch = query.isEmpty() ||
                        medicine.name.contains(query, ignoreCase = true) ||
                        medicine.description.contains(query, ignoreCase = true)

                val matchesCategory = selectedCategory == "Все" ||
                        medicine.category == selectedCategory

                matchesSearch && matchesCategory
            }
        }

        Column(Modifier.padding(16.dp)) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Поиск по названию или описанию...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Spacer(Modifier.height(12.dp))

            // Горизонтальный список категорий
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                category,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (medicines.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when {
                            query.isNotEmpty() && selectedCategory != "Все" ->
                                "Не найдено лекарств по запросу '$query' в категории '$selectedCategory'"
                            query.isNotEmpty() -> "Не найдено лекарств по запросу '$query'"
                            selectedCategory != "Все" -> "В категории '$selectedCategory' нет лекарств"
                            else -> "Лекарств пока нет"
                        },
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn {
                    items(medicines) { med ->
                        MedicineCard(med, {}, {})
                    }
                }
            }
        }
    }

    /** ========== History Screen ========== */
    @Composable
    fun HistoryScreen() {
        Column(Modifier.padding(16.dp)) {
            Text("История действий", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("История действий пуста", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(history) { h ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5)
                            )
                        ) {
                            Text(
                                h,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp
                            )
                        }
                    }
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

            var notificationsChecked by remember { mutableStateOf(true) }
            var lowStockChecked by remember { mutableStateOf(true) }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Напоминания об истечении срока", Modifier.weight(1f))
                Switch(checked = notificationsChecked, onCheckedChange = { notificationsChecked = it })
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Уведомления о низком запасе", Modifier.weight(1f))
                Switch(checked = lowStockChecked, onCheckedChange = { lowStockChecked = it })
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Моя аптечка\nПриложение для управления домашней аптекой",
                color = Color(0xFF7A3EFF)
            )
        }
    }

    private fun addHistory(action: String, name: String) {
        val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        history.add(0, "$timestamp - $action - $name")
    }
}






