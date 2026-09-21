package com.linnan.girlvideos

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.View

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun Context.dpF(value: Float): Float = value * resources.displayMetrics.density

fun roundedBg(color: Int, radiusDp: Float, context: Context, strokeColor: Int? = null, strokeDp: Int = 1): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = radiusDp * context.resources.displayMetrics.density
        if (strokeColor != null) setStroke(context.dp(strokeDp), strokeColor)
    }
}

/** Rounded background with a ripple overlay for pressable surfaces. */
fun rippleBg(color: Int, radiusDp: Float, context: Context, strokeColor: Int? = null): RippleDrawable {
    val base = roundedBg(color, radiusDp, context, strokeColor)
    val mask = roundedBg(Color.WHITE, radiusDp, context)
    return RippleDrawable(android.content.res.ColorStateList.valueOf(Color.argb(70, 255, 255, 255)), base, mask)
}

fun View.applyRippleForeground(context: Context) {
    val outValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
    foreground = context.getDrawable(outValue.resourceId)
}

fun View.setPaddingDp(l: Int, t: Int, r: Int, b: Int) {
    setPadding(context.dp(l), context.dp(t), context.dp(r), context.dp(b))
}

object Palette {
    val Bg = Color.rgb(10, 10, 12)
    val Surface = Color.rgb(21, 21, 25)
    val Surface2 = Color.rgb(32, 32, 38)
    val SurfaceElevated = Color.rgb(26, 26, 31)
    val Text = Color.rgb(247, 247, 250)
    val Muted = Color.rgb(166, 166, 176)
    val Accent = Color.rgb(243, 182, 201)
    val Accent2 = Color.rgb(255, 220, 231)
    val AccentText = Color.rgb(58, 26, 37)
    val Stroke = Color.rgb(48, 48, 56)
    val Danger = Color.rgb(240, 108, 128)
    val DangerText = Color.rgb(255, 255, 255)
}
