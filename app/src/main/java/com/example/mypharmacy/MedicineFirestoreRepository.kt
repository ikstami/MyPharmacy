package com.example.mypharmacy

import com.example.mypharmacy.data.model.Medicine
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MedicineFirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    val medicines = db.collection("medicines")

    // Добавление или вставка
    suspend fun insertMedicine(medicine: Medicine): Medicine {
        val id = if (medicine.id.isEmpty()) medicines.document().id else medicine.id
        medicines.document(id).set(medicine).await()
        return medicine.copy(id = id)
    }

    // Удаление
    suspend fun deleteMedicine(medicineId: String) {
        if (medicineId.isNotEmpty()) {
            medicines.document(medicineId).delete().await()
        }
    }

    // Редактирование (update)
    suspend fun updateMedicine(medicine: Medicine) {
        if (medicine.id.isNotEmpty()) {
            medicines.document(medicine.id).set(medicine).await()
        }
    }

    // Поток всех лекарств
    fun getAllMedicinesFlow(): Flow<List<Medicine>> = callbackFlow {
        val listener = medicines.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Medicine::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }
}



