package id.silu.browser

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.format.DateFormat
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class HomeScreen(
    context: Context,
    private val cfg: AppSettings,
    private val secret: Boolean,
    private val onSearch: () -> Unit,
    private val onOpen: (String) -> Unit,
) : FrameLayout(context) {

    private class Palette(
        val background: Int,
        val surface: Int,
        val line: Int,
        val text: Int,
        val sub: Int,
        val accent: Int,
    )

    private var builtFor = ""

    private val hasWallpaper get() = !secret && cfg.wallpaper != Wallpapers.NONE

    fun refresh() {
        val signature = signature()
        if (signature == builtFor) return
        builtFor = signature
        build()
    }

    private fun signature(): String = listOf(
        secret, cfg.theme, cfg.wallpaper, cfg.wallpaperVer, cfg.wallpaperDim, cfg.wallpaperOpacity,
        Store.shortcuts.joinToString("|") { it.title + "\u0000" + it.url },
    ).joinToString("#")

    private fun palette(): Palette = when {
        secret -> Palette(0xFF121019.toInt(), 0xFF1D1A29.toInt(), context.col(R.color.divider), 0xFFEDE9FF.toInt(), 0xFFB2ABD6.toInt(), 0xFFB6AEE8.toInt())
        hasWallpaper -> Palette(context.col(R.color.chrome), 0x8C121216.toInt(), 0x2EFFFFFF, Color.WHITE, 0xC7FFFFFF.toInt(), context.col(R.color.accent))
        else -> Palette(context.col(R.color.chrome), context.col(R.color.surface), context.col(R.color.divider), context.col(R.color.text), context.col(R.color.sub), context.col(R.color.accent))
    }

    private fun dp(v: Int) = context.dp(v)

    private fun build() {
        val p = palette()
        removeAllViews()
        setBackgroundColor(p.background)
        addWallpaper()

        val column = ConstrainedColumn(context, dp(560)).apply { gravity = Gravity.CENTER_HORIZONTAL }
        val rising = ArrayList<View>()
        fun add(view: View, params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)) {
            column.addView(view, params)
            rising.add(view)
        }
        fun topMargin(value: Int, width: Int = ViewGroup.LayoutParams.WRAP_CONTENT, height: Int = ViewGroup.LayoutParams.WRAP_CONTENT) =
            LinearLayout.LayoutParams(width, height).apply { topMargin = dp(value) }

        add(title(p))
        if (secret) {
            add(badge(p), topMargin(10))
            add(lead(p), topMargin(18))
        } else {
            add(clock(p), topMargin(18))
            add(date(p), topMargin(4))
        }
        add(searchPill(p), topMargin(26, ViewGroup.LayoutParams.MATCH_PARENT, dp(54)))
        if (secret) {
            add(card(p, "SiLu won't save", listOf("Your browsing history", "Cookies and site data", "Info you enter in forms")), topMargin(30, ViewGroup.LayoutParams.MATCH_PARENT))
            add(card(p, "Your activity might still be visible to", listOf("Websites you visit", "Your employer or school", "Your internet service provider")), topMargin(12, ViewGroup.LayoutParams.MATCH_PARENT))
        } else {
            tiles(p).forEachIndexed { index, row -> add(row, topMargin(if (index == 0) 38 else 22, ViewGroup.LayoutParams.MATCH_PARENT)) }
        }

        val page = FrameLayout(context).apply {
            setPadding(dp(20), dp(28), dp(20), dp(28))
            addView(column, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        }
        addView(ScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_NEVER
            addView(page, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        rising.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = dp(14).toFloat()
            view.animate().alpha(1f).translationY(0f).setStartDelay(30L * minOf(index, 12)).setDuration(520).setInterpolator(Motion.enter).start()
        }
    }

    private fun addWallpaper() {
        if (!hasWallpaper) return
        val layer: View = if (cfg.wallpaper == Wallpapers.IMAGE) {
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                Wallpapers.loadImage(context, cfg.wallpaperVer) { bitmap -> bitmap?.let { setImageBitmap(it) } }
            }
        } else {
            View(context).apply { background = Wallpapers.preset(cfg.wallpaper) }
        }
        layer.alpha = cfg.wallpaperOpacity / 100f
        addView(layer, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(View(context).apply {
            setBackgroundColor(Color.BLACK)
            alpha = cfg.wallpaperDim / 100f
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun title(p: Palette) = TextView(context).apply {
        val text = SpannableStringBuilder("SiLuyla")
        text.setSpan(TypefaceSpan("sans-serif-light"), 4, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(ForegroundColorSpan(p.sub), 4, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        this.text = text
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textSize = 56f
        letterSpacing = -0.045f
        includeFontPadding = false
        setTextColor(p.text)
    }

    private fun badge(p: Palette) = TextView(context).apply {
        text = "Secret mode"
        textSize = 13f
        setTextColor(p.sub)
        setPadding(dp(14), dp(6), dp(14), dp(6))
        background = context.rounded(p.surface, 20, p.line, 1)
    }

    private fun lead(p: Palette) = TextView(context).apply {
        text = "You've gone secret. Pages you view in secret tabs won't appear in your history."
        textSize = 15f
        gravity = Gravity.CENTER
        setLineSpacing(0f, 1.25f)
        setTextColor(p.sub)
    }

    private fun clock(p: Palette) = TextClock(context).apply {
        val locale = Locale.getDefault()
        format12Hour = DateFormat.getBestDateTimePattern(locale, "hm")
        format24Hour = DateFormat.getBestDateTimePattern(locale, "Hm")
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        textSize = 40f
        setTextColor(p.text)
    }

    private fun date(p: Palette) = TextClock(context).apply {
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEMMMMd")
        format12Hour = pattern
        format24Hour = pattern
        textSize = 14f
        setTextColor(p.sub)
    }

    private fun searchPill(p: Palette) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(20), 0, dp(20), 0)
        background = context.roundedRipple(p.surface, 27, p.line, 1)
        elevation = dp(4).toFloat()
        contentDescription = "Search or type web address"
        setOnClickListener { onSearch() }
        pressable()
        addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_search)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(20), dp(20)))
        addView(TextView(context).apply {
            text = "Search or type web address"
            textSize = 16f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(p.sub)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) })
    }

    private fun card(p: Palette, heading: String, items: List<String>) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(18), dp(20), dp(18))
        background = context.rounded(p.surface, 24, p.line, 1)
        addView(TextView(context).apply {
            text = heading
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(p.text)
        })
        items.forEach { item ->
            addView(TextView(context).apply {
                text = "•  $item"
                textSize = 14f
                setLineSpacing(0f, 1.2f)
                setTextColor(p.sub)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        }
    }

    private fun tiles(p: Palette): List<View> {
        val entries = Store.shortcuts.take(MAX_TILES)
        val cells = ArrayList<View>()
        entries.forEach { cells.add(tile(p, it)) }
        if (entries.size < MAX_TILES) cells.add(addTile(p))
        return cells.chunked(COLUMNS).map { chunk ->
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                chunk.forEach { addView(it, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)) }
                repeat(COLUMNS - chunk.size) { addView(View(context), LinearLayout.LayoutParams(0, 1, 1f)) }
            }
        }
    }

    private fun tile(p: Palette, entry: Entry): View {
        val icon = FrameLayout(context).apply {
            background = context.rounded(p.surface, 20, p.line, 1)
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }
        icon.addView(letterAvatar(entry.title), FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        Favicons.load(entry.url) { bitmap ->
            icon.removeAllViews()
            icon.addView(ImageView(context).apply {
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.FIT_CENTER
                background = context.rounded(p.surface, 8)
                outlineProvider = ViewOutlineProvider.BACKGROUND
                clipToOutline = true
            }, FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER))
        }
        return tileFrame(p, icon, entry.title, { onOpen(entry.url) }) {
            MaterialAlertDialogBuilder(context)
                .setTitle(entry.title)
                .setItems(arrayOf("Edit", "Remove")) { _, which ->
                    if (which == 0) {
                        context.shortcutDialog(entry) { name, url ->
                            Store.replaceShortcut(entry, name, url)
                            refresh()
                        }
                    } else {
                        Store.removeShortcut(entry)
                        refresh()
                    }
                }
                .show()
            true
        }
    }

    private fun addTile(p: Palette): View {
        val icon = FrameLayout(context).apply { background = context.rounded(p.surface, 20, p.line, 1) }
        icon.addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_plus)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER))
        return tileFrame(p, icon, "Add", {
            context.shortcutDialog(null) { name, url ->
                Store.addShortcut(name, url)
                refresh()
            }
        }) { false }
    }

    private fun tileFrame(p: Palette, icon: View, label: String, onClick: () -> Unit, onLongClick: () -> Boolean): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(2), dp(11), dp(2), dp(11))
            contentDescription = label
            setOnClickListener { onClick() }
            setOnLongClickListener { onLongClick() }
            pressable()
            addView(icon, LinearLayout.LayoutParams(dp(60), dp(60)))
            addView(TextView(context).apply {
                text = label
                textSize = 12f
                maxLines = 1
                gravity = Gravity.CENTER
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(p.text)
                alpha = .82f
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        }

    private fun letterAvatar(name: String) = TextView(context).apply {
        val label = name.ifEmpty { "?" }
        var hue = 0
        for (ch in label) hue = (hue * 31 + ch.code) % 360
        text = label.substring(0, 1).uppercase()
        textSize = 24f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(hsl(hue, .70f, .72f))
    }

    private companion object {
        const val COLUMNS = 4
        const val MAX_TILES = 12
    }
}
