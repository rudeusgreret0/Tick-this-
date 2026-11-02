package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_records")
data class MatchRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val gameMode: String,          // "PVP", "PVC", "GEMINI"
    val difficulty: String,        // "EASY", "MEDIUM", "HARD"
    val ruleMode: String,          // "CLASSIC", "INFINITY_THREE"
    val playerXName: String,
    val playerOName: String,
    val humanMark: String,         // "X" or "O"
    val winner: String,            // "X", "O", or "DRAW"
    val moveCount: Int,
    val durationSeconds: Int,
    val moveSequenceNotation: String, // e.g. "X:B2;O:A1;X:C3"
    val winningCellsCsv: String,      // e.g. "0,4,8" or ""
    val usedLocalFallback: Boolean = false
)
