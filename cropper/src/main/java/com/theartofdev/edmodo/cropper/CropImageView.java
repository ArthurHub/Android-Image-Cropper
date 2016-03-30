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
public class CropImageView extends FrameLayout implements CropOverlayView.CropWindowChangeListener {

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
     * The matrix used to transform the cripping image in the image view
     */
    private final Matrix mImageMatrix = new Matrix();

    /**
     * Progress bar widget to show progress bar on async image loading and cropping.
     */
    private final ProgressBar mProgressBar;

    /**
     * Rectengale used in image matrix transformation calculation (reusing rect instance)
     */
    private final RectF mImageRect = new RectF();

    private Bitmap mBitmap;

    private int mDegreesRotated;

    private int mLayoutWidth;

    private int mLayoutHeight;

    private int mImageResource;

    /**
     * The initial scale type of the image in the crop image view
     */
    private ImageView.ScaleType mScaleType;

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
     * The max zoom allowed during cropping
     */
    private float mMaxZoom = 4;

    /**
     * the zoom step to do for every zoom required
     */
    private float mZoomStep = 0.4f;

    /**
     * The current zoom level to to scale the cropping image
     */
    private float mZoom = 1;

    /**
     * The X offset that the cropping image was translated after zooming
     */
    private float mZoomOffsetX;

    /**
     * The Y offset that the cropping image was translated after zooming
     */
    private float mZoomOffsetY;

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
                mScaleType = CropDefaults.VALID_SCALE_TYPES[ta.getInt(R.styleable.CropImageView_cropScaleType, CropDefaults.DEFAULT_SCALE_TYPE_INDEX)];
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
        mImageView.setScaleType(ImageView.ScaleType.MATRIX);

        mCropOverlayView = (CropOverlayView) v.findViewById(R.id.CropOverlayView);
        mCropOverlayView.setCropWindowChangeListener(this);
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
        return mScaleType;
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
            mScaleType = scaleType;
            requestLayout();
            mCropOverlayView.invalidate();
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
     * Image to crop must be first set before invoking this, for async - after complete callback.
     *
     * @param rect window rectangle (position and size) relative to source bitmap
     */
    public void setCropRect(Rect rect) {
        mCropOverlayView.setInitialCropWindowRect(rect);
    }

    /**
     * Gets the crop window's position relative to the source Bitmap (not the image
     * displayed in the CropImageView) using the original image rotation.
     *
     * @return a Rect instance containing cropped area boundaries of the source Bitmap
     */
    public Rect getCropRect() {
        if (mBitmap != null) {

            // get the points of the crop rectangle adjusted to source bitmap
            float[] points = getCropPoints();

            // get the rectangle for the points (it may be larger than original if rotation is not stright)
            int orgWidth = mBitmap.getWidth() * mLoadedSampleSize;
            int orgHeight = mBitmap.getHeight() * mLoadedSampleSize;
            return BitmapUtils.getRectFromPoints(points, orgWidth, orgHeight);
        } else {
            return null;
        }
    }

    /**
     * Gets the 4 points of crop window's position relative to the source Bitmap (not the image
     * displayed in the CropImageView) using the original image rotation.<br>
     * Note: the 4 points may not be a rectangle if the image was rotates to NOT stright angle (!= 90/180/270).
     *
     * @return 4 points (x0,y0,x1,y1,x2,y2,x3,y3) of cropped area boundaries
     */
    public float[] getCropPoints() {

        // Get crop window position relative to the displayed image.
        RectF cropWindowRect = mCropOverlayView.getCropWindowRect();

        float[] points = new float[]{
                cropWindowRect.left,
                cropWindowRect.top,
                cropWindowRect.right,
                cropWindowRect.top,
                cropWindowRect.right,
                cropWindowRect.bottom,
                cropWindowRect.left,
                cropWindowRect.bottom
        };

        Matrix invertMatrix = new Matrix();
        mImageMatrix.invert(invertMatrix);
        invertMatrix.mapPoints(points);

        for (int i = 0; i < points.length; i++) {
            points[i] *= mLoadedSampleSize;
        }

        return points;
    }

    /**
     * Gets the cropped image based on the current crop window.
     *
     * @return a new Bitmap representing the cropped image
     */
    public Bitmap getCroppedImage() {
        return getCroppedImage(CropShape.RECTANGLE, 0, 0);
    }

    /**
     * Gets the cropped image based on the current crop window.
     *
     * @return a new Bitmap representing the cropped image
     */
    public Bitmap getCroppedImage(CropShape cropShape) {
        return getCroppedImage(cropShape, 0, 0);
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
    public Bitmap getCroppedImage(CropShape cropShape, int reqWidth, int reqHeight) {
        Bitmap croppedBitmap = null;
        if (mBitmap != null) {
            if (mLoadedImageUri != null && mLoadedSampleSize > 1) {
                int orgWidth = mBitmap.getWidth() * mLoadedSampleSize;
                int orgHeight = mBitmap.getHeight() * mLoadedSampleSize;
                croppedBitmap = BitmapUtils.cropBitmap(getContext(), mLoadedImageUri, getCropPoints(), mDegreesRotated, orgWidth, orgHeight, reqWidth, reqHeight);
            } else {
                croppedBitmap = BitmapUtils.cropBitmap(mBitmap, getCropPoints(), mDegreesRotated);
            }
        }

        if (cropShape == CropShape.OVAL) {
            croppedBitmap = BitmapUtils.toOvalBitmap(croppedBitmap);
        }

        return croppedBitmap;
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

        int orgWidth = mBitmap.getWidth() * mLoadedSampleSize;
        int orgHeight = mBitmap.getHeight() * mLoadedSampleSize;
        mBitmapCroppingWorkerTask = mLoadedImageUri != null && mLoadedSampleSize > 1
                ? new WeakReference<>(new BitmapCroppingWorkerTask(this, mLoadedImageUri, getCropPoints(), cropShape, mDegreesRotated, orgWidth, orgHeight, reqWidth, reqHeight))
                : new WeakReference<>(new BitmapCroppingWorkerTask(this, mBitmap, getCropPoints(), cropShape, mDegreesRotated));
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
            mDegreesRotated += degrees;
            mDegreesRotated = mDegreesRotated % 360;
            // TODO:a. handle apply matrix directly
            requestLayout();
            mCropOverlayView.invalidate();
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

            setMeasuredDimension(mLayoutWidth, mLayoutHeight);

        } else {
            setMeasuredDimension(widthSize, heightSize);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        super.onLayout(changed, l, t, r, b);

        if (mLayoutWidth > 0 && mLayoutHeight > 0) {
            // Gets original parameters, and creates the new parameters
            ViewGroup.LayoutParams origParams = this.getLayoutParams();
            origParams.width = mLayoutWidth;
            origParams.height = mLayoutHeight;
            setLayoutParams(origParams);

            if (mBitmap != null) {
                RectF transformedRect = applyImageMatrix(r - l, b - t, false);
                updateBitmapRect(transformedRect);
            } else {
                updateBitmapRect(CropDefaults.EMPTY_RECT_F);
            }
        } else {
            updateBitmapRect(CropDefaults.EMPTY_RECT_F);
        }
    }

    @Override
    public void onCropWindowChanged(boolean inProgress) {

        int width = mImageView.getWidth();
        int height = mImageView.getHeight();
        RectF cropRect = mCropOverlayView.getCropWindowRect();

        if (inProgress) {
            if (cropRect.left < 0 || cropRect.top < 0 || cropRect.right > width || cropRect.bottom > height) {
                RectF transformedRect = applyImageMatrix(width, height, false);
                updateBitmapRect(transformedRect);
            }
        } else {

            float oldZoom = mZoom;
            for (int i = 0; i < 8; i++) {

                float newZoom = 0;
                if (mZoom < mMaxZoom && cropRect.width() < width * 0.5 && cropRect.height() < height * 0.5f) {
                    newZoom = Math.min(mMaxZoom, mZoom + mZoomStep);
                } else if (mZoom > 1 && (cropRect.width() > width * 0.8 || cropRect.height() > height * 0.8f)) {
                    newZoom = Math.max(1, mZoom - mZoomStep * 1.8f);
                } else if (mZoom > 1 && (cropRect.width() > width * 0.7 || cropRect.height() > height * 0.7f)) {
                    newZoom = Math.max(1, mZoom - mZoomStep * 1.2f);
                } else {
                    break;
                }

                if (newZoom > mMaxZoom - .25f) {
                    newZoom = mMaxZoom;
                } else if (newZoom < 1.3f) {
                    newZoom = 1;
                }

                if (newZoom > 0) {
                    updateCropRectByZoomChange(cropRect, width, height, newZoom / mZoom);
                    mZoom = newZoom;
                }
            }

            if (mZoom != oldZoom) {
                mCropOverlayView.setCropWindowRect(cropRect);
                RectF transformedRect = applyImageMatrix(width, height, true);
                updateBitmapRect(transformedRect);
            }
        }
    }

    /**
     * Adjust the given crop window rectangle by the change in zoom, need to update the location and size
     * of the crop rectangle to cover the same area in new zoom level.
     */
    private void updateCropRectByZoomChange(RectF cropRect, int width, int height, float zoomChange) {
        float xCenterOffset = width / 2 - cropRect.centerX();
        float yCenterOffset = height / 2 - cropRect.centerY();
        cropRect.offset(xCenterOffset - xCenterOffset * zoomChange, yCenterOffset - yCenterOffset * zoomChange);
        cropRect.inset((cropRect.width() - cropRect.width() * zoomChange) / 2f, (cropRect.height() - cropRect.height() * zoomChange) / 2f);
    }

    /**
     * Apply matrix to handle the image inside the image view.
     *
     * @param width the width of the image view
     * @param height the height of the image view
     */
    private RectF applyImageMatrix(float width, float height, boolean center) {

        mImageMatrix.reset();
        mImageRect.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());

        // move the image to the center of the image view first so we can manipulate it from there
        mImageMatrix.postTranslate((width - mImageRect.width()) / 2, (height - mImageRect.height()) / 2);
        mapImageRectangleByImageMatrix(mImageRect);

        // rotate the image the required degrees from center of image
        if (mDegreesRotated > 0) {
            mImageMatrix.postRotate(mDegreesRotated, mImageRect.centerX(), mImageRect.centerY());
            mapImageRectangleByImageMatrix(mImageRect);
        }

        // scale the image to the image view, image rect transformed to know new width/height
        float scale = Math.min(width / mImageRect.width(), height / mImageRect.height());
        if (mScaleType == ImageView.ScaleType.FIT_CENTER || scale < 1) {
            mImageMatrix.postScale(scale, scale, mImageRect.centerX(), mImageRect.centerY());
            mapImageRectangleByImageMatrix(mImageRect);
        }

        if (mZoom > 1) {

            // scale by the current zoom level
            mImageMatrix.postScale(mZoom, mZoom, mImageRect.centerX(), mImageRect.centerY());
            mapImageRectangleByImageMatrix(mImageRect);

            RectF cropRect = mCropOverlayView.getCropWindowRect();

            // reset the crop window offset so we can update it to required value
            cropRect.offset(-mZoomOffsetX * mZoom, -mZoomOffsetY * mZoom);

            if (center) {
                // set the zoomed area to be as to the center of cropping window as possible
                mZoomOffsetX = Math.max(Math.min(width / 2 - cropRect.centerX(), -mImageRect.left), mImageView.getWidth() - mImageRect.right) / mZoom;
                mZoomOffsetY = Math.max(Math.min(height / 2 - cropRect.centerY(), -mImageRect.top), mImageView.getHeight() - mImageRect.bottom) / mZoom;
            } else {
                // adjust the zoomed area so the crop window rectangle will be inside the area in case it was moved outside
                mZoomOffsetX = Math.min(Math.max(mZoomOffsetX * mZoom, -cropRect.left), -cropRect.right + width) / mZoom;
                mZoomOffsetY = Math.min(Math.max(mZoomOffsetY * mZoom, -cropRect.top), -cropRect.bottom + height) / mZoom;
            }

            // apply to zoom offset translate and update the crop rectangle to offset correctly
            mImageMatrix.postTranslate(mZoomOffsetX * mZoom, mZoomOffsetY * mZoom);
            cropRect.offset(mZoomOffsetX * mZoom, mZoomOffsetY * mZoom);
            mCropOverlayView.setCropWindowRect(cropRect);
            mapImageRectangleByImageMatrix(mImageRect);

        } else if (mZoomOffsetX != 0 || mZoomOffsetY != 0) {

            // if fully zoomed out, need to clear the zoom offset
            RectF cropRect = mCropOverlayView.getCropWindowRect();
            cropRect.offset(-mZoomOffsetX * mZoom, -mZoomOffsetY * mZoom);
            mCropOverlayView.setCropWindowRect(cropRect);
            mZoomOffsetX = mZoomOffsetY = 0;
        }

        // set matrix to apply
        mImageView.setImageMatrix(mImageMatrix);

        return mImageRect;
    }

    /**
     * Adjust the given image rectangle by image transformation matrix to know the final rectangle of the image.<br>
     * To get the proper rectangle it must be first reset to orginal image rectangle.
     */
    private void mapImageRectangleByImageMatrix(RectF imgRect) {
        imgRect.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        mImageMatrix.mapRect(imgRect);
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
    private void updateBitmapRect(RectF bitmapRect) {
        if (mBitmap != null && bitmapRect.width() > 0 && bitmapRect.height() > 0) {

            // Get the scale factor between the actual Bitmap dimensions and the displayed dimensions for width/height.
            float scaleFactorWidth = mBitmap.getWidth() * mLoadedSampleSize / bitmapRect.width();
            float scaleFactorHeight = mBitmap.getHeight() * mLoadedSampleSize / bitmapRect.height();
            mCropOverlayView.setCropWindowLimits(mImageView.getWidth(), mImageView.getHeight(), scaleFactorWidth, scaleFactorHeight);
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
