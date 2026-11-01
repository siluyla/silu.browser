package id.silu.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class GradientWallpaper(
    private val colors: IntArray,
    private val angleDegrees: Float = 160f,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stops = floatArrayOf(0f, .55f, 1f)

    override fun onBoundsChange(bounds: Rect) {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val dx = sin(radians).toFloat()
        val dy = -cos(radians).toFloat()
        val half = (abs(bounds.width() * dx) + abs(bounds.height() * dy)) / 2f
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        paint.shader = LinearGradient(cx - dx * half, cy - dy * half, cx + dx * half, cy + dy * half, colors, stops, Shader.TileMode.CLAMP)
    }

    override fun draw(canvas: Canvas) = canvas.drawRect(bounds, paint)

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Suppress("DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

object Wallpapers {
    const val NONE = "none"
    const val IMAGE = "image"

    private val presets = mapOf(
        "teal" to intArrayOf(0xFF06171A.toInt(), 0xFF0F3B40.toInt(), 0xFF1C6A6A.toInt()),
        "graphite" to intArrayOf(0xFF0D0E10.toInt(), 0xFF23252B.toInt(), 0xFF3A3D46.toInt()),
        "dusk" to intArrayOf(0xFF14101F.toInt(), 0xFF2C2347.toInt(), 0xFF51407A.toInt()),
        "ocean" to intArrayOf(0xFF04121F.toInt(), 0xFF0A3558.toInt(), 0xFF1A6A96.toInt()),
        "forest" to intArrayOf(0xFF07140D.toInt(), 0xFF14402A.toInt(), 0xFF2F7A52.toInt()),
    )

    private val worker = Executors.newSingleThreadExecutor { Thread(it, "silu-wallpaper").apply { isDaemon = true } }
    private val main = Handler(Looper.getMainLooper())
    private var cachedVersion = -1
    private var cachedBitmap: Bitmap? = null

    fun preset(id: String): Drawable? = presets[id]?.let { GradientWallpaper(it) }

    fun file(ctx: Context): File = File(File(ctx.filesDir, "wallpaper"), "wp.jpg")

    fun loadImage(ctx: Context, version: Int, onReady: (Bitmap?) -> Unit) {
        synchronized(this) {
            val hit = cachedBitmap
            if (hit != null && cachedVersion == version) {
                onReady(hit)
                return
            }
        }
        val file = file(ctx)
        val maxSide = ctx.resources.displayMetrics.heightPixels
        worker.execute {
            val bitmap = decode(file, maxSide)
            synchronized(this) {
                cachedBitmap = bitmap
                cachedVersion = version
            }
            main.post { onReady(bitmap) }
        }
    }

    private fun decode(file: File, maxSide: Int): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
