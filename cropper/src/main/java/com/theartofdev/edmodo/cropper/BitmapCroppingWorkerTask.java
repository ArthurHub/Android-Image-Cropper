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
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

/**
 * Task to crop bitmap asynchronously from the UI thread.
 */
final class BitmapCroppingWorkerTask extends AsyncTask<Void, Void, BitmapCroppingWorkerTask.Result> {

    //region: Fields and Consts

    /**
     * Use a WeakReference to ensure the ImageView can be garbage collected
     */
    private final WeakReference<CropImageView> mCropImageViewReference;

    /**
     * the bitmap to crop
     */
    private final Bitmap mBitmap;

    /**
     * The Android URI of the image to load
     */
    private final Uri mUri;

    /**
     * The context of the crop image view widget used for loading of bitmap by Android URI
     */
    private final Context mContext;

    /**
     * Required cropping rectangle
     */
    private final Rect mRect;

    /**
     * The shape to crop the image
     */
    private final CropImageView.CropShape mCropShape;

    /**
     * Degrees the image was rotated after loading
     */
    private final int mDegreesRotated;

    /**
     * required width of the cropping image
     */
    private final int mReqWidth;

    /**
     * required height of the cropping image
     */
    private final int mReqHeight;
    //endregion

    public BitmapCroppingWorkerTask(CropImageView cropImageView, Bitmap bitmap, Rect rect, CropImageView.CropShape cropShape) {
        mCropImageViewReference = new WeakReference<>(cropImageView);
        mContext = cropImageView.getContext();
        mBitmap = bitmap;
        mRect = rect;
        mCropShape = cropShape;
        mUri = null;
        mDegreesRotated = 0;
        mReqWidth = 0;
        mReqHeight = 0;
    }

    public BitmapCroppingWorkerTask(CropImageView cropImageView, Uri uri, Rect rect, CropImageView.CropShape cropShape, int degreesRotated, int reqWidth, int reqHeight) {
        mCropImageViewReference = new WeakReference<>(cropImageView);
        mContext = cropImageView.getContext();
        mUri = uri;
        mRect = rect;
        mCropShape = cropShape;
        mDegreesRotated = degreesRotated;
        mReqWidth = reqWidth;
        mReqHeight = reqHeight;
        mBitmap = null;
    }

    /**
     * The Android URI that this task is currently loading.
     */
    public Uri getUri() {
        return mUri;
    }

    /**
     * Crop image in background.
     *
     * @param params ignored
     * @return the decoded bitmap data
     */
    @Override
    protected BitmapCroppingWorkerTask.Result doInBackground(Void... params) {
        try {
            if (!isCancelled()) {

                Bitmap bitmap = null;
                if (mUri != null) {
                    bitmap = BitmapUtils.cropBitmap(
                            mContext,
                            mUri,
                            mRect,
                            mDegreesRotated,
                            mReqWidth,
                            mReqHeight);
                } else if (mBitmap != null) {
                    bitmap = BitmapUtils.cropBitmap(mBitmap, mRect);
                }
                if (bitmap != null && mCropShape == CropImageView.CropShape.OVAL) {
                    bitmap = BitmapUtils.toOvalBitmap(bitmap);
                }
                return new Result(bitmap);
            }
            return null;
        } catch (Exception e) {
            return new Result(e);
        }
    }

    /**
     * Once complete, see if ImageView is still around and set bitmap.
     *
     * @param result the result of bitmap cropping
     */
    @Override
    protected void onPostExecute(Result result) {
        if (result != null) {
            boolean completeCalled = false;
            if (!isCancelled()) {
                CropImageView cropImageView = mCropImageViewReference.get();
                if (cropImageView != null) {
                    completeCalled = true;
                    cropImageView.onGetImageCroppingAsyncComplete(result);
                }
            }
            if (!completeCalled && result.bitmap != null) {
                // fast release of unused bitmap
                result.bitmap.recycle();
            }
        }
    }

    //region: Inner class: Result

    /**
     * The result of BitmapCroppingWorkerTask async loading.
     */
    public static final class Result {

        /**
         * The cropped bitmap
         */
        public final Bitmap bitmap;

        /**
         * The error that occurred during async bitmap cropping.
         */
        public final Exception error;

        Result(Bitmap bitmap) {
            this.bitmap = bitmap;
            this.error = null;
        }

        Result(Exception error) {
            this.bitmap = null;
            this.error = error;
        }
    }
    //endregion
}
