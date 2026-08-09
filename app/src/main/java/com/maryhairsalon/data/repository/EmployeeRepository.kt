package com.maryhairsalon.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.maryhairsalon.data.models.User
import kotlinx.coroutines.tasks.await

class EmployeeRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Real employee-role accounts, for the customer booking form's stylist picker. */
    suspend fun getEmployees(): List<User> =
        db.collection("users")
            .whereEqualTo("role", "employee")
            .get()
            .await()
            .toObjects(User::class.java)
            .sortedBy { it.name }
}