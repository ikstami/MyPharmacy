package com.example.mypharmacy

import com.example.mypharmacy.data.model.Medicine
import kotlinx.coroutines.flow.Flow

class MedicineRepository(
    private val firestoreRepo: MedicineFirestoreRepository = MedicineFirestoreRepository()
) {
    val allMedicines: Flow<List<Medicine>> = firestoreRepo.getAllMedicinesFlow()

    suspend fun insert(medicine: Medicine) = firestoreRepo.insertMedicine(medicine)
    suspend fun delete(medicine: Medicine) = firestoreRepo.deleteMedicine(medicine.id)
    suspend fun update(medicine: Medicine) = firestoreRepo.updateMedicine(medicine)
}


