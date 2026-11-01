package id.silu.browser

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class PageError(
    val kind: Kind,
    val url: String,
    val title: String,
    val hint: String,
    val detail: String,
    val httpUrl: String? = null,
) {
    enum class Kind { FAILED, INSECURE, OFFLINE }
}

class ErrorScreen(
    context: Context,
    private val onRetry: () -> Unit,
    private val onHome: () -> Unit,
    private val onOpenHttp: (String) -> Unit,
) : FrameLayout(context) {

    private var bound: PageError? = null

    fun bind(error: PageError) {
        if (error === bound) return
        bound = error
        removeAllViews()
        setBackgroundColor(context.col(R.color.chrome))

        val column = ConstrainedColumn(context, context.dp(420)).apply { gravity = Gravity.CENTER_HORIZONTAL }
        column.addView(badge(error), LinearLayout.LayoutParams(context.dp(96), context.dp(96)).apply { bottomMargin = context.dp(22) })
        column.addView(text(error.title, 26f, context.col(R.color.text), medium = true, center = true))
        column.addView(text(error.hint, 15f, context.col(R.color.sub), center = true), margin(top = 8))
        column.addView(hostChip(error), margin(top = 16))
        if (error.kind == PageError.Kind.OFFLINE) {
            listOf("Turn off airplane mode", "Check Wi-Fi or mobile data", "Move closer to your router or signal").forEach {
                column.addView(text("•  $it", 14f, context.col(R.color.sub)), margin(top = 6))
            }
        }
        column.addView(button("Try again", primary = true) { onRetry() }, buttonParams(top = 26))
        error.httpUrl?.takeIf { it.isNotEmpty() }?.let { http ->
            column.addView(button("Open HTTP version", primary = false) { onOpenHttp(http) }, buttonParams(top = 10))
        }
        column.addView(button("Go home", primary = false) { onHome() }, buttonParams(top = 10))
        if (error.kind == PageError.Kind.OFFLINE) {
            column.addView(text("This page reloads by itself when you're back online.", 12f, context.col(R.color.sub), center = true), margin(top = 18))
        } else {
            column.addView(details(error.detail), margin(top = 22, matchParent = true))
        }

        val page = FrameLayout(context).apply {
            setPadding(context.dp(22), context.dp(28), context.dp(22), context.dp(28))
            addView(column, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        }
        addView(ScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            addView(page, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        column.alpha = 0f
        column.translationY = context.dp(14).toFloat()
        column.animate().alpha(1f).translationY(0f).setDuration(500).setInterpolator(Motion.enter).start()
    }

    private fun margin(top: Int, matchParent: Boolean = false) = LinearLayout.LayoutParams(
        if (matchParent) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = context.dp(top) }

    private fun buttonParams(top: Int) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(50)).apply { topMargin = context.dp(top) }

    private fun badge(error: PageError) = FrameLayout(context).apply {
        background = context.rounded(context.col(R.color.surface), 32, context.col(R.color.divider), 1)
        val (icon, tint) = when (error.kind) {
            PageError.Kind.OFFLINE -> R.drawable.ic_cloud to context.col(R.color.accent)
            PageError.Kind.INSECURE -> R.drawable.ic_lock to WARNING
            PageError.Kind.FAILED -> R.drawable.ic_info to WARNING
        }
        addView(ImageView(context).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(tint)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, FrameLayout.LayoutParams(context.dp(46), context.dp(46), Gravity.CENTER))
    }

    private fun hostChip(error: PageError) = TextView(context).apply {
        text = hostOf(error.url)
        textSize = 13f
        maxLines = 1
        setTextColor(context.col(R.color.sub))
        setPadding(context.dp(14), context.dp(6), context.dp(14), context.dp(6))
        background = context.rounded(context.col(R.color.surface), 20, context.col(R.color.divider), 1)
    }

    private fun text(value: String, size: Float, color: Int, medium: Boolean = false, center: Boolean = false) = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (medium) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        if (center) gravity = Gravity.CENTER
        setLineSpacing(0f, 1.15f)
    }

    private fun button(label: String, primary: Boolean, onClick: () -> Unit) = TextView(context).apply {
        text = label
        textSize = 15f
        gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        val accent = context.col(R.color.accent)
        if (primary) {
            setTextColor(context.col(R.color.chrome))
            background = context.roundedRipple(accent, 25)
        } else {
            setTextColor(accent)
            background = context.roundedRipple(Color.TRANSPARENT, 25, (accent and 0x00FFFFFF) or 0x66000000, 1)
        }
        setOnClickListener { onClick() }
        pressable()
    }

    private fun details(detail: String): View {
        val code = TextView(context).apply {
            text = detail
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setTextColor(context.col(R.color.text))
            setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(12))
            background = context.rounded(context.col(R.color.surface), 14, context.col(R.color.divider), 1)
            visibility = View.GONE
        }
        val toggle = TextView(context).apply {
            text = "Technical details"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(context.col(R.color.sub))
            setPadding(0, context.dp(10), 0, context.dp(10))
            setOnClickListener { code.visibility = if (code.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(toggle, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(code, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private companion object {
        const val WARNING = 0xFFF2C14E.toInt()
    }
}
