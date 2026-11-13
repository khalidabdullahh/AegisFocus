package com.aegisfocus.app.data.local

import android.content.Context
import android.os.SystemClock
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * App-lock PIN (4 digits). Only a salted SHA-256 hash is stored, never the PIN itself.
 * After a correct PIN the lock stays open for a few minutes.
 */
class PinStore(context: Context) {

    private val prefs = context.getSharedPreferences("aegis_pin", Context.MODE_PRIVATE)

    fun hasPin(): Boolean = prefs.contains(KEY_HASH)

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        prefs.edit().putString(KEY_SALT, salt).putString(KEY_HASH, hash(salt, pin)).apply()
        markUnlocked()
    }

    fun clearPin() {
        prefs.edit().remove(KEY_SALT).remove(KEY_HASH).apply()
        unlockedUntil = 0L
    }

    /** Returns true for a correct PIN. Wrong tries are counted and lock the keypad for a while. */
    fun verify(pin: String): Boolean {
        if (lockoutSecondsLeft() > 0) return false
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val ok = hash(salt, pin) == prefs.getString(KEY_HASH, null)
        if (ok) {
            failedAttempts = 0
            markUnlocked()
        } else {
            failedAttempts++
            if (failedAttempts >= MAX_ATTEMPTS) {
                lockedUntil = SystemClock.elapsedRealtime() + LOCKOUT_MS
                failedAttempts = 0
            }
        }
        return ok
    }

    fun lockoutSecondsLeft(): Int {
        val left = lockedUntil - SystemClock.elapsedRealtime()
        return if (left > 0) ((left + 999) / 1000).toInt() else 0
    }

    fun isUnlocked(): Boolean = SystemClock.elapsedRealtime() < unlockedUntil

    fun markUnlocked() {
        unlockedUntil = SystemClock.elapsedRealtime() + UNLOCK_WINDOW_MS
    }

    private fun hash(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val KEY_SALT = "salt"
        private const val KEY_HASH = "hash"
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_MS = 30_000L
        private const val UNLOCK_WINDOW_MS = 5 * 60_000L

        @Volatile private var unlockedUntil = 0L
        @Volatile private var failedAttempts = 0
        @Volatile private var lockedUntil = 0L
    }
}
