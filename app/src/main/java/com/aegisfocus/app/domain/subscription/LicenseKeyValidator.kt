package com.aegisfocus.app.domain.subscription

import java.security.MessageDigest

/**
 * Validates offline cryptographic license keys for direct web payments / promo access.
 *
 * Key Formats:
 *  - ASR-LIFE-XXXX-XXXX (Lifetime Pro)
 *  - ASR-YEAR-XXXX-XXXX (1-Year Pro)
 *  - ASR-MNTH-XXXX-XXXX (1-Month Pro)
 *  - ASR-STUD-XXXX-XXXX (Student Pro)
 *  - ASR-VIP-XXXX-XXXX  (VIP / Tester Full Access)
 */
object LicenseKeyValidator {

    private const val SECRET_SALT = "ASR_DISCIPLINE_PROGRESS_2026_SECRET_KEY"

    data class ValidationResult(
        val isValid: Boolean,
        val plan: SubscriptionPlan = SubscriptionPlan.FREE,
        val durationDays: Int = 0,
        val message: String = ""
    )

    fun validateKey(rawKey: String): ValidationResult {
        val cleanKey = rawKey.trim().uppercase()
        if (cleanKey.isBlank()) {
            return ValidationResult(isValid = false, message = "Please enter a valid license key.")
        }

        val parts = cleanKey.split("-")
        if (parts.size != 4 || (parts[0] != "ASAR" && parts[0] != "ASR")) {
            return ValidationResult(isValid = false, message = "Invalid key format. Expected: ASAR-XXXX-XXXX-XXXX")
        }

        val tier = parts[1]
        val payload = parts[2]
        val signature = parts[3]

        // Calculate expected checksum
        val expectedSig = computeChecksum("$tier-$payload-$SECRET_SALT")
        if (!signature.equals(expectedSig, ignoreCase = true)) {
            // Also allow master VIP bypass keys for early beta testing
            if (payload == "BETA" && signature == "2026") {
                return ValidationResult(
                    isValid = true,
                    plan = SubscriptionPlan.LIFETIME,
                    durationDays = 0,
                    message = "Beta Lifetime Pro Activated!"
                )
            }
            return ValidationResult(isValid = false, message = "Invalid license key or signature mismatch.")
        }

        return when (tier) {
            "LIFE" -> ValidationResult(isValid = true, plan = SubscriptionPlan.LIFETIME, durationDays = 0, message = "Lifetime Pro Activated!")
            "YEAR" -> ValidationResult(isValid = true, plan = SubscriptionPlan.PRO_YEARLY, durationDays = 365, message = "1-Year Pro Activated!")
            "MNTH" -> ValidationResult(isValid = true, plan = SubscriptionPlan.PRO_MONTHLY, durationDays = 30, message = "1-Month Pro Activated!")
            "STUD" -> ValidationResult(isValid = true, plan = SubscriptionPlan.STUDENT_YEARLY, durationDays = 365, message = "Student Pro Activated!")
            "VIP"  -> ValidationResult(isValid = true, plan = SubscriptionPlan.LIFETIME, durationDays = 0, message = "VIP Lifetime Pro Activated!")
            else   -> ValidationResult(isValid = false, message = "Unknown license tier.")
        }
    }

    fun generateKey(tier: String = "LIFE"): String {
        val payload = "%04X".format((1000..9999).random())
        val sig = computeChecksum("$tier-$payload-$SECRET_SALT")
        return "ASAR-$tier-$payload-$sig"
    }

    private fun computeChecksum(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.take(2).joinToString("") { "%02X".format(it) } // 4-char hex
    }
}
