package com.maryhairsalon.data.models

import com.google.firebase.firestore.DocumentId

data class Category(
    @DocumentId val id: String = "",
    val label: String = "",
    val desc: String = "",
    val image: String = "",
    val icon: String = "grid",
    val count: Int = 0
)

data class Service(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = "",
    val price: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val image: String = "",
    val discount: Double = 0.0 // manager-controlled, 0–100
)