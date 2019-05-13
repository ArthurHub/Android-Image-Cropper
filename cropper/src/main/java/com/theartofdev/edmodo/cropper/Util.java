package com.theartofdev.edmodo.cropper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by root on 9/14/17.
 */

public class Util
{
    public static Uri getCompressed(Context context, Uri path, CropImageOptions options) throws IOException
    {
        if(context == null)
            throw new NullPointerException("Context must not be null.");

        Uri outputUri = options.outputUri;
        if (outputUri == null || outputUri.equals(Uri.EMPTY))
        {
            try
            {
                String ext = options.outputCompressFormat == Bitmap.CompressFormat.JPEG ? ".jpg" : options.outputCompressFormat == Bitmap.CompressFormat.PNG ? ".png" : ".webp";
                outputUri = Uri.fromFile(File.createTempFile("cropped", ext, context.getCacheDir()));
            } catch (IOException e) {
                throw new RuntimeException("Failed to create temp file for output image", e);
            }
        }

        int rotation = rotateByExif(context, path);
        Bitmap bitmap = decodeImageFromFiles(context, path, options.outputRequestWidth,  options.outputRequestHeight, rotation);

        //create placeholder for the compressed image file
        File compressed = new File(outputUri.getPath());

        //convert the decoded bitmap to stream
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        bitmap.compress(options.outputCompressFormat, options.outputCompressQuality, byteArrayOutputStream);

        FileOutputStream fileOutputStream = new FileOutputStream(compressed);
        fileOutputStream.write(byteArrayOutputStream.toByteArray());
        fileOutputStream.flush();
        fileOutputStream.close();


        //File written, return to the caller. Done!
        return Uri.fromFile(compressed);
    }

    private static Bitmap decodeImageFromFiles(Context context, Uri path, int requestedwidth, int requestedHeight, int rotation)
    {
        Log.d("fatal", "File path : " + path);

        InputStream stream = null;
        Bitmap bitmap = null;

        try
        {
            stream = context.getContentResolver().openInputStream(path);
            bitmap = BitmapFactory.decodeStream(stream);
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeSafe(stream);
        }

        Bitmap output = resizeBitmap(bitmap, requestedwidth, requestedHeight, rotation);

        Matrix m = new Matrix();
        m.setRotate(rotation, (float) output.getWidth() / 2, (float) output.getHeight() / 2);

        return Bitmap.createBitmap(output, 0, 0, output.getWidth(), output.getHeight(), m, true);
    }

    /** Resize the given bitmap to the given width/height by the given option.<br> */
    private static Bitmap resizeBitmap(Bitmap bitmap, int reqWidth, int reqHeight, int rotation)
    {
        try
        {
            if (reqWidth > 0 && reqHeight > 0)
            {
                Bitmap resized = null;
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                float scale = Math.max(width / (float) reqWidth, height / (float) reqHeight);

                if (scale > 1)
                    resized = Bitmap.createScaledBitmap(bitmap, (int) (width / scale), (int) (height / scale), false);

                if (resized != null)
                {
                    if (resized != bitmap)
                        bitmap.recycle();
                    return resized;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.d("fatal", "Failed to resize cropped image, return bitmap before resize");
        }

        return bitmap;
    }

    private static int rotateByExif(Context context, Uri uri)
    {
        ExifInterface ei = null;
        InputStream is = null;
        try
        {
            is = context.getContentResolver().openInputStream(uri);
            if (is != null)
                ei = new ExifInterface(is);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            closeSafe(is);
        }
        return ei != null ? rotateBitmapByExif(ei) : 0;
    }

    /**
     * Rotate the given image by given Exif value.<br>
     * If no rotation is required the image will not be rotated.<br>
     * New bitmap is created and the old one is recycled.
     */
    private static int rotateBitmapByExif(ExifInterface exif)
    {
        int degrees;
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (orientation)
        {
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
        return degrees;
    }


    private static void closeSafe(Closeable closeable)
    {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static Bitmap decodeSampledBitmapFromResource(Context context, Uri uri, int reqWidth, int reqHeight)
    {
        Log.d("fatal", "File path : " + uri);

        InputStream stream = null;
        Bitmap bitmap = null;
        final BitmapFactory.Options options = new BitmapFactory.Options();

        try
        {
            stream = context.getContentResolver().openInputStream(uri);
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(stream, null, options);
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeSafe(stream);
        }

        try
        {
            stream = context.getContentResolver().openInputStream(uri);
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeStream(stream, null, options);
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeSafe(stream);
        }

        return bitmap;
    }

    private static int calculateInSampleSize (BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth)
        {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}