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
import android.util.Log;

import com.theartofdev.edmodo.cropper.util.ImageViewUtil;

import java.lang.ref.WeakReference;

/**
 * Task to load bitmap asynchronously from the UI thread.
 */
class BitmapWorkerTask extends AsyncTask<Void, Void, BitmapWorkerTask.BitmapWorkerTaskResult> {

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
     * The context of the crop image view widget used for loading of bitmap by Android URI
     */
    private final Context context;

    /**
     * required width of the cropping image after density adjustment
     */
    private final int mWidth;

    /**
     * required height of the cropping image after density adjustment
     */
    private final int mHeight;
    //endregion

    public BitmapWorkerTask(CropImageView cropImageView, Uri uri) {
        mUri = uri;
        mCropImageViewReference = new WeakReference<>(cropImageView);

        context = cropImageView.getContext();

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
    protected BitmapWorkerTask.BitmapWorkerTaskResult doInBackground(Void... params) {
        try {
            Log.w("CIW", "doInBackground...");

            try {
                Thread.sleep(6000, 0);
            } catch (InterruptedException ignored) {
            }

            if (true)
                throw new RuntimeException("test");

            ImageViewUtil.DecodeBitmapResult decodeResult =
                    ImageViewUtil.decodeSampledBitmap(context, mUri, mWidth, mHeight);

            ImageViewUtil.RotateBitmapResult rotateResult =
                    ImageViewUtil.rotateBitmapByExif(context, decodeResult.bitmap, mUri);

            return new BitmapWorkerTaskResult(mUri, rotateResult.bitmap, decodeResult.sampleSize, rotateResult.degrees);
        } catch (Exception e) {
            return new BitmapWorkerTaskResult(mUri, e);
        }
    }

    /**
     * Once complete, see if ImageView is still around and set bitmap.
     *
     * @param result the result of bitmap loading
     */
    @Override
    protected void onPostExecute(BitmapWorkerTask.BitmapWorkerTaskResult result) {
        Log.w("CIW", "Post execute...");
        if (!isCancelled() && result != null) {
            CropImageView cropImageView = mCropImageViewReference.get();
            if (cropImageView != null) {
                cropImageView.onSetImageUriAsyncComplete(result);
            }
        }
    }

    //region: Inner class: BitmapWorkerTaskResult

    /**
     * The result of BitmapWorkerTask async loading.
     */
    public static final class BitmapWorkerTaskResult {

        /**
         * The Android URI of the image to load
         */
        public final Uri uri;

        /**
         * The loaded bitmap
         */
        public final Bitmap bitmap;

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

        BitmapWorkerTaskResult(Uri uri, Bitmap bitmap, int loadSampleSize, int degreesRotated) {
            this.uri = uri;
            this.bitmap = bitmap;
            this.loadSampleSize = loadSampleSize;
            this.degreesRotated = degreesRotated;
            this.error = null;
        }

        public BitmapWorkerTaskResult(Uri uri, Exception error) {
            this.uri = uri;
            this.bitmap = null;
            this.loadSampleSize = 0;
            this.degreesRotated = 0;
            this.error = error;
        }
    }
    //endregion
}
