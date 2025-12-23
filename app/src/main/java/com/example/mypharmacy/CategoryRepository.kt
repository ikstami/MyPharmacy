package com.example.mypharmacy

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CategoryRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("categories")

    suspend fun getAllCategories(): List<String> {
        val snapshot = collection.get().await()
        return snapshot.documents.mapNotNull { it.getString("name") }
    }

    suspend fun addCategory(name: String) {
        collection.add(mapOf("name" to name)).await()
    }
}
