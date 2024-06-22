package com.dicoding.asclepius.history

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HistoryPredictionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistories(prediction: HistoryPrediction)

    @Query("SELECT * FROM history_prediction")
    suspend fun getAllHistories(): List<HistoryPrediction>

    @Delete
    suspend fun deleteHistories(prediction: HistoryPrediction)
}