/**
 * Category.kt
 *
 * Data models for salon service categories and individual hair styling services.
 * Maps to Firestore documents in the "categories" and "services" collections.
 */
package com.maryhairsalon.data.models

import com.google.firebase.firestore.DocumentId

/**
 * Represents a service category (e.g. Braids, Weaves, Haircuts).
 */
data class Category(
    @DocumentId val id: String = "",
    val label: String = "",
    val desc: String = "",
    val image: String = "",
    val icon: String = "grid",
    val count: Int = 0
)

/**
 * Represents a specific service within a category along with pricing and discount details.
 */
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
