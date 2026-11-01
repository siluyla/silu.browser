package id.silu.browser

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean

object Blocker {
    @Volatile
    private var ads: Set<String> = emptySet()

    @Volatile
    private var trackers: Set<String> = emptySet()

    private val started = AtomicBoolean(false)

    fun load(ctx: Context) {
        if (!started.compareAndSet(false, true)) return
        val app = ctx.applicationContext
        Thread({
            ads = read(app, "ads.txt")
            trackers = read(app, "trackers.txt")
        }, "silu-blocklists").start()
    }

    fun check(host: String, blockAds: Boolean, blockTrackers: Boolean): Boolean {
        val adList = ads
        val trackerList = trackers
        return suffixes(host).any { (blockAds && it in adList) || (blockTrackers && it in trackerList) }
    }

    private fun suffixes(host: String): Sequence<String> =
        generateSequence(host.lowercase()) { h -> h.substringAfter('.', "").takeIf { it.contains('.') } }

    private fun read(ctx: Context, name: String): Set<String> = try {
        ctx.assets.open(name).bufferedReader().useLines { lines ->
            lines.map { it.trim().lowercase() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toHashSet()
        }
    } catch (e: Exception) {
        emptySet()
    }
}
