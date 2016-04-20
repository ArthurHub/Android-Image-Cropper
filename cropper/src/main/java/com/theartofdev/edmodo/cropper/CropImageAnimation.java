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

package com.theartofdev.edmodo.cropper;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;

/**
 * TODO:a add doc
 */
class CropImageAnimation extends Animation {

    //region: Fields and Consts

    private final ImageView mImageView;

    private final CropOverlayView mCropOverlayView;

    private final RectF mBeforeImageRect = new RectF();

    private final RectF mAfterImageRect = new RectF();

    private final RectF mBeforeCropWindowRect = new RectF();

    private final RectF mAfterCropWindowRect = new RectF();

    private final float[] mBeforeImageMatrix = new float[9];

    private final float[] mAfterImageMatrix = new float[9];

    private final RectF mAnimRect = new RectF();

    private final float[] mAnimMatrix = new float[9];
    //endregion

    public CropImageAnimation(ImageView cropImageView, CropOverlayView cropOverlayView) {
        mImageView = cropImageView;
        mCropOverlayView = cropOverlayView;

        setDuration(300);
        setFillAfter(true);
        setInterpolator(new AccelerateDecelerateInterpolator());
    }

    public void setBefore(RectF imageRect, Matrix imageMatrix) {
        reset();
        mBeforeImageRect.set(imageRect);
        mBeforeCropWindowRect.set(mCropOverlayView.getCropWindowRect());
        imageMatrix.getValues(mBeforeImageMatrix);
    }

    public void setAfter(RectF imageRect, Matrix imageMatrix) {
        mAfterImageRect.set(imageRect);
        mAfterCropWindowRect.set(mCropOverlayView.getCropWindowRect());
        imageMatrix.getValues(mAfterImageMatrix);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {

        mAnimRect.left = mBeforeCropWindowRect.left + (mAfterCropWindowRect.left - mBeforeCropWindowRect.left) * interpolatedTime;
        mAnimRect.top = mBeforeCropWindowRect.top + (mAfterCropWindowRect.top - mBeforeCropWindowRect.top) * interpolatedTime;
        mAnimRect.right = mBeforeCropWindowRect.right + (mAfterCropWindowRect.right - mBeforeCropWindowRect.right) * interpolatedTime;
        mAnimRect.bottom = mBeforeCropWindowRect.bottom + (mAfterCropWindowRect.bottom - mBeforeCropWindowRect.bottom) * interpolatedTime;
        mCropOverlayView.setCropWindowRect(mAnimRect);

        mAnimRect.left = mBeforeImageRect.left + (mAfterImageRect.left - mBeforeImageRect.left) * interpolatedTime;
        mAnimRect.top = mBeforeImageRect.top + (mAfterImageRect.top - mBeforeImageRect.top) * interpolatedTime;
        mAnimRect.right = mBeforeImageRect.right + (mAfterImageRect.right - mBeforeImageRect.right) * interpolatedTime;
        mAnimRect.bottom = mBeforeImageRect.bottom + (mAfterImageRect.bottom - mBeforeImageRect.bottom) * interpolatedTime;
        mCropOverlayView.setBitmapRect(mAnimRect, mImageView.getWidth(), mImageView.getHeight());

        for (int i = 0; i < mAnimMatrix.length; i++) {
            mAnimMatrix[i] = mBeforeImageMatrix[i] + (mAfterImageMatrix[i] - mBeforeImageMatrix[i]) * interpolatedTime;
        }
        Matrix m = mImageView.getImageMatrix();
        m.setValues(mAnimMatrix);
        mImageView.setImageMatrix(m);

        mImageView.invalidate();
        mCropOverlayView.invalidate();
    }
}

