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

package com.theartofdev.edmodo.cropper.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class that deals with operations with an ImageView.
 */
public class ImageViewUtil {

    /**
     * Gets the rectangular position of a Bitmap if it were placed inside a View.
     *
     * @param bitmap the Bitmap
     * @param view the parent View of the Bitmap
     * @param scaleType the desired scale type
     * @return the rectangular position of the Bitmap
     */
    public static Rect getBitmapRect(Bitmap bitmap, View view, ImageView.ScaleType scaleType) {

        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();
        final int viewWidth = view.getWidth();
        final int viewHeight = view.getHeight();

        switch (scaleType) {
            default:
            case CENTER_INSIDE:
                return getBitmapRectCenterInsideHelper(bitmapWidth, bitmapHeight, viewWidth, viewHeight);
            case FIT_CENTER:
                return getBitmapRectFitCenterHelper(bitmapWidth, bitmapHeight, viewWidth, viewHeight);
        }
    }

    /**
     * Gets the rectangular position of a Bitmap if it were placed inside a View.
     *
     * @param bitmapWidth the Bitmap's width
     * @param bitmapHeight the Bitmap's height
     * @param viewWidth the parent View's width
     * @param viewHeight the parent View's height
     * @param scaleType the desired scale type
     * @return the rectangular position of the Bitmap
     */
    public static Rect getBitmapRect(int bitmapWidth,
                                     int bitmapHeight,
                                     int viewWidth,
                                     int viewHeight, ImageView.ScaleType scaleType) {
        switch (scaleType) {
            default:
            case CENTER_INSIDE:
                return getBitmapRectCenterInsideHelper(bitmapWidth, bitmapHeight, viewWidth, viewHeight);
            case FIT_CENTER:
                return getBitmapRectFitCenterHelper(bitmapWidth, bitmapHeight, viewWidth, viewHeight);
        }
    }

    /**
     * Rotate the given image by reading the Exif value of the image (uri).<br>
     * If no rotation is required the image will not be rotated.<br>
     * New bitmap is created and the old one is recycled.
     */
    public static RotateBitmapResult rotateBitmapByExif(Context context, Bitmap bitmap, Uri uri) {
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
        int degrees = 0;
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
        }
        if (degrees > 0) {
            bitmap = rotateBitmap(bitmap, degrees);
        }
        return new RotateBitmapResult(bitmap, degrees);
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
            BitmapFactory.decodeStream(stream, new Rect(0, 0, 0, 0), options);
            options.inJustDecodeBounds = false;

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            closeSafe(stream);
            stream = resolver.openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(stream, new Rect(0, 0, 0, 0), options);

            return new DecodeBitmapResult(bitmap, options.inSampleSize);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load sampled bitmap", e);
        } finally {
            closeSafe(stream);
        }
    }

    /**
     * Decode specific rectangle bitmap from stream using sampling to get bitmap with the requested limit.
     */
    public static DecodeBitmapResult decodeSampledBitmapRegion(Context context, Uri uri, Rect rect, int reqWidth, int reqHeight) {
        InputStream stream = null;
        try {
            ContentResolver resolver = context.getContentResolver();
            stream = resolver.openInputStream(uri);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calculateInSampleSize(rect.width(), rect.height(), reqWidth, reqHeight);

            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(stream, false);
            Bitmap bitmap = decoder.decodeRegion(rect, options);

            return new DecodeBitmapResult(bitmap, options.inSampleSize);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load sampled bitmap", e);
        } finally {
            closeSafe(stream);
        }
    }

    /**
     * Calculate the largest inSampleSize value that is a power of 2 and keeps both
     * height and width larger than the requested height and width.
     */
    public static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
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
     * Get {@link java.io.File} object for the given Android URI.<br>
     * Use content resolver to get real path if direct path doesn't return valid file.
     */
    public static File getFileFromUri(Context context, Uri uri) {

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
            if (cursor != null)
                cursor.close();
        }

        return file;
    }

    /**
     * Rotate the given bitmap by the given degrees.<br>
     * New bitmap is created and the old one is recycled.
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees);
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        bitmap.recycle();
        return newBitmap;
    }

    /**
     * Close the given closeable object (Stream) in a safe way: check if it is null and catch-log
     * exception thrown.
     *
     * @param closeable the closable object to close
     */
    public static void closeSafe(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Helper that does the work of the above functions. Gets the rectangular
     * position of a Bitmap if it were placed inside a View with scale type set
     * to {@link android.widget.ImageView.ScaleType #CENTER_INSIDE}.
     *
     * @param bitmapWidth the Bitmap's width
     * @param bitmapHeight the Bitmap's height
     * @param viewWidth the parent View's width
     * @param viewHeight the parent View's height
     * @return the rectangular position of the Bitmap
     */
    private static Rect getBitmapRectCenterInsideHelper(int bitmapWidth,
                                                        int bitmapHeight,
                                                        int viewWidth,
                                                        int viewHeight) {
        double resultWidth;
        double resultHeight;
        int resultX;
        int resultY;

        double viewToBitmapWidthRatio = Double.POSITIVE_INFINITY;
        double viewToBitmapHeightRatio = Double.POSITIVE_INFINITY;

        // Checks if either width or height needs to be fixed
        if (viewWidth < bitmapWidth) {
            viewToBitmapWidthRatio = (double) viewWidth / (double) bitmapWidth;
        }
        if (viewHeight < bitmapHeight) {
            viewToBitmapHeightRatio = (double) viewHeight / (double) bitmapHeight;
        }

        // If either needs to be fixed, choose smallest ratio and calculate from
        // there
        if (viewToBitmapWidthRatio != Double.POSITIVE_INFINITY || viewToBitmapHeightRatio != Double.POSITIVE_INFINITY) {
            if (viewToBitmapWidthRatio <= viewToBitmapHeightRatio) {
                resultWidth = viewWidth;
                resultHeight = (bitmapHeight * resultWidth / bitmapWidth);
            } else {
                resultHeight = viewHeight;
                resultWidth = (bitmapWidth * resultHeight / bitmapHeight);
            }
        }
        // Otherwise, the picture is within frame layout bounds. Desired width
        // is simply picture size
        else {
            resultHeight = bitmapHeight;
            resultWidth = bitmapWidth;
        }

        // Calculate the position of the bitmap inside the ImageView.
        if (resultWidth == viewWidth) {
            resultX = 0;
            resultY = (int) Math.round((viewHeight - resultHeight) / 2);
        } else if (resultHeight == viewHeight) {
            resultX = (int) Math.round((viewWidth - resultWidth) / 2);
            resultY = 0;
        } else {
            resultX = (int) Math.round((viewWidth - resultWidth) / 2);
            resultY = (int) Math.round((viewHeight - resultHeight) / 2);
        }

        final Rect result = new Rect(resultX,
                resultY,
                resultX + (int) Math.ceil(resultWidth),
                resultY + (int) Math.ceil(resultHeight));

        return result;
    }

    /**
     * Helper that does the work of the above functions. Gets the rectangular
     * position of a Bitmap if it were placed inside a View with scale type set
     * to {@link ImageView.ScaleType#FIT_CENTER}.
     *
     * @param bitmapWidth the Bitmap's width
     * @param bitmapHeight the Bitmap's height
     * @param viewWidth the parent View's width
     * @param viewHeight the parent View's height
     * @return the rectangular position of the Bitmap
     */
    private static Rect getBitmapRectFitCenterHelper(int bitmapWidth, int bitmapHeight, int viewWidth, int viewHeight) {
        double resultWidth;
        double resultHeight;
        int resultX;
        int resultY;

        double viewToBitmapWidthRatio = (double) viewWidth / bitmapWidth;
        double viewToBitmapHeightRatio = (double) viewHeight / bitmapHeight;

        // If either needs to be fixed, choose smallest ratio and calculate from
        // there
        if (viewToBitmapWidthRatio <= viewToBitmapHeightRatio) {
            resultWidth = viewWidth;
            resultHeight = (bitmapHeight * resultWidth / bitmapWidth);
        } else {
            resultHeight = viewHeight;
            resultWidth = (bitmapWidth * resultHeight / bitmapHeight);
        }

        // Calculate the position of the bitmap inside the ImageView.
        if (resultWidth == viewWidth) {
            resultX = 0;
            resultY = (int) Math.round((viewHeight - resultHeight) / 2);
        } else if (resultHeight == viewHeight) {
            resultX = (int) Math.round((viewWidth - resultWidth) / 2);
            resultY = 0;
        } else {
            resultX = (int) Math.round((viewWidth - resultWidth) / 2);
            resultY = (int) Math.round((viewHeight - resultHeight) / 2);
        }

        final Rect result = new Rect(resultX,
                resultY,
                resultX + (int) Math.ceil(resultWidth),
                resultY + (int) Math.ceil(resultHeight));

        return result;
    }

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