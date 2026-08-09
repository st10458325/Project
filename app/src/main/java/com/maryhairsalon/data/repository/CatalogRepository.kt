package com.maryhairsalon.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.maryhairsalon.data.models.Category
import com.maryhairsalon.data.models.Service
import kotlinx.coroutines.tasks.await

class CatalogRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getCategories(): List<Category> =
        db.collection("categories").get().await().toObjects(Category::class.java)

    suspend fun getServices(): List<Service> =
        db.collection("services").get().await().toObjects(Service::class.java)
}
