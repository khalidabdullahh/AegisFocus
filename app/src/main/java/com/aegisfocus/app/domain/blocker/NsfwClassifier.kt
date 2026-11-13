package com.aegisfocus.app.domain.blocker

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * On-device NSFW image classifier (Yahoo open_nsfw, TensorFlow Lite build, BSD-3-Clause).
 * Input: 224x224 BGR float image with the VGG mean subtracted. Output: [sfw, nsfw] scores.
 * Nothing leaves the device.
 */
class NsfwClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val input: ByteBuffer =
        ByteBuffer.allocateDirect(SIZE * SIZE * 3 * 4).order(ByteOrder.nativeOrder())
    private val pixels = IntArray(SIZE * SIZE)
    private val output = Array(1) { FloatArray(2) }

    @Synchronized
    private fun ensureInterpreter(): Interpreter? {
        interpreter?.let { return it }
        return try {
            val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
            val model = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
            model.put(bytes)
            model.rewind()
            Interpreter(model, Interpreter.Options().setNumThreads(2)).also { interpreter = it }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Highest NSFW score (0..1) over the whole screen plus its top and bottom halves,
     * so a single explicit image inside a long feed is not diluted by the rest of the screen.
     */
    @Synchronized
    fun nsfwScore(bitmap: Bitmap): Float {
        val model = ensureInterpreter() ?: return 0f
        if (bitmap.width < 64 || bitmap.height < 64) return 0f

        val w = bitmap.width
        val h = bitmap.height
        val half = h / 2
        var best = 0f
        val regions = listOf(
            intArrayOf(0, 0, w, h),
            intArrayOf(0, 0, w, half),
            intArrayOf(0, h - half, w, half)
        )
        for (r in regions) {
            val score = scoreRegion(model, bitmap, r[0], r[1], r[2], r[3])
            if (score > best) best = score
        }
        return best
    }

    private fun scoreRegion(model: Interpreter, src: Bitmap, x: Int, y: Int, w: Int, h: Int): Float {
        val region = Bitmap.createBitmap(src, x, y, w, h)
        val scaled = Bitmap.createScaledBitmap(region, SCALE_TO, SCALE_TO, true)
        val offset = (SCALE_TO - SIZE) / 2
        scaled.getPixels(pixels, 0, SIZE, offset, offset, SIZE, SIZE)

        input.rewind()
        for (color in pixels) {
            input.putFloat((color and 0xFF) - MEAN_B)            // blue
            input.putFloat(((color shr 8) and 0xFF) - MEAN_G)    // green
            input.putFloat(((color shr 16) and 0xFF) - MEAN_R)   // red
        }
        input.rewind()

        model.run(input, output)

        if (scaled !== region) scaled.recycle()
        if (region !== src) region.recycle()
        return output[0][1]
    }

    @Synchronized
    fun close() {
        interpreter?.close()
        interpreter = null
    }

    companion object {
        private const val MODEL_ASSET = "nsfw.tflite"
        private const val SIZE = 224
        private const val SCALE_TO = 256
        private const val MEAN_B = 103.939f
        private const val MEAN_G = 116.779f
        private const val MEAN_R = 123.68f
    }
}
