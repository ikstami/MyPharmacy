package com.example.mypharmacy

import android.app.Application

class MyPharmacyApp : Application() {

    val medicineRepository: MedicineRepository by lazy {
        MedicineRepository()
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepository()
    }
}
