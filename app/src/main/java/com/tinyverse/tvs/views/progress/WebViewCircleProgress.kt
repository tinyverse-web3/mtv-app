package com.tinyverse.tvs.views.progress

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Property
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import com.tinyverse.tvs.R
import kotlin.math.min


class WebViewCircleProgress @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), View.OnClickListener {
    private val fBounds: RectF = RectF()
    private var mObjectAnimatorSweep: ObjectAnimator? = null
    private var mObjectAnimatorAngle: ObjectAnimator? = null
    private var fractionAnimator: ValueAnimator? = null
    private var mModeAppearing = true
    private val mPaint: Paint
    private var mCurrentGlobalAngleOffset = 0f
    private var mCurrentGlobalAngle = 0f
    private var mCurrentSweepAngle = 0f
    private val mBorderWidth: Float
    private var isRunning = false
    private val mColors: IntArray
    private var mCurrentColorIndex: Int
    private var mNextColorIndex: Int
    private var mCurrentState = 0
    private val mHook: Path
    private val mHookPaint: Paint
    private var mArrow: Path? = null
    private var mRingCenterRadius = 0f
    private val mStrokeInset = 2.5f
    private var fraction = 0f
    private val mError: Path
    private val showArrow: Boolean
    private fun start() {
        if (isRunning) {
            return
        }
        isRunning = true
        mCurrentState = STATE_LOADING
        mObjectAnimatorAngle?.start()
        mObjectAnimatorSweep?.start()
        setOnClickListener(this)
        invalidate()
    }

    fun finish(type: Int) {
        stop()
        mCurrentState = type
        if (!fractionAnimator?.isRunning!!) {
            fractionAnimator?.start()
        }
    }

    private fun stop() {
        if (!isRunning) {
            return
        }
        isRunning = false
        mObjectAnimatorAngle?.cancel()
        mObjectAnimatorSweep?.cancel()
        invalidate()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            start()
        } else {
            stop()
        }
    }

    override fun onAttachedToWindow() {
        start()
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val min = min(w, h)
        fBounds.left = mBorderWidth * 2f + .5f
        fBounds.right = min - mBorderWidth * 2f - .5f
        fBounds.top = mBorderWidth * 2f + .5f
        fBounds.bottom = min - mBorderWidth * 2f - .5f
        mRingCenterRadius = min(
            fBounds.centerX() - fBounds.left,
            fBounds.centerY() - fBounds.top
        ) - mBorderWidth
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        when (mCurrentState) {
            STATE_LOADING -> drawArc(canvas)
            STATE_FINISH -> drawHook(canvas)
            STATE_ERROR -> drawError(canvas)
        }
    }

    private fun drawError(canvas: Canvas) {
        mError.reset()
        mError.moveTo(
            fBounds.centerX() + fBounds.width() * 0.2f * fraction,
            fBounds.centerY() - fBounds.height() * 0.2f * fraction
        )
        mError.lineTo(
            fBounds.centerX() - fBounds.width() * 0.2f * fraction,
            fBounds.centerY() + fBounds.height() * 0.2f * fraction
        )
        mError.moveTo(
            fBounds.centerX() - fBounds.width() * 0.2f * fraction,
            fBounds.centerY() - fBounds.height() * 0.2f * fraction
        )
        mError.lineTo(
            fBounds.centerX() + fBounds.width() * 0.2f * fraction,
            fBounds.centerY() + fBounds.height() * 0.2f * fraction
        )
        mHookPaint.color = mColors[3]
        canvas.drawPath(mError, mHookPaint)
        canvas.drawArc(fBounds, 0f, 360f, false, mHookPaint)
    }

    private fun drawHook(canvas: Canvas) {
        mHook.reset()
        mHook.moveTo(fBounds.centerX() - fBounds.width() * 0.25f * fraction, fBounds.centerY())
        mHook.lineTo(
            fBounds.centerX() - fBounds.width() * 0.1f * fraction,
            fBounds.centerY() + fBounds.height() * 0.18f * fraction
        )
        mHook.lineTo(
            fBounds.centerX() + fBounds.width() * 0.25f * fraction,
            fBounds.centerY() - fBounds.height() * 0.20f * fraction
        )
        mHookPaint.color = mColors[0]
        canvas.drawPath(mHook, mHookPaint)
        canvas.drawArc(fBounds, 0f, 360f, false, mHookPaint)
    }

    private fun drawArc(canvas: Canvas) {
        var startAngle = mCurrentGlobalAngle - mCurrentGlobalAngleOffset
        var sweepAngle = mCurrentSweepAngle
        if (mModeAppearing) {
            mPaint.color = gradient(
                mColors[mCurrentColorIndex], mColors[mNextColorIndex],
                mCurrentSweepAngle / (360 - MIN_SWEEP_ANGLE * 2)
            )
            sweepAngle += MIN_SWEEP_ANGLE.toFloat()
        } else {
            startAngle += sweepAngle
            sweepAngle = 360 - sweepAngle - MIN_SWEEP_ANGLE
        }
        canvas.drawArc(fBounds, startAngle, sweepAngle, false, mPaint)
        if (showArrow) {
            drawTriangle(canvas, startAngle, sweepAngle)
        }
    }

    private fun drawTriangle(c: Canvas, startAngle: Float, sweepAngle: Float) {
        if (mArrow == null) {
            mArrow = Path()
            mArrow!!.fillType = Path.FillType.EVEN_ODD
        } else {
            mArrow!!.reset()
        }
        val x = (mRingCenterRadius + fBounds.centerX()) as Float
        val y = fBounds.centerY() as Float
        mArrow!!.moveTo(0f, 0f)
        val mArrowScale = 1f
        mArrow!!.lineTo(ARROW_WIDTH * mArrowScale, 0f)
        mArrow!!.lineTo(
            ARROW_WIDTH * mArrowScale / 2, ARROW_HEIGHT
                    * mArrowScale
        )
        mArrow!!.offset(x, y)
        mArrow!!.close()
        c.rotate(
            startAngle + sweepAngle, fBounds.centerX(),
            fBounds.centerY()
        )
        c.drawPoint(x, y, mPaint)
        c.drawPath(mArrow!!, mPaint)
    }

    private fun toggleAppearingMode() {
        mModeAppearing = !mModeAppearing
        if (mModeAppearing) {
            mCurrentColorIndex = ++mCurrentColorIndex % 4
            mNextColorIndex = ++mNextColorIndex % 4
            mCurrentGlobalAngleOffset = (mCurrentGlobalAngleOffset + MIN_SWEEP_ANGLE * 2) % 360
        }
    }

    // ////////////////////////////////////////////////////////////////////////////
    // ////////////// Animation
    private val mAngleProperty: Property<WebViewCircleProgress, Float> =
        object : Property<WebViewCircleProgress, Float>(
            Float::class.java, "angle"
        ) {
            override fun get(`object`: WebViewCircleProgress): Float {
                return `object`.currentGlobalAngle
            }

            override fun set(`object`: WebViewCircleProgress, value: Float) {
                `object`.currentGlobalAngle = value
            }
        }
    private val mSweepProperty: Property<WebViewCircleProgress, Float> =
        object : Property<WebViewCircleProgress, Float>(
            Float::class.java, "arc"
        ) {
            override fun get(`object`: WebViewCircleProgress): Float {
                return `object`.currentSweepAngle
            }

            override fun set(`object`: WebViewCircleProgress, value: Float) {
                `object`.currentSweepAngle = value
            }
        }

    init {
        val density = context.resources.displayMetrics.density
        val a: TypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.CircleProgress,
            defStyleAttr,
            0
        )
        mBorderWidth = a.getDimension(
            R.styleable.CircleProgress_progressBorderWidth,
            DEFAULT_BORDER_WIDTH * density
        )
        showArrow = a.getBoolean(R.styleable.CircleProgress_showArrow, false)
        a.recycle()
        ARROW_WIDTH = (mBorderWidth * 2).toInt()
        ARROW_HEIGHT = mBorderWidth.toInt()
        mColors = IntArray(4)
        mColors[0] = Color.RED
        mColors[1] = Color.BLUE
        mColors[2] = Color.GREEN
        mColors[3] = Color.GRAY
        mCurrentColorIndex = 0
        mNextColorIndex = 1
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeWidth = mBorderWidth
        mPaint.color = mColors[mCurrentColorIndex]
        mHookPaint = Paint(mPaint)
        val mArrowPaint = Paint(mPaint)
        mHook = Path()
        mError = Path()
        setupAnimations()
    }

    private fun setupAnimations() {
        mObjectAnimatorAngle = ObjectAnimator.ofFloat<WebViewCircleProgress>(this, mAngleProperty, 360f)
        mObjectAnimatorAngle?.interpolator = ANGLE_INTERPOLATOR
        mObjectAnimatorAngle?.duration = ANGLE_ANIMATOR_DURATION.toLong()
        mObjectAnimatorAngle?.repeatMode = ValueAnimator.RESTART
        mObjectAnimatorAngle?.repeatCount = ValueAnimator.INFINITE
        mObjectAnimatorSweep = ObjectAnimator.ofFloat<WebViewCircleProgress>(
            this,
            mSweepProperty,
            360f - MIN_SWEEP_ANGLE * 2
        )
        mObjectAnimatorSweep?.interpolator = SWEEP_INTERPOLATOR
        mObjectAnimatorSweep?.duration = SWEEP_ANIMATOR_DURATION.toLong()
        mObjectAnimatorSweep?.repeatMode = ValueAnimator.RESTART
        mObjectAnimatorSweep?.repeatCount = ValueAnimator.INFINITE
        mObjectAnimatorSweep?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {}
            override fun onAnimationEnd(animator: Animator) {}
            override fun onAnimationCancel(animator: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {
                toggleAppearingMode()
            }
        })
        fractionAnimator = ValueAnimator.ofInt(0, 255)
        fractionAnimator?.interpolator = ANGLE_INTERPOLATOR
        fractionAnimator?.duration = 100
        fractionAnimator?.addUpdateListener{ animation ->
            fraction = animation.animatedFraction
            mHookPaint.alpha = animation.animatedValue as Int
            invalidate()
        }
    }

    var currentGlobalAngle: Float
        get() = mCurrentGlobalAngle
        set(currentGlobalAngle) {
            mCurrentGlobalAngle = currentGlobalAngle
            invalidate()
        }
    var currentSweepAngle: Float
        get() = mCurrentSweepAngle
        set(currentSweepAngle) {
            mCurrentSweepAngle = currentSweepAngle
            invalidate()
        }

    override fun onClick(v: View) {}

    companion object {
        private val ANGLE_INTERPOLATOR: Interpolator = LinearInterpolator()
        private val SWEEP_INTERPOLATOR: Interpolator = AccelerateDecelerateInterpolator()
        private const val ANGLE_ANIMATOR_DURATION = 1500 //转速
        private const val SWEEP_ANIMATOR_DURATION = 1000
        private const val MIN_SWEEP_ANGLE = 30
        private const val DEFAULT_BORDER_WIDTH = 3
        private const val STATE_LOADING = 1
        const val STATE_FINISH = 2
        const val STATE_ERROR = 3
        private var ARROW_WIDTH = 10 * 2
        private var ARROW_HEIGHT = 5 * 2
        private const val ARROW_OFFSET_ANGLE = 5f
        private fun gradient(color1: Int, color2: Int, p: Float): Int {
            val r1 = color1 and 0xff0000 shr 16
            val g1 = color1 and 0xff00 shr 8
            val b1 = color1 and 0xff
            val r2 = color2 and 0xff0000 shr 16
            val g2 = color2 and 0xff00 shr 8
            val b2 = color2 and 0xff
            val newr = (r2 * p + r1 * (1 - p)).toInt()
            val newg = (g2 * p + g1 * (1 - p)).toInt()
            val newb = (b2 * p + b1 * (1 - p)).toInt()
            return Color.argb(255, newr, newg, newb)
        }
    }
}