/**
 * CatalogRepository.kt
 *
 * Repository for managing service categories and individual hair styling services in Firestore.
 * Supports CRUD operations and real-time reactive snapshot flows for catalog maintenance.
 */
package com.maryhairsalon.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.maryhairsalon.data.models.Category
import com.maryhairsalon.data.models.Service
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CatalogRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getCategories(): List<Category> =
        db.collection("categories").get().await().toObjects(Category::class.java)

    suspend fun getServices(): List<Service> =
        db.collection("services").get().await().toObjects(Service::class.java)

    /** Live category list, for the manager's Catalog tab — edits reflect immediately. */
    fun observeCategories(): Flow<Result<List<Category>>> = callbackFlow {
        val registration = db.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                trySend(Result.success(snapshot?.toObjects(Category::class.java)?.sortedBy { it.label } ?: emptyList()))
            }
        awaitClose { registration.remove() }
    }

    /** Live service list, for the manager's Catalog tab. */
    fun observeServices(): Flow<Result<List<Service>>> = callbackFlow {
        val registration = db.collection("services")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                trySend(Result.success(snapshot?.toObjects(Service::class.java)?.sortedBy { it.title } ?: emptyList()))
            }
        awaitClose { registration.remove() }
    }

    suspend fun addCategory(data: Map<String, Any?>): String =
        db.collection("categories").add(data).await().id

    suspend fun updateCategory(id: String, data: Map<String, Any?>) {
        db.collection("categories").document(id).update(data).await()
    }

    /** Refuses to delete a category that still has services in it, to avoid silently orphaning them. */
    suspend fun deleteCategory(id: String) {
        val stillHasServices = db.collection("services").whereEqualTo("category", id).limit(1).get().await()
        if (!stillHasServices.isEmpty) {
            throw IllegalStateException("This category still has services in it. Move or delete those first.")
        }
        db.collection("categories").document(id).delete().await()
    }

    suspend fun addService(data: Map<String, Any?>): String =
        db.collection("services").add(data).await().id

    suspend fun updateService(id: String, data: Map<String, Any?>) {
        db.collection("services").document(id).update(data).await()
    }

    suspend fun deleteService(id: String) {
        db.collection("services").document(id).delete().await()
    }
}
