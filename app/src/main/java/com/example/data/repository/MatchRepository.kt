package com.example.data.repository

import com.example.data.local.MatchDao
import com.example.data.local.MatchRecordEntity
import kotlinx.coroutines.flow.Flow

class MatchRepository(private val matchDao: MatchDao) {

    val allMatches: Flow<List<MatchRecordEntity>> = matchDao.getAllMatches()

    suspend fun recordMatch(match: MatchRecordEntity): Long {
        return matchDao.insertMatch(match)
    }

    suspend fun deleteMatch(id: Long) {
        matchDao.deleteMatchById(id)
    }

    suspend fun clearAllMatches() {
        matchDao.clearAllMatches()
    }
}
