package com.aegisfocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Stores domain or keyword blocking rules (Pro feature). */
@Entity(tableName = "website_rules")
data class WebsiteRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,                    // e.g. "reddit.com" or keyword "news"
    val ruleType: String = "DOMAIN",       // DOMAIN | KEYWORD
    val isEnabled: Boolean = true,
    val profileId: Long? = null,           // null = applies to all profiles
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
