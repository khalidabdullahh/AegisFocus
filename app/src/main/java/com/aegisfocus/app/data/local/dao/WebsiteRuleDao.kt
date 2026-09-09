package com.aegisfocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aegisfocus.app.data.local.entity.WebsiteRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WebsiteRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: WebsiteRuleEntity)

    @Delete
    suspend fun deleteRule(rule: WebsiteRuleEntity)

    @Query("SELECT * FROM website_rules WHERE isEnabled = 1 ORDER BY domain ASC")
    fun observeActiveRules(): Flow<List<WebsiteRuleEntity>>

    @Query("SELECT * FROM website_rules WHERE isEnabled = 1 AND (profileId IS NULL OR profileId = :profileId)")
    suspend fun getRulesForProfile(profileId: Long): List<WebsiteRuleEntity>

    @Query("DELETE FROM website_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("UPDATE website_rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setRuleEnabled(id: Long, enabled: Boolean)
}
