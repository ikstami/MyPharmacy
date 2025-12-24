package com.example.mypharmacy.data.model

import com.google.firebase.Timestamp

data class Medicine(
    val id: String = "",
    val name: String = "",
    val dosage: String = "",
    val quantity: String = "",
    val category: String = "",
    val manufacturer: String = "",
    val description: String = "",
    val expirationDate: com.google.firebase.Timestamp? = null,
    val addedDate: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)