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
import android.net.Uri;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Pair;

import java.lang.ref.WeakReference;

/**
 * Task to load bitmap asynchronously from the UI thread.
 */
final class BitmapLoadingWorkerTask extends AsyncTask<Void, Void, BitmapLoadingWorkerTask.Result> {

    //region: Fields and Consts

    /**
     * Use a WeakReference to ensure the ImageView can be garbage collected
     */
    private final WeakReference<CropImageView> mCropImageViewReference;

    /**
     * The Android URI of the image to load
     */
    private final Uri mUri;

    /**
     * Optional: if given use this rotation and not by exif
     */
    private final Integer mPreSetRotation;

    /**
     * The context of the crop image view widget used for loading of bitmap by Android URI
     */
    private final Context mContext;

    /**
     * required width of the cropping image after density adjustment
     */
    private final int mWidth;

    /**
     * required height of the cropping image after density adjustment
     */
    private final int mHeight;
    //endregion

    public BitmapLoadingWorkerTask(CropImageView cropImageView, Uri uri, Integer preSetRotation) {
        mUri = uri;
        mPreSetRotation = preSetRotation;
        mCropImageViewReference = new WeakReference<>(cropImageView);

        mContext = cropImageView.getContext();

        DisplayMetrics metrics = cropImageView.getResources().getDisplayMetrics();
        double densityAdj = metrics.density > 1 ? 1 / metrics.density : 1;
        mWidth = (int) (metrics.widthPixels * densityAdj);
        mHeight = (int) (metrics.heightPixels * densityAdj);
    }

    /**
     * The Android URI that this task is currently loading.
     */
    public Uri getUri() {
        return mUri;
    }

    /**
     * Decode image in background.
     *
     * @param params ignored
     * @return the decoded bitmap data
     */
    @Override
    protected Result doInBackground(Void... params) {
        try {
            if (!isCancelled()) {

                BitmapUtils.DecodeBitmapResult decodeResult =
                        BitmapUtils.decodeSampledBitmap(mContext, mUri, mWidth, mHeight);

                if (!isCancelled()) {

                    BitmapUtils.RotateBitmapResult rotateResult = mPreSetRotation != null
                            ? BitmapUtils.rotateBitmapResult(decodeResult.bitmap, mPreSetRotation)
                            : BitmapUtils.rotateBitmapByExif(mContext, decodeResult.bitmap, mUri);

                    Pair<Integer, Integer> sourceImageDimens = BitmapUtils.getImageDimensions(mContext, mUri);
                    if(mPresetRotation == null) {
                        // In this case, rotateResult.degrees is due to Exif data.
                        if(rotateResult.degrees == 90 || rotateResult.degrees == 270)
                            sourceImageDimens = new Pair(sourceImageDimens.second, sourceImageDimens.first);
                    }

                    return new Result(mUri, rotateResult.bitmap, sourceImageDimens, decodeResult.sampleSize, rotateResult.degrees);
                }
            }
            return null;
        } catch (Exception e) {
            return new Result(mUri, e);
        }
    }

    /**
     * Once complete, see if ImageView is still around and set bitmap.
     *
     * @param result the result of bitmap loading
     */
    @Override
    protected void onPostExecute(Result result) {
        if (result != null) {
            boolean completeCalled = false;
            if (!isCancelled()) {
                CropImageView cropImageView = mCropImageViewReference.get();
                if (cropImageView != null) {
                    completeCalled = true;
                    cropImageView.onSetImageUriAsyncComplete(result);
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
     * The result of BitmapLoadingWorkerTask async loading.
     */
    public static final class Result {

        /**
         * The Android URI of the image to load
         */
        public final Uri uri;

        /**
         * The loaded bitmap
         */
        public final Bitmap bitmap;

        /**
         * The dimensions of source bitmap
         */
        public final Pair<Integer, Integer> sourceImageDimensions;

        /**
         * The sample size used to load the given bitmap
         */
        public final int loadSampleSize;

        /**
         * The degrees the image was rotated
         */
        public final int degreesRotated;

        /**
         * The error that occurred during async bitmap loading.
         */
        public final Exception error;

        Result(Uri uri, Bitmap bitmap, Pair<Integer, Integer> sourceImageDimens, int loadSampleSize, int degreesRotated) {
            this.uri = uri;
            this.bitmap = bitmap;
            this.sourceImageDimensions = sourceImageDimens;
            this.loadSampleSize = loadSampleSize;
            this.degreesRotated = degreesRotated;
            this.error = null;
        }

        Result(Uri uri, Exception error) {
            this.uri = uri;
            this.bitmap = null;
            this.sourceImageDimensions = null;
            this.loadSampleSize = 0;
            this.degreesRotated = 0;
            this.error = error;
        }
    }
    //endregion
}
