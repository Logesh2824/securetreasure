package com.example.securetreasure.repository

import com.example.securetreasure.models.ClueData
import com.example.securetreasure.models.Hunt
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val huntsCollection = db.collection("hunts")
    private val cluesCollection = db.collection("clues")

    fun getHunts(onResult: (List<Hunt>) -> Unit) {
        huntsCollection
            .orderBy("title", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }

                val hunts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Hunt::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                onResult(hunts)
            }
    }

    fun getHunt(huntId: String, onResult: (Hunt?) -> Unit) {
        huntsCollection.document(huntId)
            .get()
            .addOnSuccessListener { doc ->
                val hunt = doc.toObject(Hunt::class.java)?.copy(id = doc.id)
                onResult(hunt)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun getClue(huntId: String, stepNumber: Int, onResult: (ClueData?) -> Unit) {
        cluesCollection
            .whereEqualTo("huntId", huntId)
            .whereEqualTo("stepNumber", stepNumber)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val clue = snapshot.documents.firstOrNull()?.toObject(ClueData::class.java)
                onResult(clue)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    suspend fun getClueSync(huntId: String, stepNumber: Int): ClueData? {
        return try {
            val snapshot = cluesCollection
                .whereEqualTo("huntId", huntId)
                .whereEqualTo("stepNumber", stepNumber)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(ClueData::class.java)
        } catch (e: Exception) {
            null
        }
    }
}