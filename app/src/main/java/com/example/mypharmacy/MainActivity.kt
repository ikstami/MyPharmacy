package com.example.mypharmacy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.mypharmacy.data.model.Medicine
import kotlinx.coroutines.launch
import java.util.UUID
//trsrорпорролkhhjhлр
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var medicineRepository: MedicineRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        medicineRepository =
            (application as MyPharmacyApp).medicineRepository

        setContent {
            MyPharmacyScreen()
        }
    }

    @Composable
    fun MyPharmacyScreen() {

        val medicines by medicineRepository
            .allMedicines
            .collectAsState(initial = emptyList())

        var showDialog by remember { mutableStateOf(false) }
        var editedMedicine by remember { mutableStateOf<Medicine?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Моя аптечка") }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        editedMedicine = null
                        showDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить")
                }
            }
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                items(medicines) { medicine ->
                    MedicineItem(
                        medicine = medicine,
                        onDelete = {
                            lifecycleScope.launch {
                                medicineRepository.delete(medicine)
                            }
                        },
                        onEdit = {
                            editedMedicine = medicine
                            showDialog = true
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
                                medicineRepository.insert(med)
                            } else {
                                medicineRepository.update(med)
                            }
                            showDialog = false
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun MedicineItem(
        medicine: Medicine,
        onDelete: () -> Unit,
        onEdit: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .clickable { onEdit() }
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
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }
        }
    }

    @Composable
    fun MedicineDialog(
        medicine: Medicine?,
        onDismiss: () -> Unit,
        onSave: (Medicine) -> Unit
    ) {
        var name by remember { mutableStateOf(medicine?.name ?: "") }
        var dosage by remember { mutableStateOf(medicine?.dosage ?: "") }
        var quantity by remember { mutableStateOf(medicine?.quantity?.toString() ?: "") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(if (medicine == null) "Добавить лекарство" else "Редактировать")
            },
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
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onSave(
                            Medicine(
                                id = medicine?.id ?: UUID.randomUUID().toString(),
                                name = name,
                                dosage = dosage,
                                quantity = quantity.toIntOrNull() ?: 0
                            )
                        )
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        )
    }
}
