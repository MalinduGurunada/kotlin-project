package com.example.personalfinanceapp.feature.category

import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.SpendingClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class FirestoreCategoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : CategoryRepository {

    private val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    private fun getCategoryCollection(uid: String) =
        firestore.collection("users").document(uid).collection("categories")

    override fun observeCategories(): Flow<List<Category>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = getCategoryCollection(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val categories = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val name = doc.getString("name") ?: return@mapNotNull null
                val spendingClassStr = doc.getString("defaultSpendingClass") ?: SpendingClass.DISCRETIONARY.name
                val spendingClass = try {
                    SpendingClass.valueOf(spendingClassStr)
                } catch (e: Exception) {
                    SpendingClass.DISCRETIONARY
                }
                val archived = doc.getBoolean("archived") ?: false

                Category(
                    id = id,
                    name = name,
                    defaultSpendingClass = spendingClass,
                    archived = archived
                )
            }

            trySend(categories)
        }

        awaitClose {
            listener.remove()
        }
    }.distinctUntilChanged()

    override suspend fun addCategory(category: Category): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(
            IllegalStateException("User is not signed in")
        )

        val name = category.name.trim()
        if (category.id.isBlank() || name.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Category ID and name cannot be blank")
            )
        }

        return suspendCancellableCoroutine { continuation ->
            val collectionRef = getCategoryCollection(uid)

            collectionRef.get().addOnSuccessListener { snapshot ->
                val existingNames = snapshot.documents.mapNotNull { it.getString("name") }
                if (existingNames.any { it.equals(name, ignoreCase = true) }) {
                    continuation.resume(
                        Result.failure(IllegalArgumentException("Category name already exists"))
                    )
                    return@addOnSuccessListener
                }

                val categoryData = hashMapOf(
                    "id" to category.id,
                    "name" to name,
                    "defaultSpendingClass" to category.defaultSpendingClass.name,
                    "archived" to category.archived
                )

                collectionRef.document(category.id).set(categoryData)
                    .addOnSuccessListener {
                        continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            }.addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
        }
    }

    override suspend fun renameCategory(id: DocumentId, newName: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(
            IllegalStateException("User is not signed in")
        )

        val name = newName.trim()
        if (name.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Category name cannot be blank")
            )
        }

        return suspendCancellableCoroutine { continuation ->
            val collectionRef = getCategoryCollection(uid)

            collectionRef.get().addOnSuccessListener { snapshot ->
                val existingDocs = snapshot.documents
                val duplicateNameExists = existingDocs.any { doc ->
                    doc.id != id && doc.getString("name")?.equals(name, ignoreCase = true) == true
                }

                if (duplicateNameExists) {
                    continuation.resume(
                        Result.failure(IllegalArgumentException("Category name already exists"))
                    )
                    return@addOnSuccessListener
                }

                collectionRef.document(id).update("name", name)
                    .addOnSuccessListener {
                        continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            }.addOnFailureListener { exception ->
                continuation.resume(Result.failure(exception))
            }
        }
    }

    override suspend fun setCategoryArchived(id: DocumentId, archived: Boolean): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(
            IllegalStateException("User is not signed in")
        )

        return suspendCancellableCoroutine { continuation ->
            getCategoryCollection(uid).document(id).update("archived", archived)
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
        }
    }
}
