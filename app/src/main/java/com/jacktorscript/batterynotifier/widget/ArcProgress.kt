package com.jacktorscript.batterynotifier.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.jacktorscript.batterynotifier.BuildConfig
import com.jacktorscript.batterynotifier.R
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.cos
import kotlin.math.roundToInt


open class ArcProgress @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    View(context, attrs, defStyleAttr) {
    private var paint: Paint? = null
    private var textPaint: Paint? = null
    private val rectF = RectF()
    private var strokeWidth = 0f
    private var suffixTextSize = 0f
    private var bottomTextSize = 0f
    private var bottomText: String? = null
    private var text: String? = null
    private var textSize = 0f
    private var textColor = 0
    private var currentProgress = 0
    private var progress = 0f
    private var max = 0
        set(max) {
            if (max > 0) {
                field = max
                invalidate()
            }
        }
    private var finishedStrokeColor = 0
    private var unfinishedStrokeColor = 0
    private var arcAngle = 0f
    private var suffixText: String? = "%"
    private var suffixTextPadding = 0f
    private var typeFace: Typeface? = null
    //private val fontResourceId = 0
    private var arcBottomHeight = 0f
    private val defaultFinishedColor = Color.WHITE
    private val defaultUnfinishedColor = Color.rgb(72, 106, 176)
    private val defaultTextColor = Color.rgb(66, 145, 241)
    private val defaultSuffixTextSize: Float
    private val defaultSuffixPadding: Float
    private val defaultBottomTextSize: Float
    private val defaultStrokeWidth: Float
    private val defaultSuffixText: String
    private val defaultMax = 100
    private val defaultArcAngle = 360 * 0.8f
    private var defaultTextSize: Float = ProgressUtils.sp2px(resources, 18)
    private val minSize: Int = ProgressUtils.dp2px(resources, 100).toInt()

    init {
        defaultTextSize = ProgressUtils.sp2px(resources, 40)
        defaultSuffixTextSize = ProgressUtils.sp2px(resources, 15)
        defaultSuffixPadding = ProgressUtils.dp2px(resources, 4)
        defaultSuffixText = "%"
        defaultBottomTextSize = ProgressUtils.sp2px(resources, 10)
        defaultStrokeWidth = ProgressUtils.dp2px(resources, 4)
        val attributes =
            context.theme.obtainStyledAttributes(attrs, R.styleable.ArcProgress, defStyleAttr, 0)
        initByAttributes(attributes)
        attributes.recycle()
        initPainters()
    }

    private fun initByAttributes(attributes: TypedArray) {
        finishedStrokeColor =
            attributes.getColor(R.styleable.ArcProgress_arc_finished_color, defaultFinishedColor)
        unfinishedStrokeColor = attributes.getColor(
            R.styleable.ArcProgress_arc_unfinished_color,
            defaultUnfinishedColor
        )
        textColor = attributes.getColor(R.styleable.ArcProgress_arc_text_color, defaultTextColor)
        textSize = attributes.getDimension(R.styleable.ArcProgress_arc_text_size, defaultTextSize)
        arcAngle = attributes.getFloat(R.styleable.ArcProgress_arc_angle, defaultArcAngle)
        max = attributes.getInt(R.styleable.ArcProgress_arc_max, defaultMax)
        setProgress(attributes.getFloat(R.styleable.ArcProgress_arc_progress, 0f))
        strokeWidth =
            attributes.getDimension(R.styleable.ArcProgress_arc_stroke_width, defaultStrokeWidth)
        suffixTextSize = attributes.getDimension(
            R.styleable.ArcProgress_arc_suffix_text_size,
            defaultSuffixTextSize
        )
        suffixText =
            if (TextUtils.isEmpty(attributes.getString(R.styleable.ArcProgress_arc_suffix_text)))
                defaultSuffixText else attributes.getString(
                R.styleable.ArcProgress_arc_suffix_text
            )
        suffixTextPadding = attributes.getDimension(
            R.styleable.ArcProgress_arc_suffix_text_padding,
            defaultSuffixPadding
        )
        bottomTextSize = attributes.getDimension(
            R.styleable.ArcProgress_arc_bottom_text_size,
            defaultBottomTextSize
        )
        bottomText = attributes.getString(R.styleable.ArcProgress_arc_bottom_text)
        initTypeFace(attributes)
    }

    private fun initTypeFace(attributes: TypedArray) {
        if (Build.VERSION.SDK_INT < 26) {
            val fontId = attributes.getResourceId(R.styleable.ArcProgress_arc_suffix_text_font, 0)
            if (fontId != 0) {
                try {
                    typeFace = ResourcesCompat.getFont(context, fontId)
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) e.printStackTrace()
                }
            }
        } else {
            typeFace = attributes.getFont(R.styleable.ArcProgress_arc_suffix_text_font)
        }
    }

    private fun initPainters() {
        textPaint = TextPaint()
        (textPaint as TextPaint).color = textColor
        (textPaint as TextPaint).textSize = textSize
        (textPaint as TextPaint).isAntiAlias = true
        paint = Paint()
        paint!!.color = defaultUnfinishedColor
        paint!!.isAntiAlias = true
        paint!!.strokeWidth = strokeWidth
        paint!!.style = Paint.Style.STROKE
        paint!!.strokeCap = Paint.Cap.ROUND
    }

    override fun invalidate() {
        initPainters()
        super.invalidate()
    }

    open fun getStrokeWidth(): Float {
        return strokeWidth
    }

    open fun setStrokeWidth(strokeWidth: Float) {
        this.strokeWidth = strokeWidth
        this.invalidate()
    }

    open fun getSuffixTextSize(): Float {
        return suffixTextSize
    }

    open fun setSuffixTextSize(suffixTextSize: Float) {
        this.suffixTextSize = suffixTextSize
        this.invalidate()
    }

    open fun getBottomText(): String? {
        return bottomText
    }

    open fun setBottomText(bottomText: String?) {
        this.bottomText = bottomText
        this.invalidate()
    }

    open fun getProgress(): Float {
        return progress
    }

    open fun setProgress(progress: Float) {
        val dfs = DecimalFormatSymbols(Locale.US)
        val formatted = DecimalFormat("#.##", dfs).format(progress.toDouble()).toFloat()
        val normalized = formatted.roundToInt().coerceIn(0, max)
        if (this.progress == normalized.toFloat()) {
            return
        }
        this.progress = normalized.toFloat()
        if (currentProgress > max) {
            currentProgress = max
        }
        if (currentProgress < 0) {
            currentProgress = 0
        }
        invalidate()
    }

    open fun getBottomTextSize(): Float {
        return bottomTextSize
    }

    open fun setBottomTextSize(bottomTextSize: Float) {
        this.bottomTextSize = bottomTextSize
        this.invalidate()
    }

    open fun getText(): String? {
        return text
    }

    /**
     * Setting Central Text to custom String
     */
    open fun setText(text: String?) {
        this.text = text
        this.invalidate()
    }

    /**
     * Setting Central Text back to default one (value of the progress)
     */
    open fun setDefaultText() {
        text = getProgress().toString()
        invalidate()
    }

    open fun getTextSize(): Float {
        return textSize
    }

    open fun setTextSize(textSize: Float) {
        this.textSize = textSize
        this.invalidate()
    }

    open fun getTextColor(): Int {
        return textColor
    }

    open fun setTextColor(textColor: Int) {
        this.textColor = textColor
        this.invalidate()
    }

    open fun getFinishedStrokeColor(): Int {
        return finishedStrokeColor
    }

    open fun setFinishedStrokeColor(finishedStrokeColor: Int) {
        this.finishedStrokeColor = finishedStrokeColor
        this.invalidate()
    }

    open fun getUnfinishedStrokeColor(): Int {
        return unfinishedStrokeColor
    }

    open fun setUnfinishedStrokeColor(unfinishedStrokeColor: Int) {
        this.unfinishedStrokeColor = unfinishedStrokeColor
        this.invalidate()
    }

    open fun getArcAngle(): Float {
        return arcAngle
    }

    open fun setArcAngle(arcAngle: Float) {
        this.arcAngle = arcAngle
        this.invalidate()
    }

    open fun getSuffixText(): String? {
        return suffixText
    }

    open fun setSuffixText(suffixText: String?) {
        this.suffixText = suffixText
        this.invalidate()
    }

    open fun getSuffixTextPadding(): Float {
        return suffixTextPadding
    }

    open fun setSuffixTextPadding(suffixTextPadding: Float) {
        this.suffixTextPadding = suffixTextPadding
        this.invalidate()
    }

    override fun getSuggestedMinimumHeight(): Int {
        return minSize
    }

    override fun getSuggestedMinimumWidth(): Int {
        return minSize
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        rectF[strokeWidth / 2f, strokeWidth / 2f, width - strokeWidth / 2f] =
            MeasureSpec.getSize(heightMeasureSpec) - strokeWidth / 2f
        val radius = width / 2f
        val angle = (360 - arcAngle) / 2f
        arcBottomHeight = radius * (1 - cos(angle / 180 * Math.PI)).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val startAngle = 270 - arcAngle / 2f
        val targetProgress = progress.toInt().coerceIn(0, max)
        if (currentProgress > max) {
            currentProgress = max
        }
        if (currentProgress < 0) {
            currentProgress = 0
        }
        val finishedSweepAngle = currentProgress / max.toFloat() * arcAngle
        var finishedStartAngle = startAngle
        if (progress == 0f) finishedStartAngle = 0.01f
        paint!!.color = unfinishedStrokeColor
        canvas.drawArc(rectF, startAngle, arcAngle, false, paint!!)
        paint!!.color = finishedStrokeColor
        canvas.drawArc(rectF, finishedStartAngle, finishedSweepAngle, false, paint!!)
        val text = currentProgress.toString()
        if (typeFace != null) textPaint!!.typeface = typeFace
        if (!TextUtils.isEmpty(text)) {
            textPaint!!.color = textColor
            textPaint!!.textSize = textSize
            val textHeight = textPaint!!.descent() + textPaint!!.ascent()
            val textBaseline = (height - textHeight) / 2.0f
            canvas.drawText(
                text, (width - textPaint!!.measureText(text)) / 2.0f, textBaseline,
                textPaint!!
            )
            textPaint!!.textSize = suffixTextSize
            val suffixHeight = textPaint!!.descent() + textPaint!!.ascent()
            canvas.drawText(
                suffixText!!,
                width / 2.0f + textPaint!!.measureText(text) + suffixTextPadding,
                textBaseline + textHeight - suffixHeight,
                textPaint!!
            )
        }
        if (arcBottomHeight == 0f) {
            val radius = width / 2f
            val angle = (360 - arcAngle) / 2f
            arcBottomHeight = radius * (1 - cos(angle / 180 * Math.PI)).toFloat()
        }
        if (!TextUtils.isEmpty(getBottomText())) {
            textPaint!!.textSize = bottomTextSize
            val bottomTextBaseline =
                height - arcBottomHeight - (textPaint!!.descent() + textPaint!!.ascent()) / 2
            canvas.drawText(
                getBottomText()!!,
                (width - textPaint!!.measureText(getBottomText())) / 2.0f,
                bottomTextBaseline,
                textPaint!!
            )
        }
        when {
            currentProgress < targetProgress -> {
                currentProgress++
                invalidate()
            }

            currentProgress > targetProgress -> {
                currentProgress--
                invalidate()
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable(INSTANCE_STATE, super.onSaveInstanceState())
        bundle.putFloat(INSTANCE_STROKE_WIDTH, getStrokeWidth())
        bundle.putFloat(INSTANCE_SUFFIX_TEXT_SIZE, getSuffixTextSize())
        bundle.putFloat(INSTANCE_SUFFIX_TEXT_PADDING, getSuffixTextPadding())
        bundle.putFloat(INSTANCE_BOTTOM_TEXT_SIZE, getBottomTextSize())
        bundle.putString(INSTANCE_BOTTOM_TEXT, getBottomText())
        bundle.putFloat(INSTANCE_TEXT_SIZE, getTextSize())
        bundle.putInt(INSTANCE_TEXT_COLOR, getTextColor())
        bundle.putFloat(INSTANCE_PROGRESS, getProgress())
        bundle.putInt(INSTANCE_MAX, max)
        bundle.putInt(INSTANCE_FINISHED_STROKE_COLOR, getFinishedStrokeColor())
        bundle.putInt(INSTANCE_UNFINISHED_STROKE_COLOR, getUnfinishedStrokeColor())
        bundle.putFloat(INSTANCE_ARC_ANGLE, getArcAngle())
        bundle.putString(INSTANCE_SUFFIX, getSuffixText())
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            strokeWidth = state.getFloat(INSTANCE_STROKE_WIDTH)
            suffixTextSize = state.getFloat(INSTANCE_SUFFIX_TEXT_SIZE)
            suffixTextPadding = state.getFloat(INSTANCE_SUFFIX_TEXT_PADDING)
            bottomTextSize = state.getFloat(INSTANCE_BOTTOM_TEXT_SIZE)
            bottomText = state.getString(INSTANCE_BOTTOM_TEXT)
            textSize = state.getFloat(INSTANCE_TEXT_SIZE)
            textColor = state.getInt(INSTANCE_TEXT_COLOR)
            max = state.getInt(INSTANCE_MAX)
            setProgress(state.getFloat(INSTANCE_PROGRESS))
            finishedStrokeColor = state.getInt(INSTANCE_FINISHED_STROKE_COLOR)
            unfinishedStrokeColor = state.getInt(INSTANCE_UNFINISHED_STROKE_COLOR)
            suffixText = state.getString(INSTANCE_SUFFIX)
            initPainters()
            @Suppress("DEPRECATION")
            super.onRestoreInstanceState(state.getParcelable(INSTANCE_STATE))
            return
        }
        super.onRestoreInstanceState(state)
    }

    companion object {
        private const val INSTANCE_STATE = "saved_instance"
        private const val INSTANCE_STROKE_WIDTH = "stroke_width"
        private const val INSTANCE_SUFFIX_TEXT_SIZE = "suffix_text_size"
        private const val INSTANCE_SUFFIX_TEXT_PADDING = "suffix_text_padding"
        private const val INSTANCE_BOTTOM_TEXT_SIZE = "bottom_text_size"
        private const val INSTANCE_BOTTOM_TEXT = "bottom_text"
        private const val INSTANCE_TEXT_SIZE = "text_size"
        private const val INSTANCE_TEXT_COLOR = "text_color"
        private const val INSTANCE_PROGRESS = "progress"
        private const val INSTANCE_MAX = "max"
        private const val INSTANCE_FINISHED_STROKE_COLOR = "finished_stroke_color"
        private const val INSTANCE_UNFINISHED_STROKE_COLOR = "unfinished_stroke_color"
        private const val INSTANCE_ARC_ANGLE = "arc_angle"
        private const val INSTANCE_SUFFIX = "suffix"
    }
}