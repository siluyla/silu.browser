package id.silu.browser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object Favicons {
    private const val MIN_SIZE = 16
    private const val TIMEOUT_MS = 4000

    private val cache = LruCache<String, Bitmap>(64)
    private val misses = HashSet<String>()
    private val pool = Executors.newFixedThreadPool(2) { Thread(it, "silu-favicon").apply { isDaemon = true } }
    private val main = Handler(Looper.getMainLooper())

    fun load(url: String, onReady: (Bitmap) -> Unit) {
        val origin = originOf(url) ?: return
        cache.get(origin)?.let {
            onReady(it)
            return
        }
        if (synchronized(misses) { origin in misses }) return
        pool.execute {
            val bitmap = fetch(origin)
            if (bitmap == null) {
                synchronized(misses) { misses.add(origin) }
            } else {
                cache.put(origin, bitmap)
                main.post { onReady(bitmap) }
            }
        }
    }

    private fun originOf(url: String): String? {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val scheme = uri.scheme ?: return null
        val authority = uri.authority ?: return null
        return if (scheme == "http" || scheme == "https") "$scheme://$authority" else null
    }

    private fun fetch(origin: String): Bitmap? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL("$origin/favicon.ico").openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.use { BitmapFactory.decodeStream(it) }?.takeIf { it.width > MIN_SIZE }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
