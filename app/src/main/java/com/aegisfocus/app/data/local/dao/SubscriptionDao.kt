package com.aegisfocus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aegisfocus.app.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubscription(sub: SubscriptionEntity)

    @Query("SELECT * FROM subscription_state WHERE id = 1")
    fun observeSubscription(): Flow<SubscriptionEntity?>

    @Query("SELECT * FROM subscription_state WHERE id = 1")
    suspend fun getSubscription(): SubscriptionEntity?

    @Query("SELECT planType FROM subscription_state WHERE id = 1")
    suspend fun getPlanType(): String?
}
