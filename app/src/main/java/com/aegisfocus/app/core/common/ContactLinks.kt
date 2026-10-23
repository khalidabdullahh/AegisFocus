package com.aegisfocus.app.core.common

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Where customers go to buy a license key. */
object ContactLinks {
    /** WhatsApp number in international format without the plus sign (+880 1643-526721). */
    const val WHATSAPP_NUMBER = "8801643526721"
    const val FACEBOOK_URL = "https://www.facebook.com/VoiceoffKhalid"
    const val INSTAGRAM_URL = "https://www.instagram.com/khalid.abdullahh"

    fun whatsappUrl(message: String): String =
        "https://wa.me/$WHATSAPP_NUMBER?text=${Uri.encode(message)}"

    /** Opens a link in the matching app or the browser. Returns false if nothing could open it. */
    fun open(context: Context, url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}
