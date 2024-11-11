package org.rivchain.cuplink.renderer

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import org.rivchain.cuplink.R

class DescriptiveTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val titleTextView: TextView
    val subtitleTextView: TextView


    init {
        // Retrieve custom attributes
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DescriptiveTextView)

        orientation = VERTICAL

        titleTextView = TextView(context).apply {
            id = View.generateViewId()
        }
        subtitleTextView = TextView(context).apply {
            id = View.generateViewId()
        }

        addView(titleTextView)
        addView(subtitleTextView)

        context.withStyledAttributes(attrs, R.styleable.DescriptiveTextView) {
            titleTextView.text = getString(R.styleable.DescriptiveTextView_titleText) ?: ""
            subtitleTextView.text = getString(R.styleable.DescriptiveTextView_subtitleText) ?: ""
            if(getString(R.styleable.DescriptiveTextView_subtitleText)?.isEmpty() == true){
                subtitleTextView.visibility = View.GONE
            }

            val titleFontFamilyResId = getResourceId(R.styleable.DescriptiveTextView_titleFontFamily, -1)
            if (titleFontFamilyResId != -1) {
                val typeface = ResourcesCompat.getFont(context, titleFontFamilyResId)
                titleTextView.typeface = typeface

            }

            val subtitleFontFamilyResId = getResourceId(R.styleable.DescriptiveTextView_subtitleFontFamily, -1)
            if (subtitleFontFamilyResId != -1) {
                val typeface = ResourcesCompat.getFont(context, subtitleFontFamilyResId)
                subtitleTextView.typeface = typeface
            }

            val titleTextSize = typedArray.getDimensionPixelSize(R.styleable.DescriptiveTextView_titleTextSize, 16)
            val subtitleTextSize = typedArray.getDimensionPixelSize(R.styleable.DescriptiveTextView_subtitleTextSize, 12)

            // Apply text size in PX, respecting SP scaling
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleTextSize.toFloat())
            subtitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, subtitleTextSize.toFloat())


            // Recycle TypedArray to free up resources
            typedArray.recycle()
        }
    }

    /**
     * Sets constraints and margin parameters for the custom LinearLayout
     */
    fun applyConstraints(constraintLayout: ConstraintLayout, customId: Int, startToStartOf: Int, endToStartOf: Int, topToTopOf: Int) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        constraintSet.apply {
            connect(customId, ConstraintSet.START, startToStartOf, ConstraintSet.START)
            connect(customId, ConstraintSet.TOP, topToTopOf, ConstraintSet.TOP)
            connect(customId, ConstraintSet.END, endToStartOf, ConstraintSet.START)
            setMargin(customId, ConstraintSet.END, resources.getDimensionPixelSize(R.dimen.padding_20))
            setMargin(customId, ConstraintSet.TOP, resources.getDimensionPixelSize(R.dimen.padding_20))
        }
        constraintSet.applyTo(constraintLayout)
    }
}
