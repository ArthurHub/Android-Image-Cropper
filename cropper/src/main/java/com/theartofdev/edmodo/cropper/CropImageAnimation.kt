// "Therefore those skilled at the unorthodox
// are infinite as heaven and earth,
// inexhaustible as the great rivers.
// When they come to an end,
// they begin again,
// like the days and months;
// they die and are reborn,
// like the four seasons."
//
// - Sun Tsu,
// "The Art of War"
package com.theartofdev.edmodo.cropper

import android.graphics.Matrix
import android.graphics.RectF
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.Transformation
import android.widget.ImageView

/**
 * Animation to handle smooth cropping image matrix transformation change, specifically for
 * zoom-in/out.
 */
internal class CropImageAnimation(// region: Fields and Consts
        private val mImageView: ImageView, private val mCropOverlayView: CropOverlayView?) : Animation(), AnimationListener {
    private val mStartBoundPoints = FloatArray(8)
    private val mEndBoundPoints = FloatArray(8)
    private val mStartCropWindowRect = RectF()
    private val mEndCropWindowRect = RectF()
    private val mStartImageMatrix = FloatArray(9)
    private val mEndImageMatrix = FloatArray(9)
    private val mAnimRect = RectF()
    private val mAnimPoints = FloatArray(8)
    private val mAnimMatrix = FloatArray(9)
    fun setStartState(boundPoints: FloatArray?, imageMatrix: Matrix) {
        reset()
        System.arraycopy(boundPoints, 0, mStartBoundPoints, 0, 8)
        mStartCropWindowRect.set(mCropOverlayView!!.cropWindowRect!!)
        imageMatrix.getValues(mStartImageMatrix)
    }

    fun setEndState(boundPoints: FloatArray?, imageMatrix: Matrix) {
        System.arraycopy(boundPoints, 0, mEndBoundPoints, 0, 8)
        mEndCropWindowRect.set(mCropOverlayView!!.cropWindowRect!!)
        imageMatrix.getValues(mEndImageMatrix)
    }

    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
        mAnimRect.left = (mStartCropWindowRect.left
                + (mEndCropWindowRect.left - mStartCropWindowRect.left) * interpolatedTime)
        mAnimRect.top = (mStartCropWindowRect.top
                + (mEndCropWindowRect.top - mStartCropWindowRect.top) * interpolatedTime)
        mAnimRect.right = (mStartCropWindowRect.right
                + (mEndCropWindowRect.right - mStartCropWindowRect.right) * interpolatedTime)
        mAnimRect.bottom = (mStartCropWindowRect.bottom
                + (mEndCropWindowRect.bottom - mStartCropWindowRect.bottom) * interpolatedTime)
        mCropOverlayView!!.cropWindowRect = mAnimRect
        for (i in mAnimPoints.indices) {
            mAnimPoints[i] = mStartBoundPoints[i] + (mEndBoundPoints[i] - mStartBoundPoints[i]) * interpolatedTime
        }
        mCropOverlayView!!.setBounds(mAnimPoints, mImageView.width, mImageView.height)
        for (i in mAnimMatrix.indices) {
            mAnimMatrix[i] = mStartImageMatrix[i] + (mEndImageMatrix[i] - mStartImageMatrix[i]) * interpolatedTime
        }
        val m = mImageView.imageMatrix
        m.setValues(mAnimMatrix)
        mImageView.imageMatrix = m
        mImageView.invalidate()
        mCropOverlayView.invalidate()
    }

    override fun onAnimationStart(animation: Animation) {}
    override fun onAnimationEnd(animation: Animation) {
        mImageView.clearAnimation()
    }

    override fun onAnimationRepeat(animation: Animation) {}

    // endregion
    init {
        duration = 300
        fillAfter = true
        interpolator = AccelerateDecelerateInterpolator()
        setAnimationListener(this)
    }
}