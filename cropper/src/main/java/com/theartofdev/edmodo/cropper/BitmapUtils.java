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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class that deals with operations with an ImageView.
 */
final class BitmapUtils {

    /**
     * Reusable rectengale for general internal usage
     */
    static final RectF RECT = new RectF();

    /**
     * Rotate the given bitmap by the given degrees.<br>
     * New bitmap is created and the old one is recycled.
     */
    public static RotateBitmapResult rotateBitmap(Bitmap bitmap, int degrees) {
        return new RotateBitmapResult(rotateBitmapInt(bitmap, degrees), degrees);
    }

    /**
     * Rotate the given image by reading the Exif value of the image (uri).<br>
     * If no rotation is required the image will not be rotated.<br>
     * New bitmap is created and the old one is recycled.
     */
    public static RotateBitmapResult rotateBitmapByExif(Bitmap bitmap, Context context, Uri uri) {
        try {
            File file = getFileFromUri(context, uri);
            if (file.exists()) {
                ExifInterface ei = new ExifInterface(file.getAbsolutePath());
                return rotateBitmapByExif(bitmap, ei);
            }
        } catch (Exception ignored) {
        }
        return new RotateBitmapResult(bitmap, 0);
    }

    /**
     * Rotate the given image by given Exif value.<br>
     * If no rotation is required the image will not be rotated.<br>
     * New bitmap is created and the old one is recycled.
     */
    public static RotateBitmapResult rotateBitmapByExif(Bitmap bitmap, ExifInterface exif) {
        int degrees;
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                degrees = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degrees = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degrees = 270;
                break;
            default:
                degrees = 0;
                break;
        }
        Bitmap rotatedBitmap = rotateBitmapInt(bitmap, degrees);
        return new RotateBitmapResult(rotatedBitmap, degrees);
    }

    /**
     * Decode bitmap from stream using sampling to get bitmap with the requested limit.
     */
    public static DecodeBitmapResult decodeSampledBitmap(Context context, Uri uri, int reqWidth, int reqHeight) {

        InputStream stream = null;
        try {
            ContentResolver resolver = context.getContentResolver();
            stream = resolver.openInputStream(uri);

            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(stream, CropDefaults.EMPTY_RECT, options);
            options.inJustDecodeBounds = false;

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            closeSafe(stream);
            stream = resolver.openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(stream, CropDefaults.EMPTY_RECT, options);

            return new DecodeBitmapResult(bitmap, options.inSampleSize);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load sampled bitmap: " + uri, e);
        } finally {
            closeSafe(stream);
        }
    }

    /**
     * Crop image bitmap from given bitmap using the given points in the original bitmap and the given rotation.<br>
     * if the rotation is not 0,90,180 or 270 degrees then we must first crop a larger area of the image that
     * contains the requires rectangle, rotate and then crop again a sub rectangle.
     */
    public static Bitmap cropBitmap(Bitmap bitmap, float[] points,
                                    int degreesRotated, boolean fixAspectRatio, int aspectRatioX, int aspectRatioY) {

        // get the rectangle in original image that contains the required cropped area (larger for non rectengular crop)
        Rect rect = getRectFromPoints(points, bitmap.getWidth(), bitmap.getHeight(), fixAspectRatio, aspectRatioX, aspectRatioY);

        // crop and rotate the cropped image in one operation
        Matrix matrix = new Matrix();
        matrix.setRotate(degreesRotated, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        Bitmap result = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height(), matrix, true);

        // rotating by 0, 90, 180 or 270 degrees doesn't require extra cropping
        if (degreesRotated % 90 != 0) {

            // extra crop because non rectengular crop cannot be done directly on the image without rotating first
            result = cropForRotatedImage(result, points, rect, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY);
        }

        return result;
    }

    /**
     * Crop image bitmap from URI by decoding it with specific width and height to down-sample if required.
     */
    public static Bitmap cropBitmap(Context context, Uri loadedImageUri, float[] points,
                                    int degreesRotated, int orgWidth, int orgHeight, boolean fixAspectRatio,
                                    int aspectRatioX, int aspectRatioY, int reqWidth, int reqHeight) {

        // get the rectangle in original image that contains the required cropped area (larger for non rectengular crop)
        Rect rect = getRectFromPoints(points, orgWidth, orgHeight, fixAspectRatio, aspectRatioX, aspectRatioY);

        int width = reqWidth > 0 ? reqWidth : rect.width();
        int height = reqHeight > 0 ? reqHeight : rect.height();

        Bitmap result = null;
        try {
            // decode only the required image from URI, optionally sub-sampling if reqWidth/reqHeight is given.
            result = decodeSampledBitmapRegion(context, loadedImageUri, rect, width, height);
        } catch (Exception e) {
        }

        if (result != null) {
            // rotate the decoded region by the required amount
            result = rotateBitmapInt(result, degreesRotated);

            // rotating by 0, 90, 180 or 270 degrees doesn't require extra cropping
            if (degreesRotated % 90 != 0) {

                // extra crop because non rectengular crop cannot be done directly on the image without rotating first
                result = cropForRotatedImage(result, points, rect, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY);
            }
        } else {

            // failed to decode region, may be skia issue, try full decode and then crop
            InputStream stream = null;
            try {
                stream = context.getContentResolver().openInputStream(loadedImageUri);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = calculateInSampleSize(rect.width(), rect.height(), reqWidth, reqHeight);

                Bitmap fullBitmap = BitmapFactory.decodeStream(stream, CropDefaults.EMPTY_RECT, options);
                if (fullBitmap != null) {
                    result = cropBitmap(fullBitmap, points, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY);
                    fullBitmap.recycle();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load sampled bitmap: " + loadedImageUri, e);
            } finally {
                closeSafe(stream);
            }
        }

        return result;
    }

    /**
     * Create a new bitmap that has all pixels beyond the oval shape transparent.
     */
    public static Bitmap toOvalBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);

        int color = 0xff424242;
        Paint paint = new Paint();

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);

        RectF rect = new RectF(0, 0, width, height);
        canvas.drawOval(rect, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        bitmap.recycle();

        return output;
    }

    /**
     * Get a rectangle for the given 4 points (x0,y0,x1,y1,x2,y2,x3,y3) by finding the min/max 2 points that
     * contains the given 4 points and is a stright rectangle.
     */
    public static Rect getRectFromPoints(float[] points, int imageWidth, int imageHeight, boolean fixAspectRatio, int aspectRatioX, int aspectRatioY) {
        int left = Math.round(Math.max(0, Math.min(Math.min(Math.min(points[0], points[2]), points[4]), points[6])));
        int top = Math.round(Math.max(0, Math.min(Math.min(Math.min(points[1], points[3]), points[5]), points[7])));
        int right = Math.round(Math.min(imageWidth, Math.max(Math.max(Math.max(points[0], points[2]), points[4]), points[6])));
        int bottom = Math.round(Math.min(imageHeight, Math.max(Math.max(Math.max(points[1], points[3]), points[5]), points[7])));

        Rect rect = new Rect(left, top, right, bottom);
        if (fixAspectRatio) {
            fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY);
        }

        return rect;
    }

    /**
     * Fix the given rectangle if it doesn't confirm to aspect ration rule.<br>
     * Make sure that width and height are equal if 1:1 fixed aspect ratio is requested.
     */
    public static void fixRectForAspectRatio(Rect rect, int aspectRatioX, int aspectRatioY) {
        if (aspectRatioX == aspectRatioY && rect.width() != rect.height()) {
            if (rect.height() > rect.width()) {
                rect.bottom -= rect.height() - rect.width();
            } else {
                rect.right -= rect.width() - rect.height();
            }
        }
    }

    //region: Private methods

    /**
     * Decode specific rectangle bitmap from stream using sampling to get bitmap with the requested limit.
     */
    private static Bitmap decodeSampledBitmapRegion(Context context, Uri uri, Rect rect, int reqWidth, int reqHeight) {
        InputStream stream = null;
        try {
            ContentResolver resolver = context.getContentResolver();
            stream = resolver.openInputStream(uri);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calculateInSampleSize(rect.width(), rect.height(), reqWidth, reqHeight);

            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(stream, false);
            Bitmap bitmap = decoder.decodeRegion(rect, options);
            decoder.recycle();

            return bitmap;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load sampled bitmap: " + uri, e);
        } finally {
            closeSafe(stream);
        }
    }

    /**
     * Special crop of bitmap rotated by not stright angle, in this case the original crop bitmap contains parts
     * beyond the required crop area, this method crops the already cropped and rotated bitmap to the final
     * rectangle.<br>
     * Note: rotating by 0, 90, 180 or 270 degrees doesn't require extra cropping.
     */
    private static Bitmap cropForRotatedImage(Bitmap bitmap, float[] points, Rect rect, int degreesRotated,
                                              boolean fixAspectRatio, int aspectRatioX, int aspectRatioY) {
        if (degreesRotated % 90 != 0) {

            int adjLeft = 0, adjTop = 0, width = 0, height = 0;
            double rads = Math.toRadians(degreesRotated);
            int compareTo = degreesRotated < 90 || (degreesRotated > 180 && degreesRotated < 270) ? rect.left : rect.right;
            for (int i = 0; i < points.length; i += 2) {
                if (((int) points[i]) == compareTo) {
                    adjLeft = (int) Math.abs(Math.sin(rads) * (rect.bottom - points[i + 1]));
                    adjTop = (int) Math.abs(Math.cos(rads) * (points[i + 1] - rect.top));
                    width = (int) Math.abs((points[i + 1] - rect.top) / Math.sin(rads));
                    height = (int) Math.abs((rect.bottom - points[i + 1]) / Math.cos(rads));
                    break;
                }
            }

            rect.set(adjLeft, adjTop, adjLeft + width, adjTop + height);
            if (fixAspectRatio) {
                fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY);
            }

            Bitmap bitmapTmp = bitmap;
            bitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
            bitmapTmp.recycle();
        }
        return bitmap;
    }

    /**
     * Calculate the largest inSampleSize value that is a power of 2 and keeps both
     * height and width larger than the requested height and width.
     */
    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Get {@link File} object for the given Android URI.<br>
     * Use content resolver to get real path if direct path doesn't return valid file.
     */
    private static File getFileFromUri(Context context, Uri uri) {

        // first try by direct path
        File file = new File(uri.getPath());
        if (file.exists()) {
            return file;
        }

        // try reading real path from content resolver (gallery images)
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String realPath = cursor.getString(column_index);
            file = new File(realPath);
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return file;
    }

    /**
     * Rotate the given bitmap by the given degrees.<br>
     * New bitmap is created and the old one is recycled.
     */
    private static Bitmap rotateBitmapInt(Bitmap bitmap, int degrees) {
        if (degrees > 0) {
            Matrix matrix = new Matrix();
            matrix.setRotate(degrees);
            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            bitmap.recycle();
            return newBitmap;
        } else {
            return bitmap;
        }
    }

    /**
     * Close the given closeable object (Stream) in a safe way: check if it is null and catch-log
     * exception thrown.
     *
     * @param closeable the closable object to close
     */
    private static void closeSafe(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }
    //endregion

    //region: Inner class: DecodeBitmapResult

    /**
     * The result of {@link #decodeSampledBitmap(android.content.Context, android.net.Uri, int, int)}.
     */
    public static final class DecodeBitmapResult {

        /**
         * The loaded bitmap
         */
        public final Bitmap bitmap;

        /**
         * The sample size used to load the given bitmap
         */
        public final int sampleSize;

        DecodeBitmapResult(Bitmap bitmap, int sampleSize) {
            this.sampleSize = sampleSize;
            this.bitmap = bitmap;
        }
    }
    //endregion

    //region: Inner class: RotateBitmapResult

    /**
     * The result of {@link #rotateBitmapByExif(android.graphics.Bitmap, android.media.ExifInterface)}.
     */
    public static final class RotateBitmapResult {

        /**
         * The loaded bitmap
         */
        public final Bitmap bitmap;

        /**
         * The degrees the image was rotated
         */
        public final int degrees;

        RotateBitmapResult(Bitmap bitmap, int degrees) {
            this.bitmap = bitmap;
            this.degrees = degrees;
        }
    }
    //endregion
}