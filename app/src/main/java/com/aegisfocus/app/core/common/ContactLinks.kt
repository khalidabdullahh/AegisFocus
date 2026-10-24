package com.aegisfocus.app.core.common

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Where customers go to buy a license key, download updates or visit site. */
object ContactLinks {
    /** Official website and download URLs. */
    const val WEBSITE_URL = "https://aegisfocus.pages.dev/"
    const val DOWNLOAD_PAGE_URL = "https://aegisfocus.pages.dev/download"
    const val GITHUB_RELEASES_URL = "https://github.com/khalidabdullahh/AegisFocus/releases/latest"

    /** WhatsApp number in international format without the plus sign (+880 1643-526721). */
    const val WHATSAPP_NUMBER = "8801643526721"
    const val FACEBOOK_URL = "https://www.facebook.com/VoiceoffKhalid"
    const val INSTAGRAM_URL = "https://www.instagram.com/khalid.abdullahh"

    fun whatsappUrl(message: String): String =
        "https://wa.me/$WHATSAPP_NUMBER?text=${Uri.encode(message)}"

    fun openWebsite(context: Context): Boolean = open(context, WEBSITE_URL)
    fun openDownloadPage(context: Context): Boolean = open(context, DOWNLOAD_PAGE_URL)
    fun openReleases(context: Context): Boolean = open(context, GITHUB_RELEASES_URL)

    /** Opens a link in the matching app or the browser. Returns false if nothing could open it. */
    fun open(context: Context, url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}
