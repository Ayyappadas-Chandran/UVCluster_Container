package com.suprajit.uvcluster

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.R
import com.google.android.material.card.MaterialCardView
import java.util.ArrayDeque

@SuppressLint("StaticFieldLeak")
object ClusterNotification {

    private const val TAG = "ClusterNotification"

    enum class Priority { PENDING, IMMEDIATE }
    enum class Result { SHOWN, UPDATED, QUEUED, SUPPRESSED, IGNORED, REPLACED }
    private enum class State { IDLE, SHOWING, DISMISSING }

    data class Params(
        val heading: String,
        val subtext: String,
        val dismissTimeMs: Long = 5000,
        val priority: Priority = Priority.PENDING,
        val windowType: Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        val suppressAboveSpeed: Int = 60
    )

    private var windowManager: WindowManager? = null
    private var rootView: View? = null
    private var appContext: Context? = null
    private val handler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    private var state = State.IDLE
    private var currentSpeed = 0
    private val queue: ArrayDeque<Params> = ArrayDeque()

    fun setCurrentSpeed(kmph: Int) {
        currentSpeed = kmph
    }

    fun show(context: Context?, params: Params): Result {
        if (appContext == null) appContext = context?.applicationContext
        if (!Settings.canDrawOverlays(appContext!!)) return Result.IGNORED

        if (currentSpeed > params.suppressAboveSpeed && params.priority != Priority.IMMEDIATE) {
            queue.add(params)
            return Result.SUPPRESSED
        }

        return when (state) {
            State.DISMISSING -> {
                if (params.priority == Priority.IMMEDIATE) queue.addFirst(params) else queue.add(params)
                Result.QUEUED
            }
            State.SHOWING -> {
                if (params.priority == Priority.IMMEDIATE) {
                    queue.addFirst(params)
                    dismiss()
                } else queue.add(params)
                Result.QUEUED
            }
            State.IDLE -> {
                showInternal(appContext!!, params)
                Result.SHOWN
            }
        }
    }

    fun dismiss(): Result {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        dismissRunnable = null
        return if (state == State.SHOWING && rootView != null) {
            animateOut { cleanup() }
            Result.REPLACED
        } else Result.IGNORED
    }

    private fun showInternal(context: Context, params: Params) {
        state = State.SHOWING
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val card = buildLayout(context)
        rootView = card

        card.findViewWithTag<TextView>("heading").text = params.heading
        card.findViewWithTag<TextView>("subtext").text = params.subtext

        val bg = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.parseColor("#4B0000"),
                Color.parseColor("#120000"),
                Color.parseColor("#000000")
            )
        )
        bg.cornerRadius = dpToPx(context, 12f).toFloat()
        card.background = bg

        //card.findViewWithTag<View>("remove_btn").setOnClickListener { dismiss() }
        card.findViewWithTag<View>("dismiss_btn").setOnClickListener { dismiss() }

        // Increased height/width slightly to prevent clipping on different screen densities
        val lp = WindowManager.LayoutParams(
            dpToPx(context, 520f),
            dpToPx(context, 120f),
            params.windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dpToPx(context, 70f)
        }

        windowManager?.addView(card, lp)
        animateIn()
        scheduleDismiss(params.dismissTimeMs)
    }

    private fun buildLayout(ctx: Context): MaterialCardView {
        val themedCtx = ContextThemeWrapper(ctx, R.style.Theme_Material3_DayNight_NoActionBar)

        val card = MaterialCardView(themedCtx).apply {
            radius = dpToPx(ctx, 10f).toFloat()
            strokeWidth = dpToPx(ctx, 1f)
            strokeColor = Color.parseColor("#33FF0000")
            cardElevation = 0f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            // Increased horizontal padding for better spacing
            setPadding(dpToPx(ctx, 28f), dpToPx(ctx, 20f), dpToPx(ctx, 28f), dpToPx(ctx, 20f))
            weightSum = 1f
        }

        // --- Left Text Section ---
        val textLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            // weight 1f ensures this takes up all available space, pushing buttons to the right
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        textLayout.addView(TextView(ctx).apply {
            tag = "heading"
            textSize = 24f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.create("sans-serif", Typeface.BOLD))
        })

        textLayout.addView(TextView(ctx).apply {
            tag = "subtext"
            textSize = 18f
            setTextColor(Color.parseColor("#D1D1D1"))
            setPadding(0, dpToPx(ctx, 4f), 0, 0)
        })

        // --- Right Action Section ---
        val actionsLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            // Explicitly wrap content so it doesn't get squeezed
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        fun createActionView(tag: String, label: String, iconRes: Int): LinearLayout {
            return LinearLayout(ctx).apply {
                this.tag = tag
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(ctx, 8f), 0, dpToPx(ctx, 8f), 0)   // ← was 14 → 10
                isClickable = true
                isFocusable = true

                addView(ImageView(ctx).apply {
                    setImageResource(iconRes)
                    layoutParams = LinearLayout.LayoutParams(dpToPx(ctx, 22f), dpToPx(ctx, 22f))  // ← was 26
                    setColorFilter(Color.WHITE)
                })

                addView(TextView(ctx).apply {
                    text = label
                    textSize = 12.5f          // ← was 14
                    setTextColor(Color.WHITE)
                    setPadding(0, dpToPx(ctx, 3f), 0, 0)   // reduced from 6
                    includeFontPadding = false
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
            }
        }

        //actionsLayout.addView(createActionView("remove_btn", "Remove", android.R.drawable.presence_invisible))
        actionsLayout.addView(createActionView("dismiss_btn", "Dismiss", android.R.drawable.ic_menu_close_clear_cancel))

        root.addView(textLayout)
        root.addView(actionsLayout)
        card.addView(root)
        return card
    }

    private fun cleanup() {
        try { rootView?.let { windowManager?.removeViewImmediate(it) } } catch (_: Exception) {}
        rootView = null
        windowManager = null
        state = State.IDLE
        if (queue.isNotEmpty() && appContext != null) showInternal(appContext!!, queue.removeFirst())
    }

    private fun dpToPx(context: Context, dp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()

    private fun animateIn() {
        rootView?.apply {
            alpha = 0f; translationY = -40f
            animate().alpha(1f).translationY(0f).setDuration(250).start()
        }
    }

    private fun animateOut(onEnd: () -> Unit) {
        state = State.DISMISSING
        rootView?.animate()?.alpha(0f)?.translationY(-40f)?.setDuration(200)
            ?.withEndAction { onEnd() }?.start()
    }

    private fun scheduleDismiss(delay: Long) {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        dismissRunnable = Runnable { dismiss() }
        handler.postDelayed(dismissRunnable!!, delay)
    }
}
