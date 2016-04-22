/*
 * Copyright 2013, Edmodo, Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file, or at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License. 
 */

package com.theartofdev.edmodo.cropper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

/**
 * Custom view that provides cropping capabilities to an image.
 */
public class CropImageView extends FrameLayout {

    //region: Fields and Consts

    /**
     * Image view widget used to show the image for cropping.
     */
    private final ImageView mImageView;

    /**
     * Overlay over the image view to show cropping UI.
     */
    private final CropOverlayView mCropOverlayView;

    /**
     * Progress bar widget to show progress bar on async image loading and cropping.
     */
    private final ProgressBar mProgressBar;

    private Bitmap mBitmap;

    private int mDegreesRotated;

    private int mLayoutWidth;

    private int mLayoutHeight;

    private int mImageResource;

    /**
     * if to show crop overlay UI what contains the crop window UI surrounded by background over the cropping
     * image.<br>
     * default: true, may disable for animation or frame transition.
     */
    private boolean mShowCropOverlay = true;

    /**
     * if to show progress bar when image async loading/cropping is in progress.<br>
     * default: true, disable to provide custom progress bar UI.
     */
    private boolean mShowProgressBar = true;

    /**
     * callback to be invoked when image async loading is complete
     */
    private WeakReference<OnSetImageUriCompleteListener> mOnSetImageUriCompleteListener;

    /**
     * callback to be invoked when image async cropping is complete
     */
    private WeakReference<OnGetCroppedImageCompleteListener> mOnGetCroppedImageCompleteListener;

    /**
     * The URI that the image was loaded from (if loaded from URI)
     */
    private Uri mLoadedImageUri;

    /**
     * The sample size the image was loaded by if was loaded by URI
     */
    private int mLoadedSampleSize = 1;

    /**
     * Task used to load bitmap async from UI thread
     */
    private WeakReference<BitmapLoadingWorkerTask> mBitmapLoadingWorkerTask;

    /**
     * Task used to crop bitmap async from UI thread
     */
    private WeakReference<BitmapCroppingWorkerTask> mBitmapCroppingWorkerTask;
    //endregion

    public CropImageView(Context context) {
        this(context, null);
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        DisplayMetrics dm = getResources().getDisplayMetrics();

        boolean fixAspectRatio = CropDefaults.DEFAULT_FIXED_ASPECT_RATIO;
        int aspectRatioX = CropDefaults.DEFAULT_ASPECT_RATIO_X;
        int aspectRatioY = CropDefaults.DEFAULT_ASPECT_RATIO_Y;
        ImageView.ScaleType scaleType = CropDefaults.VALID_SCALE_TYPES[CropDefaults.DEFAULT_SCALE_TYPE_INDEX];
        CropShape cropShape = CropShape.RECTANGLE;
        Guidelines guidelines = CropImageView.Guidelines.ON_TOUCH;
        float snapRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CropDefaults.SNAP_RADIUS, dm);
        float touchRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CropDefaults.TOUCH_RADIUS, dm);
        float initialCropWindowPaddingRatio = CropDefaults.DEFAULT_INITIAL_CROP_WINDOW_PADDING_RATIO;
        float borderLineThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CropDefaults.DEFAULT_BORDER_LINE_THICKNESS, dm);
        int borderLineColor = CropDefaults.DEFAULT_BORDER_LINE_COLOR;
        float borderCornerThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CropDefaults.DEFAULT_BORDER_CORNER_THICKNESS, dm);
        float borderCornerOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CropDefaults.DEFAULT_BORDER_CORNER_OFFSET, dm);
        float borderCornerLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CropDefaults.DEFAULT_BORDER_CORNER_LENGTH, dm);
        int borderCornerColor = CropDefaults.DEFAULT_BORDER_CORNER_COLOR;
        float guidelinesThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CropDefaults.DEFAULT_GUIDELINE_THICKNESS, dm);
        int guidelinesColor = CropDefaults.DEFAULT_GUIDELINE_COLOR;
        int backgroundColor = CropDefaults.DEFAULT_BACKGROUND_COLOR;
        float minCropWindowWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CropDefaults.MIN_CROP_WINDOW_SIZE, dm);
        float minCropWindowHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CropDefaults.MIN_CROP_WINDOW_SIZE, dm);
        float minCropResultWidth = CropDefaults.MIN_CROP_RESULT_SIZE;
        float minCropResultHeight = CropDefaults.MIN_CROP_RESULT_SIZE;
        float maxCropResultWidth = CropDefaults.MAX_CROP_RESULT_SIZE;
        float maxCropResultHeight = CropDefaults.MAX_CROP_RESULT_SIZE;
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CropImageView, 0, 0);
            try {
                fixAspectRatio = ta.getBoolean(R.styleable.CropImageView_cropFixAspectRatio, CropDefaults.DEFAULT_FIXED_ASPECT_RATIO);
                aspectRatioX = ta.getInteger(R.styleable.CropImageView_cropAspectRatioX, CropDefaults.DEFAULT_ASPECT_RATIO_X);
                aspectRatioY = ta.getInteger(R.styleable.CropImageView_cropAspectRatioY, CropDefaults.DEFAULT_ASPECT_RATIO_Y);
                scaleType = CropDefaults.VALID_SCALE_TYPES[ta.getInt(R.styleable.CropImageView_cropScaleType, CropDefaults.DEFAULT_SCALE_TYPE_INDEX)];
                cropShape = CropDefaults.VALID_CROP_SHAPES[ta.getInt(R.styleable.CropImageView_cropShape, CropDefaults.DEFAULT_CROP_SHAPE_INDEX)];
                guidelines = CropDefaults.VALID_GUIDELINES[ta.getInt(R.styleable.CropImageView_cropGuidelines, CropDefaults.DEFAULT_GUIDELINES_INDEX)];
                snapRadius = ta.getDimension(R.styleable.CropImageView_cropSnapRadius, snapRadius);
                touchRadius = ta.getDimension(R.styleable.CropImageView_cropTouchRadius, touchRadius);
                initialCropWindowPaddingRatio = ta.getFloat(R.styleable.CropImageView_cropInitialCropWindowPaddingRatio, initialCropWindowPaddingRatio);
                borderLineThickness = ta.getDimension(R.styleable.CropImageView_cropBorderLineThickness, borderLineThickness);
                borderLineColor = ta.getInteger(R.styleable.CropImageView_cropBorderLineColor, borderLineColor);
                borderCornerThickness = ta.getDimension(R.styleable.CropImageView_cropBorderCornerThickness, borderCornerThickness);
                borderCornerOffset = ta.getDimension(R.styleable.CropImageView_cropBorderCornerOffset, borderCornerOffset);
                borderCornerLength = ta.getDimension(R.styleable.CropImageView_cropBorderCornerLength, borderCornerLength);
                borderCornerColor = ta.getInteger(R.styleable.CropImageView_cropBorderCornerColor, borderCornerColor);
                guidelinesThickness = ta.getDimension(R.styleable.CropImageView_cropGuidelinesThickness, guidelinesThickness);
                guidelinesColor = ta.getInteger(R.styleable.CropImageView_cropGuidelinesColor, guidelinesColor);
                backgroundColor = ta.getInteger(R.styleable.CropImageView_cropBackgroundColor, backgroundColor);
                mShowCropOverlay = ta.getBoolean(R.styleable.CropImageView_cropShowCropOverlay, mShowCropOverlay);
                mShowProgressBar = ta.getBoolean(R.styleable.CropImageView_cropShowProgressBar, mShowProgressBar);
                borderCornerThickness = ta.getDimension(R.styleable.CropImageView_cropBorderCornerThickness, borderCornerThickness);
                minCropWindowWidth = ta.getDimension(R.styleable.CropImageView_cropMinCropWindowWidth, minCropWindowWidth);
                minCropWindowHeight = ta.getDimension(R.styleable.CropImageView_cropMinCropWindowHeight, minCropWindowHeight);
                minCropResultWidth = ta.getFloat(R.styleable.CropImageView_cropMinCropResultWidthPX, minCropResultWidth);
                minCropResultHeight = ta.getFloat(R.styleable.CropImageView_cropMinCropResultHeightPX, minCropResultHeight);
                maxCropResultWidth = ta.getFloat(R.styleable.CropImageView_cropMaxCropResultWidthPX, maxCropResultWidth);
                maxCropResultHeight = ta.getFloat(R.styleable.CropImageView_cropMaxCropResultHeightPX, maxCropResultHeight);
            } finally {
                ta.recycle();
            }
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.crop_image_view, this, true);

        mImageView = (ImageView) v.findViewById(R.id.ImageView_image);
        mImageView.setScaleType(scaleType);

        mCropOverlayView = (CropOverlayView) v.findViewById(R.id.CropOverlayView);
        mCropOverlayView.setInitialAttributeValues(
                cropShape, snapRadius, touchRadius, guidelines,
                fixAspectRatio, aspectRatioX, aspectRatioY,
                initialCropWindowPaddingRatio,
                borderLineThickness, borderLineColor,
                borderCornerThickness, borderCornerOffset, borderCornerLength, borderCornerColor,
                guidelinesThickness, guidelinesColor,
                backgroundColor,
                minCropWindowWidth, minCropWindowHeight,
                minCropResultWidth, minCropResultHeight,
                maxCropResultWidth, maxCropResultHeight);

        mProgressBar = (ProgressBar) v.findViewById(R.id.CropProgressBar);
        setProgressBarVisibility();
    }

    /**
     * Get the scale type of the image in the crop view.
     */
    public ImageView.ScaleType getScaleType() {
        return mImageView.getScaleType();
    }

    /**
     * Get the amount of degrees the cropping image is rotated cloackwise.<br>
     *
     * @return 0-360
     */
    public int getRotatedDegrees() {
        return mDegreesRotated;
    }

    /**
     * Set the scale type of the image in the crop view
     */
    public void setScaleType(ImageView.ScaleType scaleType) {
        if (scaleType != mImageView.getScaleType()) {
            mImageView.setScaleType(scaleType);
            mCropOverlayView.resetCropOverlayView();
        }
    }

    /**
     * The shape of the cropping area - rectangle/circular.
     */
    public CropShape getCropShape() {
        return mCropOverlayView.getCropShape();
    }

    /**
     * The shape of the cropping area - rectangle/circular.
     */
    public void setCropShape(CropShape cropShape) {
        mCropOverlayView.setCropShape(cropShape);
    }

    /**
     * whether the aspect ratio is fixed or not; true fixes the aspect ratio, while false allows it to be changed.
     */
    public boolean isFixAspectRatio() {
        return mCropOverlayView.isFixAspectRatio();
    }

    /**
     * Sets whether the aspect ratio is fixed or not; true fixes the aspect ratio, while false allows it to be changed.
     */
    public void setFixedAspectRatio(boolean fixAspectRatio) {
        mCropOverlayView.setFixedAspectRatio(fixAspectRatio);
    }

    /**
     * Get the current guidelines option set.
     */
    public Guidelines getGuidelines() {
        return mCropOverlayView.getGuidelines();
    }

    /**
     * Sets the guidelines for the CropOverlayView to be either on, off, or to show when resizing the application.
     */
    public void setGuidelines(Guidelines guidelines) {
        mCropOverlayView.setGuidelines(guidelines);
    }

    /**
     * both the X and Y values of the aspectRatio.
     */
    public Pair<Integer, Integer> getAspectRatio() {
        return new Pair<>(mCropOverlayView.getAspectRatioX(), mCropOverlayView.getAspectRatioY());
    }

    /**
     * Sets the both the X and Y values of the aspectRatio.
     *
     * @param aspectRatioX int that specifies the new X value of the aspect ratio
     * @param aspectRatioY int that specifies the new Y value of the aspect ratio
     */
    public void setAspectRatio(int aspectRatioX, int aspectRatioY) {
        mCropOverlayView.setAspectRatioX(aspectRatioX);
        mCropOverlayView.setAspectRatioY(aspectRatioY);
    }

    /**
     * An edge of the crop window will snap to the corresponding edge of a
     * specified bounding box when the crop window edge is less than or equal to
     * this distance (in pixels) away from the bounding box edge. (default: 3dp)
     */
    public void setSnapRadius(float snapRadius) {
        if (snapRadius >= 0) {
            mCropOverlayView.setSnapRadius(snapRadius);
        }
    }

    /**
     * if to show progress bar when image async loading/cropping is in progress.<br>
     * default: true, disable to provide custom progress bar UI.
     */
    public boolean isShowProgressBar() {
        return mShowProgressBar;
    }

    /**
     * if to show crop overlay UI what contains the crop window UI surrounded by background over the cropping
     * image.<br>
     * default: true, may disable for animation or frame transition.
     */
    public boolean isShowCropOverlay() {
        return mShowCropOverlay;
    }

    /**
     * if to show crop overlay UI what contains the crop window UI surrounded by background over the cropping
     * image.<br>
     * default: true, may disable for animation or frame transition.
     */
    public void setShowCropOverlay(boolean showCropOverlay) {
        if (mShowCropOverlay != showCropOverlay) {
            mShowCropOverlay = showCropOverlay;
            setCropOverlayVisibility();
        }
    }

    /**
     * if to show progress bar when image async loading/cropping is in progress.<br>
     * default: true, disable to provide custom progress bar UI.
     */
    public void setShowProgressBar(boolean showProgressBar) {
        if (mShowProgressBar != showProgressBar) {
            mShowProgressBar = showProgressBar;
            setProgressBarVisibility();
        }
    }

    /**
     * Returns the integer of the imageResource
     */
    public int getImageResource() {
        return mImageResource;
    }

    /**
     * Get the URI of an image that was set by URI, null otherwise.
     */
    public Uri getImageUri() {
        return mLoadedImageUri;
    }

    /**
     * Set the crop window position and size to the given rectangle.<br>
     * Image to crop must be first set before invoking this, for async after complete callback.
     *
     * @param rect window rectangle (position and size) relative to source bitmap
     */
    public void setCropRect(Rect rect) {
        mCropOverlayView.setInitialCropWindowRect(rect);
    }

    /**
     * Gets the crop window's position relative to the source Bitmap (not the image
     * displayed in the CropImageView).
     *
     * @return a Rect instance containing cropped area boundaries of the source Bitmap
     */
    public Rect getActualCropRect() {
        if (mBitmap != null) {
            Rect displayedImageRect = BitmapUtils.getBitmapRect(mBitmap, mImageView, mImageView.getScaleType());

            // Get the scale factor between the actual Bitmap dimensions and the displayed dimensions.
            Pair<Float, Float> scaleFactor = getScaleFactorWidth(displayedImageRect);

            // Get crop window position relative to the displayed image.
            RectF cropWindowRect = mCropOverlayView.getCropWindowRect();
            float displayedCropLeft = cropWindowRect.left - displayedImageRect.left;
            float displayedCropTop = cropWindowRect.top - displayedImageRect.top;
            float displayedCropWidth = cropWindowRect.width();
            float displayedCropHeight = cropWindowRect.height();

            // Scale the crop window position to the actual size of the Bitmap.
            float actualCropLeft = displayedCropLeft * scaleFactor.first;
            float actualCropTop = displayedCropTop * scaleFactor.second;
            float actualCropRight = actualCropLeft + displayedCropWidth * scaleFactor.first;
            float actualCropBottom = actualCropTop + displayedCropHeight * scaleFactor.second;

            // Correct for floating point errors. Crop rect boundaries should not exceed the source Bitmap bounds.
            actualCropLeft = Math.round(Math.max(0f, actualCropLeft) * mLoadedSampleSize);
            actualCropTop = Math.round(Math.max(0f, actualCropTop) * mLoadedSampleSize);
            actualCropRight = Math.round(Math.min(mBitmap.getWidth(), actualCropRight) * mLoadedSampleSize);
            actualCropBottom = Math.round(Math.min(mBitmap.getHeight(), actualCropBottom) * mLoadedSampleSize);

            // extra cehck to be sure that width and height are equal if 1:1 fixed aspect ratio is requested
            if (mCropOverlayView.isFixAspectRatio()
                    && mCropOverlayView.getAspectRatioX() == mCropOverlayView.getAspectRatioY()
                    && actualCropBottom - actualCropTop != actualCropRight - actualCropLeft) {
                if (actualCropBottom - actualCropTop > actualCropRight - actualCropLeft) {
                    actualCropBottom -= actualCropBottom - actualCropTop - (actualCropRight - actualCropLeft);
                } else {
                    actualCropRight -= actualCropRight - actualCropLeft - (actualCropBottom - actualCropTop);
                }
            }

            return new Rect((int) actualCropLeft, (int) actualCropTop, (int) actualCropRight, (int) actualCropBottom);
        } else {
            return null;
        }
    }

    /**
     * Gets the crop window's position relative to the source Bitmap (not the image
     * displayed in the CropImageView) using the original image rotation rotation - if the image was rotated after it
     * was, this rotation is ignored.
     *
     * @return a Rect instance containing cropped area boundaries of the source Bitmap
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public Rect getActualCropRectNoRotation() {
        if (mBitmap != null) {
            Rect rect = getActualCropRect();
            int rotateSide = mDegreesRotated / 90;
            int bitmapWidth = mBitmap.getWidth() * mLoadedSampleSize;
            int bitmapHeight = mBitmap.getHeight() * mLoadedSampleSize;
            if (rotateSide == 1) {
                rect.set(rect.top, bitmapWidth - rect.right, rect.bottom, bitmapWidth - rect.left);
            } else if (rotateSide == 2) {
                rect.set(bitmapWidth - rect.right, bitmapHeight - rect.bottom, bitmapWidth - rect.left, bitmapHeight - rect.top);
            } else if (rotateSide == 3) {
                rect.set(bitmapHeight - rect.bottom, rect.left, bitmapHeight - rect.top, rect.right);
            }
            rect.set(rect.left, rect.top, rect.right, rect.bottom);
            return rect;
        } else {
            return null;
        }
    }

    /**
     * Gets the cropped image based on the current crop window.
     *
     * @return a new Bitmap representing the cropped image
     */
    public Bitmap getCroppedImage() {
        return getCroppedImage(0, 0);
    }

    /**
     * Gets the cropped image based on the current crop window.<br>
     * If image loaded from URI will use sample size to fit in the requested width and height down-sampling
     * if required - optimization to get best size to quality.<br>
     * NOTE: resulting image will not be exactly (reqWidth, reqHeight)
     * see: <a href="http://developer.android.com/training/displaying-bitmaps/load-bitmap.html">Loading Large
     * Bitmaps Efficiently</a>
     *
     * @return a new Bitmap representing the cropped image
     */
    public Bitmap getCroppedImage(int reqWidth, int reqHeight) {
        if (mBitmap != null) {
            if (mLoadedImageUri != null && mLoadedSampleSize > 1) {
                return BitmapUtils.cropBitmap(
                        getContext(),
                        mLoadedImageUri,
                        getActualCropRectNoRotation(),
                        mDegreesRotated,
                        reqWidth,
                        reqHeight);
            } else {
                return BitmapUtils.cropBitmap(mBitmap, getActualCropRect());
            }
        } else {
            return null;
        }
    }

    /**
     * Gets the cropped circle image based on the current crop selection.<br>
     * Same as {@link #getCroppedImage()} (as the bitmap is rectangular by nature) but the pixels beyond the
     * oval crop will be transparent.
     *
     * @return a new Circular Bitmap representing the cropped image
     */
    public Bitmap getCroppedOvalImage() {
        if (mBitmap != null) {
            Bitmap cropped = getCroppedImage();
            return BitmapUtils.toOvalBitmap(cropped);
        } else {
            return null;
        }
    }

    /**
     * Gets the cropped image based on the current crop window.<br>
     * Get rectangle crop shape matching exactly the visual crop window pixel-to-pixel.<br>
     * The result will be invoked to listener set by {@link #setOnGetCroppedImageCompleteListener(OnGetCroppedImageCompleteListener)}.
     */
    public void getCroppedImageAsync() {
        getCroppedImageAsync(CropShape.RECTANGLE, 0, 0);
    }

    /**
     * Gets the cropped image based on the current crop window.<br>
     * Use the given cropShape to "fix" resulting crop image for {@link CropShape#OVAL} by setting pixels
     * outside the oval (circular) shape to transparent.<br>
     * The result will be invoked to listener set by {@link #setOnGetCroppedImageCompleteListener(OnGetCroppedImageCompleteListener)}.
     *
     * @param cropShape the shape to crop the image: {@link CropShape#RECTANGLE} will get the raw crop rectangle from
     * the image, {@link CropShape#OVAL} will "fix" rectangle to oval by setting outside pixels to transparent.
     */
    public void getCroppedImageAsync(CropShape cropShape) {
        getCroppedImageAsync(cropShape, 0, 0);
    }

    /**
     * Gets the cropped image based on the current crop window.<br>
     * Use the given cropShape to "fix" resulting crop image for {@link CropShape#OVAL} by setting pixels
     * outside the oval (circular) shape to transparent.<br>
     * If (reqWidth,reqHeight) is given AND image is loaded from URI cropping will try to use sample size to fit in
     * the requested width and height down-sampling if possible - optimization to get best size to quality.<br>
     * NOTE: resulting image will not be exactly (reqWidth, reqHeight)
     * see: <a href="http://developer.android.com/training/displaying-bitmaps/load-bitmap.html">Loading Large
     * Bitmaps Efficiently</a><br>
     * The result will be invoked to listener set by {@link #setOnGetCroppedImageCompleteListener(OnGetCroppedImageCompleteListener)}.
     *
     * @param cropShape the shape to crop the image: {@link CropShape#RECTANGLE} will get the raw crop rectangle from
     * the image, {@link CropShape#OVAL} will "fix" rectangle to oval by setting outside pixels to transparent.
     * @param reqWidth the width to downsample the cropped image to
     * @param reqHeight the height to downsample the cropped image to
     */
    public void getCroppedImageAsync(CropShape cropShape, int reqWidth, int reqHeight) {
        if (mOnGetCroppedImageCompleteListener == null) {
            throw new IllegalArgumentException("OnGetCroppedImageCompleteListener is not set");
        }
        BitmapCroppingWorkerTask currentTask = mBitmapCroppingWorkerTask != null ? mBitmapCroppingWorkerTask.get() : null;
        if (currentTask != null) {
            // cancel previous cropping
            currentTask.cancel(true);
        }

        mBitmapCroppingWorkerTask = mLoadedImageUri != null && mLoadedSampleSize > 1
                ? new WeakReference<>(new BitmapCroppingWorkerTask(this, mLoadedImageUri, getActualCropRectNoRotation(), cropShape, mDegreesRotated, reqWidth, reqHeight))
                : new WeakReference<>(new BitmapCroppingWorkerTask(this, mBitmap, getActualCropRect(), cropShape));
        mBitmapCroppingWorkerTask.get().execute();
        setProgressBarVisibility();
    }

    /**
     * Set the callback to be invoked when image async loading ({@link #setImageUriAsync(Uri)})
     * is complete (successful or failed).
     */
    public void setOnSetImageUriCompleteListener(OnSetImageUriCompleteListener listener) {
        mOnSetImageUriCompleteListener = listener != null ? new WeakReference<>(listener) : null;
    }

    /**
     * Set the callback to be invoked when image async cropping ({@link #getCroppedImageAsync()})
     * is complete (successful or failed).
     */
    public void setOnGetCroppedImageCompleteListener(OnGetCroppedImageCompleteListener listener) {
        mOnGetCroppedImageCompleteListener = listener != null ? new WeakReference<>(listener) : null;
    }

    /**
     * Sets a Bitmap as the content of the CropImageView.
     *
     * @param bitmap the Bitmap to set
     */
    public void setImageBitmap(Bitmap bitmap) {
        mCropOverlayView.setInitialCropWindowRect(null);
        setBitmap(bitmap, true);
    }

    /**
     * Sets a Bitmap and initializes the image rotation according to the EXIT data.<br>
     * <br>
     * The EXIF can be retrieved by doing the following:
     * <code>ExifInterface exif = new ExifInterface(path);</code>
     *
     * @param bitmap the original bitmap to set; if null, this
     * @param exif the EXIF information about this bitmap; may be null
     */
    public void setImageBitmap(Bitmap bitmap, ExifInterface exif) {
        Bitmap setBitmap;
        if (bitmap != null && exif != null) {
            BitmapUtils.RotateBitmapResult result = BitmapUtils.rotateBitmapByExif(bitmap, exif);
            setBitmap = result.bitmap;
            mDegreesRotated = result.degrees;
        } else {
            setBitmap = bitmap;
        }
        mCropOverlayView.setInitialCropWindowRect(null);
        setBitmap(setBitmap, true);
    }

    /**
     * Sets a Drawable as the content of the CropImageView.
     *
     * @param resId the drawable resource ID to set
     */
    public void setImageResource(int resId) {
        if (resId != 0) {
            mCropOverlayView.setInitialCropWindowRect(null);
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
            setBitmap(bitmap, true);
            mImageResource = resId;
        }
    }

    /**
     * Sets a bitmap loaded from the given Android URI as the content of the CropImageView.<br>
     * Can be used with URI from gallery or camera source.<br>
     * Will rotate the image by exif data.<br>
     *
     * @param uri the URI to load the image from
     * @deprecated Use {@link #setImageUriAsync(Uri)} for better async handling
     */
    @Deprecated
    public void setImageUri(Uri uri) {
        if (uri != null) {

            mCropOverlayView.setInitialCropWindowRect(null);

            DisplayMetrics metrics = getResources().getDisplayMetrics();
            double densityAdj = metrics.density > 1 ? 1 / metrics.density : 1;

            int width = (int) (metrics.widthPixels * densityAdj);
            int height = (int) (metrics.heightPixels * densityAdj);
            BitmapUtils.DecodeBitmapResult decodeResult =
                    BitmapUtils.decodeSampledBitmap(getContext(), uri, width, height);

            BitmapUtils.RotateBitmapResult rotateResult =
                    BitmapUtils.rotateBitmapByExif(getContext(), decodeResult.bitmap, uri);

            setBitmap(rotateResult.bitmap, true);

            mLoadedImageUri = uri;
            mLoadedSampleSize = decodeResult.sampleSize;
            mDegreesRotated = rotateResult.degrees;
        }
    }

    /**
     * Sets a bitmap loaded from the given Android URI as the content of the CropImageView.<br>
     * Can be used with URI from gallery or camera source.<br>
     * Will rotate the image by exif data.<br>
     *
     * @param uri the URI to load the image from
     */
    public void setImageUriAsync(Uri uri) {
        setImageUriAsync(uri, null);
    }

    /**
     * Clear the current image set for cropping.
     */
    public void clearImage() {
        clearImage(true);
        mCropOverlayView.setInitialCropWindowRect(null);
    }

    /**
     * Rotates image by the specified number of degrees clockwise.<br>
     * Cycles from 0 to 360 degrees.
     *
     * @param degrees Integer specifying the number of degrees to rotate.
     */
    public void rotateImage(int degrees) {
        if (mBitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            Bitmap bitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
            setBitmap(bitmap, false);

            mDegreesRotated += degrees;
            mDegreesRotated = mDegreesRotated % 360;
        }
    }

    //region: Private methods

    /**
     * Load image from given URI async using {@link BitmapLoadingWorkerTask}<br>
     * optionally rotate the loaded image given degrees, used for restore state.
     */
    private void setImageUriAsync(Uri uri, Integer preSetRotation) {
        if (uri != null) {
            BitmapLoadingWorkerTask currentTask = mBitmapLoadingWorkerTask != null ? mBitmapLoadingWorkerTask.get() : null;
            if (currentTask != null) {
                // cancel previous loading (no check if the same URI because camera URI can be the same for different images)
                currentTask.cancel(true);
            }

            // either no existing task is working or we canceled it, need to load new URI
            clearImage(true);
            mCropOverlayView.setInitialCropWindowRect(null);
            mBitmapLoadingWorkerTask = new WeakReference<>(new BitmapLoadingWorkerTask(this, uri, preSetRotation));
            mBitmapLoadingWorkerTask.get().execute();
            setProgressBarVisibility();
        }
    }

    /**
     * On complete of the async bitmap loading by {@link #setImageUriAsync(Uri)} set the result
     * to the widget if still relevant and call listener if set.
     *
     * @param result the result of bitmap loading
     */
    void onSetImageUriAsyncComplete(BitmapLoadingWorkerTask.Result result) {

        mBitmapLoadingWorkerTask = null;
        setProgressBarVisibility();

        if (result.error == null) {
            setBitmap(result.bitmap, true);
            mLoadedImageUri = result.uri;
            mLoadedSampleSize = result.loadSampleSize;
            mDegreesRotated = result.degreesRotated;
        }

        OnSetImageUriCompleteListener listener = mOnSetImageUriCompleteListener != null
                ? mOnSetImageUriCompleteListener.get() : null;
        if (listener != null) {
            listener.onSetImageUriComplete(this, result.uri, result.error);
        }
    }

    /**
     * On complete of the async bitmap cropping by {@link #getCroppedImageAsync()} call listener if set.
     *
     * @param result the result of bitmap cropping
     */
    void onGetImageCroppingAsyncComplete(BitmapCroppingWorkerTask.Result result) {

        mBitmapCroppingWorkerTask = null;
        setProgressBarVisibility();

        OnGetCroppedImageCompleteListener listener = mOnGetCroppedImageCompleteListener != null
                ? mOnGetCroppedImageCompleteListener.get() : null;
        if (listener != null) {
            listener.onGetCroppedImageComplete(this, result.bitmap, result.error);
        }
    }

    /**
     * Set the given bitmap to be used in for cropping<br>
     * Optionally clear full if the bitmap is new, or partial clear if the bitmap has been manipulated.
     */
    private void setBitmap(Bitmap bitmap, boolean clearFull) {
        if (mBitmap == null || !mBitmap.equals(bitmap)) {

            clearImage(clearFull);

            mBitmap = bitmap;
            mImageView.setImageBitmap(mBitmap);
            if (mCropOverlayView != null) {
                mCropOverlayView.resetCropOverlayView();
                setCropOverlayVisibility();
            }
        }
    }

    /**
     * Clear the current image set for cropping.<br>
     * Full clear will also clear the data of the set image like Uri or Resource id while partial clear
     * will only clear the bitmap and recycle if required.
     */
    private void clearImage(boolean full) {

        // if we allocated the bitmap, release it as fast as possible
        if (mBitmap != null && (mImageResource > 0 || mLoadedImageUri != null)) {
            mBitmap.recycle();
        }
        mBitmap = null;

        if (full) {
            // clean the loaded image flags for new image
            mImageResource = 0;
            mLoadedImageUri = null;
            mLoadedSampleSize = 1;
            mDegreesRotated = 0;

            mImageView.setImageBitmap(null);

            setCropOverlayVisibility();
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putParcelable("LOADED_IMAGE_URI", mLoadedImageUri);
        bundle.putInt("LOADED_IMAGE_RESOURCE", mImageResource);
        if (mLoadedImageUri == null && mImageResource < 1) {
            bundle.putParcelable("SET_BITMAP", mBitmap);
        }
        if (mBitmapLoadingWorkerTask != null) {
            BitmapLoadingWorkerTask task = mBitmapLoadingWorkerTask.get();
            if (task != null) {
                bundle.putParcelable("LOADING_IMAGE_URI", task.getUri());
            }
        }
        bundle.putInt("DEGREES_ROTATED", mDegreesRotated);
        bundle.putParcelable("INITIAL_CROP_RECT", mCropOverlayView.getInitialCropWindowRect());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;

            Bitmap bitmap = null;
            Uri uri = bundle.getParcelable("LOADED_IMAGE_URI");
            if (uri != null) {
                setImageUriAsync(uri, bundle.getInt("DEGREES_ROTATED"));
            } else {
                int resId = bundle.getInt("LOADED_IMAGE_RESOURCE");
                if (resId > 0) {
                    setImageResource(resId);
                } else {
                    bitmap = bundle.getParcelable("SET_BITMAP");
                    if (bitmap != null) {
                        setBitmap(bitmap, true);
                    } else {
                        uri = bundle.getParcelable("LOADING_IMAGE_URI");
                        if (uri != null) {
                            setImageUriAsync(uri);
                        }
                    }
                }
            }

            mDegreesRotated = bundle.getInt("DEGREES_ROTATED");
            if (mBitmap != null && bitmap == null) {
                // Fixes the rotation of the image when we reloaded it.
                int tmpRotated = mDegreesRotated;
                rotateImage(mDegreesRotated);
                mDegreesRotated = tmpRotated;
            }

            mCropOverlayView.setInitialCropWindowRect((Rect) bundle.getParcelable("INITIAL_CROP_RECT"));

            super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        if (mBitmap != null) {
            Rect bitmapRect = BitmapUtils.getBitmapRect(mBitmap, this, mImageView.getScaleType());
            updateBitmapRect(bitmapRect);
        } else {
            updateBitmapRect(CropDefaults.EMPTY_RECT);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (mBitmap != null) {

            // Bypasses a baffling bug when used within a ScrollView, where heightSize is set to 0.
            if (heightSize == 0) {
                heightSize = mBitmap.getHeight();
            }

            int desiredWidth;
            int desiredHeight;

            double viewToBitmapWidthRatio = Double.POSITIVE_INFINITY;
            double viewToBitmapHeightRatio = Double.POSITIVE_INFINITY;

            // Checks if either width or height needs to be fixed
            if (widthSize < mBitmap.getWidth()) {
                viewToBitmapWidthRatio = (double) widthSize / (double) mBitmap.getWidth();
            }
            if (heightSize < mBitmap.getHeight()) {
                viewToBitmapHeightRatio = (double) heightSize / (double) mBitmap.getHeight();
            }

            // If either needs to be fixed, choose smallest ratio and calculate from there
            if (viewToBitmapWidthRatio != Double.POSITIVE_INFINITY || viewToBitmapHeightRatio != Double.POSITIVE_INFINITY) {
                if (viewToBitmapWidthRatio <= viewToBitmapHeightRatio) {
                    desiredWidth = widthSize;
                    desiredHeight = (int) (mBitmap.getHeight() * viewToBitmapWidthRatio);
                } else {
                    desiredHeight = heightSize;
                    desiredWidth = (int) (mBitmap.getWidth() * viewToBitmapHeightRatio);
                }
            } else {
                // Otherwise, the picture is within frame layout bounds. Desired width is simply picture size
                desiredWidth = mBitmap.getWidth();
                desiredHeight = mBitmap.getHeight();
            }

            int width = getOnMeasureSpec(widthMode, widthSize, desiredWidth);
            int height = getOnMeasureSpec(heightMode, heightSize, desiredHeight);

            mLayoutWidth = width;
            mLayoutHeight = height;

            Rect bitmapRect = BitmapUtils.getBitmapRect(mBitmap.getWidth(),
                    mBitmap.getHeight(),
                    mLayoutWidth,
                    mLayoutHeight,
                    mImageView.getScaleType());
            updateBitmapRect(bitmapRect);

            // MUST CALL THIS
            setMeasuredDimension(mLayoutWidth, mLayoutHeight);

        } else {
            updateBitmapRect(CropDefaults.EMPTY_RECT);
            setMeasuredDimension(widthSize, heightSize);
        }
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        super.onLayout(changed, l, t, r, b);

        if (mLayoutWidth > 0 && mLayoutHeight > 0) {
            // Gets original parameters, and creates the new parameters
            ViewGroup.LayoutParams origParams = this.getLayoutParams();
            origParams.width = mLayoutWidth;
            origParams.height = mLayoutHeight;
            setLayoutParams(origParams);
        }
    }

    /**
     * Determines the specs for the onMeasure function. Calculates the width or height
     * depending on the mode.
     *
     * @param measureSpecMode The mode of the measured width or height.
     * @param measureSpecSize The size of the measured width or height.
     * @param desiredSize The desired size of the measured width or height.
     * @return The final size of the width or height.
     */
    private static int getOnMeasureSpec(int measureSpecMode, int measureSpecSize, int desiredSize) {

        // Measure Width
        int spec;
        if (measureSpecMode == MeasureSpec.EXACTLY) {
            // Must be this size
            spec = measureSpecSize;
        } else if (measureSpecMode == MeasureSpec.AT_MOST) {
            // Can't be bigger than...; match_parent value
            spec = Math.min(desiredSize, measureSpecSize);
        } else {
            // Be whatever you want; wrap_content
            spec = desiredSize;
        }

        return spec;
    }

    /**
     * Set visibility of crop overlay to hide it when there is no image or specificly set by client.
     */
    private void setCropOverlayVisibility() {
        if (mCropOverlayView != null) {
            mCropOverlayView.setVisibility(mShowCropOverlay && mBitmap != null ? VISIBLE : INVISIBLE);
        }
    }

    /**
     * Set visibility of progress bar when async loading/cropping is in process and show is enabled.
     */
    private void setProgressBarVisibility() {
        boolean visible = mShowProgressBar &&
                (mBitmap == null && mBitmapLoadingWorkerTask != null || mBitmapCroppingWorkerTask != null);
        mProgressBar.setVisibility(visible ? VISIBLE : INVISIBLE);
    }

    /**
     * Update the scale factor between the actual image bitmap and the shown image.<br>
     */
    private void updateBitmapRect(Rect bitmapRect) {
        if (mBitmap != null && bitmapRect.width() > 0 && bitmapRect.height() > 0) {

            // Get the scale factor between the actual Bitmap dimensions and the displayed dimensions for width/height.
            float scaleFactorWidth = mBitmap.getWidth() * mLoadedSampleSize / (float) bitmapRect.width();
            float scaleFactorHeight = mBitmap.getHeight() * mLoadedSampleSize / (float) bitmapRect.height();
            mCropOverlayView.setScaleFactor(scaleFactorWidth, scaleFactorHeight);
        }

        // set the bitmap rectangle and update the crop window after scale factor is set
        mCropOverlayView.setBitmapRect(bitmapRect);
    }

    /**
     * Get the scale factor between the actual Bitmap dimensions and the displayed dimensions.
     */
    private Pair<Float, Float> getScaleFactorWidth(Rect displayedImageRect) {
        float actualImageWidth = mBitmap.getWidth();
        float displayedImageWidth = displayedImageRect.width();
        float scaleFactorWidth = actualImageWidth / displayedImageWidth;

        float actualImageHeight = mBitmap.getHeight();
        float displayedImageHeight = displayedImageRect.height();
        float scaleFactorHeight = actualImageHeight / displayedImageHeight;

        return Pair.create(scaleFactorWidth, scaleFactorHeight);
    }

    //endregion

    //region: Inner class: CropShape

    /**
     * The possible cropping area shape.
     */
    public enum CropShape {
        RECTANGLE,
        OVAL
    }
    //endregion

    //region: Inner class: Guidelines

    /**
     * The possible guidelines showing types.
     */
    public enum Guidelines {
        /**
         * Never show
         */
        OFF,

        /**
         * Show when crop move action is live
         */
        ON_TOUCH,

        /**
         * Always show
         */
        ON
    }
    //endregion

    //region: Inner class: OnSetImageUriCompleteListener

    /**
     * Interface definition for a callback to be invoked when image async loading is complete.
     */
    public interface OnSetImageUriCompleteListener {

        /**
         * Called when a crop image view has completed loading image for cropping.<br>
         * If loading failed error parameter will contain the error.
         *
         * @param view The crop image view that loading of image was complete.
         * @param uri the URI of the image that was loading
         * @param error if error occurred during loading will contain the error, otherwise null.
         */
        void onSetImageUriComplete(CropImageView view, Uri uri, Exception error);
    }
    //endregion

    //region: Inner class: OnGetCroppedImageCompleteListener

    /**
     * Interface definition for a callback to be invoked when image async cropping is complete.
     */
    public interface OnGetCroppedImageCompleteListener {

        /**
         * Called when a crop image view has completed loading image for cropping.<br>
         * If loading failed error parameter will contain the error.
         *
         * @param view The crop image view that cropping of image was complete.
         * @param bitmap the cropped image bitmap (null if failed)
         * @param error if error occurred during cropping will contain the error, otherwise null.
         */
        void onGetCroppedImageComplete(CropImageView view, Bitmap bitmap, Exception error);
    }
    //endregion
}
