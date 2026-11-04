package com.example.personalfinanceapp.feature.category

import com.example.personalfinanceapp.core.model.Category
import com.example.personalfinanceapp.core.model.DocumentId
import com.example.personalfinanceapp.core.model.SpendingClass
import com.example.personalfinanceapp.feature.auth.accountScopedData
import com.example.personalfinanceapp.feature.auth.observeUserIds
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreCategoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : CategoryRepository {

    private val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    private fun getCategoryCollection(uid: String) =
        firestore.collection("users").document(uid).collection("categories")

    override fun observeCategories(): Flow<List<Category>> = firebaseAuth.observeUserIds()
        .accountScopedData(emptyList(), ::observeCategoriesForOwner)

    private fun observeCategoriesForOwner(uid: String): Flow<List<Category>> = callbackFlow {
        val collection = getCategoryCollection(uid)
        seedMissingDefaultCategories(collection)

        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot == null) {
                close(IllegalStateException("Category listener returned no snapshot"))
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

    private suspend fun seedMissingDefaultCategories(
        collection: CollectionReference
    ) {
        val snapshot = collection.get().awaitValue()
        val existingIds = buildSet {
            snapshot.documents.forEach { document ->
                add(document.id)
                document.getString("id")?.let(::add)
            }
        }
        val existingNames = snapshot.documents.mapNotNullTo(mutableSetOf()) {
            it.getString("name")
        }
        val missingCategories = missingDefaultCategories(existingIds, existingNames)

        if (missingCategories.isEmpty()) return

        val batch = firestore.batch()
        missingCategories.forEach { category ->
            batch.set(
                collection.document(category.id),
                mapOf(
                    "id" to category.id,
                    "name" to category.name,
                    "defaultSpendingClass" to category.defaultSpendingClass.name,
                    "archived" to false
                )
            )
        }
        batch.commit().awaitValue()
    }

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
        val snapshot = runCatching {
            getCategoryCollection(uid).document(id).get().awaitValue()
        }.getOrElse { exception ->
            return Result.failure(exception)
        }
        val spendingClass = snapshot.getString("defaultSpendingClass")
            ?.let { value -> runCatching { SpendingClass.valueOf(value) }.getOrNull() }
            ?: SpendingClass.DISCRETIONARY

        return updateCategory(id, newName, spendingClass)
    }

    override suspend fun updateCategory(
        id: DocumentId,
        newName: String,
        spendingClass: SpendingClass
    ): Result<Unit> {
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
                    doc.id != id && doc.getString("name")?.let(::normalizeCategoryName) ==
                        normalizeCategoryName(name)
                }

                if (duplicateNameExists) {
                    continuation.resume(
                        Result.failure(IllegalArgumentException("Category name already exists"))
                    )
                    return@addOnSuccessListener
                }

                collectionRef.document(id).update(
                    mapOf(
                        "name" to name,
                        "defaultSpendingClass" to spendingClass.name
                    )
                )
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

private suspend fun <T> Task<T>.awaitValue(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { value ->
        if (continuation.isActive) continuation.resume(value)
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) continuation.resumeWithException(exception)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}
