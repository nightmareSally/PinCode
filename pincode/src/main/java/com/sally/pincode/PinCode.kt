package com.sally.pincode

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.text.InputFilter
import android.util.AttributeSet
import android.util.TypedValue
import android.view.animation.CycleInterpolator
import android.view.animation.TranslateAnimation
import androidx.appcompat.widget.AppCompatEditText
import com.sally.pincode.Extensions.dpToPx

class PinCode : AppCompatEditText {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initAttrs(context, attrs)
    }

    val textNumber: Int
        get() = _textNumber
    private var _textNumber: Int = 6
    private var intervalSpace: Float = -1f
    private var textBgVerticalPadding: Float = 0f
    private var textBgColor: Int = Color.GRAY
    private var textBgDrawable: Drawable? = null

    private var focusTextBgColor: Int? = null
    private var focusTextBgDrawable: Drawable? = null

    private var errorTextBgColor: Int? = null
    private var errorTextBgDrawable: Drawable? = null

    private var backgroundBounds: RectF = RectF()

    private var textBgWidth: Float = 0f

    private var textBgPaint = Paint()
    private var textPaint = Paint()
    private var cursorCurrentVisible: Boolean = false
    private var lastCursorChangeTime: Long = 0L

    var isError: Boolean = false
        set(value) {
            field = value
            invalidate()
            if (value) {
                val shakeAnimation = TranslateAnimation(0f, 10f, 0f, 0f)
                shakeAnimation.duration = 500
                shakeAnimation.interpolator = CycleInterpolator(7f)
                this.startAnimation(shakeAnimation)
            }
        }

    private val cursorTimeout = 500L

    private fun initAttrs(context: Context, attrs: AttributeSet) {
        val attributeSet = context.obtainStyledAttributes(attrs, R.styleable.PinCode)

        when (attributeSet.getType(R.styleable.PinCode_text_background)) {
            TypedValue.TYPE_STRING -> {
                textBgDrawable = attributeSet.getDrawable(R.styleable.PinCode_text_background)
            }
            else -> {
                textBgColor =
                    attributeSet.getColor(R.styleable.PinCode_text_background, Color.GRAY)
            }
        }

        val textBgFocusType = attributeSet.getType(R.styleable.PinCode_text_background_focus)
        when {
            textBgFocusType == TypedValue.TYPE_STRING -> {
                focusTextBgDrawable =
                    attributeSet.getDrawable(R.styleable.PinCode_text_background_focus)
            }
            textBgFocusType >= TypedValue.TYPE_FIRST_COLOR_INT && textBgFocusType <= TypedValue.TYPE_LAST_COLOR_INT -> {
                focusTextBgColor =
                    attributeSet.getColor(R.styleable.PinCode_text_background_focus, Color.GRAY)
            }
        }

        val textBgErrorType = attributeSet.getType(R.styleable.PinCode_text_background_error)
        when {
            textBgErrorType == TypedValue.TYPE_STRING -> {
                errorTextBgDrawable =
                    attributeSet.getDrawable(R.styleable.PinCode_text_background_error)
            }
            textBgErrorType >= TypedValue.TYPE_FIRST_COLOR_INT && textBgFocusType <= TypedValue.TYPE_LAST_COLOR_INT -> {
                errorTextBgColor =
                    attributeSet.getColor(R.styleable.PinCode_text_background_error, Color.GRAY)
            }
        }

        intervalSpace = attributeSet.getDimension(R.styleable.PinCode_interval_spacing, -1f)
        textBgVerticalPadding = attributeSet.getDimension(
            R.styleable.PinCode_text_padding_vertical,
            30f.dpToPx()
        )

        _textNumber = attributeSet.getInt(R.styleable.PinCode_text_number, 6)
        attributeSet.recycle()
        setBackgroundColor(Color.TRANSPARENT)
        filters = arrayOf(InputFilter.LengthFilter(_textNumber))
    }

    private fun drawBackground(canvas: Canvas) {
        backgroundBounds = RectF().apply {
            left = 0f
            top = 0f
            right = measuredWidth.toFloat()
            bottom = measuredHeight.toFloat()
        }

        if (intervalSpace < 0) {
            textBgWidth = measuredWidth.toFloat() / (_textNumber * 2 - 1)
            intervalSpace = textBgWidth
        } else {
            textBgWidth =
                (measuredWidth.toFloat() - intervalSpace * (_textNumber - 1)) / _textNumber
        }

        var bgDrawable = textBgDrawable
        var bgColor = textBgColor

        if (isError) {
            bgDrawable = errorTextBgDrawable
            bgColor = errorTextBgColor ?: textBgColor
        }

        for (i in 0 until _textNumber) {
            val left = i * (intervalSpace + textBgWidth)
            val right = left + textBgWidth
            if (!isError && isFocused && (text?.length ?: 0) == i) {
                focusTextBgDrawable?.let { drawable ->
                    drawable.setBounds(left.toInt(), 0, right.toInt(), measuredHeight)
                    drawable.draw(canvas)
                } ?: run {
                    textBgPaint.color = focusTextBgColor ?: textBgColor
                    val bounds = RectF().apply {
                        this.left = left
                        this.right = right
                        this.top = 0f
                        this.bottom = measuredHeight.toFloat()
                    }
                    canvas.drawRect(bounds, textBgPaint)
                }
            } else {
                bgDrawable?.let { drawable ->
                    drawable.setBounds(left.toInt(), 0, right.toInt(), measuredHeight)
                    drawable.draw(canvas)
                } ?: run {
                    textBgPaint.color = bgColor
                    val bounds = RectF().apply {
                        this.left = left
                        this.right = right
                        this.top = 0f
                        this.bottom = measuredHeight.toFloat()
                    }
                    canvas.drawRect(bounds, textBgPaint)
                }
            }
        }
    }

    private fun drawText(canvas: Canvas) {
        textPaint.color = Color.BLACK
        textPaint.textSize = textSize
        textPaint.typeface = typeface
        text?.toString()?.forEachIndexed { index, char ->
            val textWidth = textPaint.measureText(char.toString())
            val textY = canvas.height / 2 - (textPaint.descent() / 2 + textPaint.ascent() / 2)
            val textX = index * (intervalSpace + textBgWidth) + (textBgWidth / 2 - textWidth / 2)
            canvas.drawText(char.toString(), textX, textY, textPaint)
        }
    }

    private fun drawCursor(canvas: Canvas) {
        if (System.currentTimeMillis() - lastCursorChangeTime > 500) {
            cursorCurrentVisible = !cursorCurrentVisible
            lastCursorChangeTime = System.currentTimeMillis()
        }

        if (cursorCurrentVisible) {
            val cursorPaint = Paint().apply {
                color = highlightColor
            }
            val x = (((text?.length ?: 0) * (textBgWidth + intervalSpace))
                    + 0.5 * textBgWidth).toFloat()
            val top = textBgVerticalPadding / 2
            val bottom = measuredHeight - top
            canvas.drawLine(x, top, x, bottom, cursorPaint)
        }

        postInvalidateDelayed(cursorTimeout)
    }

    override fun onDraw(canvas: Canvas) {
        drawBackground(canvas)
        drawText(canvas)
        if (isFocused) {
            drawCursor(canvas)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight + textBgVerticalPadding.toInt())
    }
}