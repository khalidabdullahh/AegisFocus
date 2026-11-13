package com.aegisfocus.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.aegisfocus.app.domain.blocker.NsfwClassifier
import com.aegisfocus.app.domain.blocker.ShortsBlockerEngine
import java.util.concurrent.Executors

/**
 * Watches social and browser apps. While one is on screen it takes a screenshot every ~1.2 s
 * (Android 11+), scores it with the on-device NSFW model and covers the screen with a blurred
 * overlay when the score is high. Screenshots are never stored or sent anywhere.
 */
class NsfwBlurController(
    private val service: AccessibilityService,
    private val isEnabled: () -> Boolean
) {
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val classifier = NsfwClassifier(service.applicationContext)

    private var overlay: FrameLayout? = null
    private var overlayImage: ImageView? = null
    private var overlayShown = false
    private var looping = false
    private var scanning = false

    @Volatile
    private var activePackage: String? = null

    private val tick = object : Runnable {
        override fun run() {
            if (!looping) return
            if (!isEnabled() || !isMonitored(activePackage)) {
                stopLoop()
                return
            }
            if (!scanning) scanOnce()
            main.postDelayed(this, SCAN_INTERVAL_MS)
        }
    }

    fun onForegroundChanged(packageName: String) {
        activePackage = packageName
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        if (isEnabled() && isMonitored(packageName)) {
            startLoop()
        } else {
            stopLoop()
        }
    }

    private fun startLoop() {
        if (looping) return
        looping = true
        main.post(tick)
    }

    private fun stopLoop() {
        looping = false
        main.removeCallbacks(tick)
        hideOverlay()
    }

    fun release() {
        stopLoop()
        worker.shutdown()
        classifier.close()
    }

    private fun isMonitored(packageName: String?): Boolean {
        if (packageName == null) return false
        return packageName in SOCIAL_PACKAGES || ShortsBlockerEngine.isBrowserPackage(packageName)
    }

    // ─── Scan ────────────────────────────────────────────────────────────────

    private fun scanOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        scanning = true
        if (overlayShown) {
            // Hide our own overlay for a moment so the screenshot shows the real content
            overlay?.visibility = View.INVISIBLE
            main.postDelayed({ capture() }, 150L)
        } else {
            capture()
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    private fun capture() {
        try {
            service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                service.mainExecutor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                        val buffer = result.hardwareBuffer
                        val software = try {
                            Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                                ?.copy(Bitmap.Config.ARGB_8888, false)
                        } finally {
                            buffer.close()
                        }
                        if (software == null) {
                            finishScan(null, 0f)
                            return
                        }
                        worker.execute {
                            val score = try {
                                classifier.nsfwScore(software)
                            } catch (e: Exception) {
                                0f
                            }
                            main.post { finishScan(software, score) }
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        finishScan(null, 0f)
                    }
                }
            )
        } catch (e: Exception) {
            finishScan(null, 0f)
        }
    }

    private fun finishScan(screen: Bitmap?, score: Float) {
        scanning = false
        if (!looping) {
            screen?.recycle()
            return
        }
        if (screen != null && score >= THRESHOLD) {
            showOverlay(screen)
        } else {
            screen?.recycle()
            hideOverlay()
        }
    }

    // ─── Overlay ─────────────────────────────────────────────────────────────

    private fun showOverlay(screen: Bitmap) {
        val blurred = Bitmap.createScaledBitmap(
            screen,
            (screen.width / BLUR_DIVISOR).coerceAtLeast(1),
            (screen.height / BLUR_DIVISOR).coerceAtLeast(1),
            true
        )
        if (blurred !== screen) screen.recycle()

        val existing = overlay
        if (existing != null) {
            overlayImage?.setImageBitmap(blurred)
            existing.visibility = View.VISIBLE
            overlayShown = true
            return
        }

        val ctx: Context = service
        val image = ImageView(ctx).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            setImageBitmap(blurred)
        }
        val scrim = View(ctx).apply { setBackgroundColor(Color.argb(150, 8, 12, 20)) }
        val label = TextView(ctx).apply {
            text = "Sensitive content hidden"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
        }
        val back = Button(ctx).apply {
            text = "Go back"
            setOnClickListener {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                hideOverlay()
            }
        }
        val column = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(label)
            addView(back)
        }
        val root = FrameLayout(ctx).apply {
            addView(image, FrameLayout.LayoutParams(-1, -1))
            addView(scrim, FrameLayout.LayoutParams(-1, -1))
            addView(column, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        try {
            (service.getSystemService(Context.WINDOW_SERVICE) as WindowManager).addView(root, params)
            overlay = root
            overlayImage = image
            overlayShown = true
        } catch (e: Exception) {
            overlay = null
            overlayImage = null
            overlayShown = false
        }
    }

    private fun hideOverlay() {
        val view = overlay ?: return
        try {
            (service.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(view)
        } catch (e: Exception) {
            // already removed
        }
        overlay = null
        overlayImage = null
        overlayShown = false
    }

    companion object {
        private const val SCAN_INTERVAL_MS = 1200L
        private const val THRESHOLD = 0.85f
        private const val BLUR_DIVISOR = 30

        /** Social/video/chat apps checked in addition to browsers. */
        private val SOCIAL_PACKAGES = setOf(
            "com.facebook.katana", "com.facebook.lite", "com.facebook.orca",
            "com.instagram.android", "com.instagram.barcelona",
            "com.twitter.android", "com.reddit.frontpage",
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
            "com.snapchat.android", "com.pinterest", "com.tumblr",
            "org.telegram.messenger", "com.whatsapp", "com.discord",
            "com.google.android.youtube"
        )
    }
}
