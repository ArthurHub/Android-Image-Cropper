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

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.system.ErrnoException;
import android.system.OsConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper to simplify crop image work like starting pick-image acitvity and handling camera/gallery intents.<br>
 * The goal of the helper is to simplify the starting and most-common usage of image cropping and not
 * all porpose all possible scenario one-to-rule-them-all code base. So feel free to use it as is and as
 * a wiki to make your own.<br>
 * Added value you get out-of-the-box is some edge case handling that you may miss otherwise, like the
 * stupid-ass Android camera result URI that may differ from version to version and from device to device.
 */
public final class CropImage {

    /**
     * The request code used to start pick image activity to be used on result to identify the this specific request.
     */
    public static final int PICK_IMAGE_CHOOSER_REQUEST_CODE = 200;

    /**
     * The request code used to start pick image activity to be used on result to identify the this specific request.
     */
    public static final int PICK_IMAGE_PERMISSIONS_REQUEST_CODE = 201;

    /**
     * The request code used to start {@link CropImageActivity} to be used on result to identify the this specific
     * request.
     */
    public static final int CROP_IMAGE_ACTIVITY_REQUEST_CODE = 203;

    private CropImage() {
    }

    /**
     * Create a new bitmap that has all pixels beyond the oval shape transparent.
     * Old bitmap is recycled.
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
     * Start an activity to get image for cropping using chooser intent that will have all the available
     * applications for the device like camera (MyCamera), galery (Photos), store apps (Dropbox), etc.
     *
     * @param activity the activity to be used to start activity from
     */
    public static void startPickImageActivity(Activity activity) {
        activity.startActivityForResult(getPickImageChooserIntent(activity), PICK_IMAGE_CHOOSER_REQUEST_CODE);
    }

    /**
     * Create a chooser intent to select the  source to get image from.<br>
     * The source can be camera's  (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br>
     * All possible sources are added to the intent chooser.
     *
     * @param context used to access Android APIs, like content resolve, it is your activity/fragment/widget.
     */
    public static Intent getPickImageChooserIntent(Context context) {

        // Determine Uri of camera image to  save.
        Uri outputFileUri = getCaptureImageOutputUri(context);

        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();

        // collect all camera intents
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            }
            allIntents.add(intent);
        }

        // collect all gallery intents
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            allIntents.add(intent);
        }

        // the main intent is the last in the  list (fucking android) so pickup the useless one
        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (intent.getComponent().getClassName().equals("com.android.documentsui.DocumentsActivity")) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);

        // Create a chooser from the main  intent
        Intent chooserIntent = Intent.createChooser(mainIntent, "Select source");

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;
    }

    /**
     * Get URI to image received from capture  by camera.
     *
     * @param context used to access Android APIs, like content resolve, it is your activity/fragment/widget.
     */
    public static Uri getCaptureImageOutputUri(Context context) {
        Uri outputFileUri = null;
        File getImage = context.getExternalCacheDir();
        if (getImage != null) {
            outputFileUri = Uri.fromFile(new File(getImage.getPath(), "pickImageResult.jpeg"));
        }
        return outputFileUri;
    }

    /**
     * Get the URI of the selected image from {@link #getPickImageChooserIntent(Context)}.<br>
     * Will return the correct URI for camera and gallery image.
     *
     * @param context used to access Android APIs, like content resolve, it is your activity/fragment/widget.
     * @param data the returned data of the  activity result
     */
    public static Uri getPickImageResultUri(Context context, Intent data) {
        boolean isCamera = true;
        if (data != null && data.getData() != null) {
            String action = data.getAction();
            isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
        }
        return isCamera ? getCaptureImageOutputUri(context) : data.getData();
    }

    /**
     * Check if the given picked image URI requires READ_EXTERNAL_STORAGE permissions.<br>
     * Only relevant for API version 23 and above and not required for all URI's depends on the
     * implementation of the app that was used for picking the image. So we just test if we can open the stream or
     * do we get an exception when we try, Android is awesome.
     *
     * @param context used to access Android APIs, like content resolve, it is your activity/fragment/widget.
     * @param uri the result URI of image pick.
     * @return true - required permission are not granted, false - either no need for permissions or they are granted
     */
    public static boolean isReadExternalStoragePermissionsRequired(Context context, Uri uri) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                isUriRequiresPermissions(context, uri);
    }

    /**
     * Test if we can open the given Android URI to test if permission required error is thrown.<br>
     * Only relevant for API version 23 and above.
     *
     * @param context used to access Android APIs, like content resolve, it is your activity/fragment/widget.
     * @param uri the result URI of image pick.
     */
    public static boolean isUriRequiresPermissions(Context context, Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            InputStream stream = resolver.openInputStream(uri);
            stream.close();
            return false;
        } catch (FileNotFoundException e) {
            if (e.getCause() instanceof ErrnoException && ((ErrnoException) e.getCause()).errno == OsConstants.EACCES) {
                return true;
            }
        } catch (SecurityException e) {
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Create builder for {@link CropImageActivity} intent to be used for cropping the given image.
     *
     * @param context Android Context
     * @param uri Android URI to source image to crop
     * @return builder for Crop Image Activity
     */

    public static ActivityBuilder activity(Context context, Uri uri) {
        return new ActivityBuilder(context, uri);
    }

    //region: Inner class: ActivityBuilder

    /**
     * Builder used for creating Image Crop Activity by user request.
     */
    public static final class ActivityBuilder {

        /**
         * The context to build the activity intent in.
         */
        private Context mContext;

        /**
         * The intent for Crop Image Activity.
         */
        private final Intent mIntent;

        /**
         * Options for image crop UX
         */
        private final CropImageOptions mOptions;

        private ActivityBuilder(Context context, Uri source) {
            mContext = context;

            mIntent = new Intent();
            mIntent.setClass(context, CropImageActivity.class);
            mIntent.putExtra("CROP_IMAGE_EXTRA_SOURCE", source);

            mOptions = new CropImageOptions(context.getResources().getDisplayMetrics());
        }

        /**
         * Get {@link CropImageActivity} intent to start the activity.
         */
        public Intent getIntent() {
            mOptions.validate();
            mIntent.putExtra("CROP_IMAGE_EXTRA_OPTIONS", mOptions);
            return mIntent;
        }

        /**
         * Start {@link CropImageActivity}.
         *
         * @param activity activity to receive result
         */
        public void start(Activity activity) {
            mOptions.validate();
            activity.startActivityForResult(getIntent(), CROP_IMAGE_ACTIVITY_REQUEST_CODE);
        }

        /**
         * Start {@link CropImageActivity}.
         *
         * @param fragment fragment to receive result
         */
        public void start(Fragment fragment) {
            fragment.startActivityForResult(getIntent(), CROP_IMAGE_ACTIVITY_REQUEST_CODE);
        }

        /**
         * The shape of the cropping window.
         */
        public void setCropShape(CropImageView.CropShape cropShape) {
            mOptions.cropShape = cropShape;
        }

        /**
         * An edge of the crop window will snap to the corresponding edge of a specified bounding box
         * when the crop window edge is less than or equal to this distance (in pixels) away from the bounding box
         * edge.
         */
        public void setSnapRadius(float snapRadius) {
            mOptions.snapRadius = snapRadius;
        }

        /**
         * The radius of the touchable area around the handle.<br>
         * We are basing this value off of the recommended 48dp Rhythm.<br>
         * See: http://developer.android.com/design/style/metrics-grids.html#48dp-rhythm
         */
        public void setTouchRadius(float touchRadius) {
            mOptions.touchRadius = touchRadius;
        }

        /**
         * whether the guidelines should be on, off, or only showing when resizing.
         */
        public void setGuidelines(CropImageView.Guidelines guidelines) {
            mOptions.guidelines = guidelines;
        }

        /**
         * The initial scale type of the image in the crop image view
         */
        public void setScaleType(CropImageView.ScaleType scaleType) {
            mOptions.scaleType = scaleType;
        }

        /**
         * if to show crop overlay UI what contains the crop window UI surrounded by background over the cropping
         * image.<br>
         * default: true, may disable for animation or frame transition.
         */
        public void setShowCropOverlay(boolean showCropOverlay) {
            mOptions.showCropOverlay = showCropOverlay;
        }

        /**
         * if auto-zoom functionality is enabled.<br>
         * default: true.
         */
        public void setAutoZoomEnabled(boolean autoZoomEnabled) {
            mOptions.autoZoomEnabled = autoZoomEnabled;
        }

        /**
         * The max zoom allowed during cropping
         */
        public void setMaxZoom(int maxZoom) {
            mOptions.maxZoom = maxZoom;
        }

        /**
         * The initial crop window padding from image borders in percentage of the cropping image dimensions.
         */
        public void setInitialCropWindowPaddingRatio(float initialCropWindowPaddingRatio) {
            mOptions.initialCropWindowPaddingRatio = initialCropWindowPaddingRatio;
        }

        /**
         * whether the width to height aspect ratio should be maintained or free to change.
         */
        public void setFixAspectRatio(boolean fixAspectRatio) {
            mOptions.fixAspectRatio = fixAspectRatio;
        }

        /**
         * the X,Y value of the aspect ratio
         */
        public void setAspectRatio(int aspectRatioX, int aspectRatioY) {
            mOptions.aspectRatioX = aspectRatioX;
            mOptions.aspectRatioY = aspectRatioY;
        }

        /**
         * the thickness of the guidelines lines in pixels
         */
        public void setBorderLineThickness(float borderLineThickness) {
            mOptions.borderLineThickness = borderLineThickness;
        }

        /**
         * the color of the guidelines lines
         */
        public void setBorderLineColor(int borderLineColor) {
            mOptions.borderLineColor = borderLineColor;
        }

        /**
         * thickness of the corner line
         */
        public void setBorderCornerThickness(float borderCornerThickness) {
            mOptions.borderCornerThickness = borderCornerThickness;
        }

        /**
         * the offset of corner line from crop window border
         */
        public void setBorderCornerOffset(float borderCornerOffset) {
            mOptions.borderCornerOffset = borderCornerOffset;
        }

        /**
         * the length of the corner line away from the corner
         */
        public void setBorderCornerLength(float borderCornerLength) {
            mOptions.borderCornerLength = borderCornerLength;
        }

        /**
         * the color of the corner line
         */
        public void setBorderCornerColor(int borderCornerColor) {
            mOptions.borderCornerColor = borderCornerColor;
        }

        /**
         * the thickness of the guidelines lines
         */
        public void setGuidelinesThickness(float guidelinesThickness) {
            mOptions.guidelinesThickness = guidelinesThickness;
        }

        /**
         * the color of the guidelines lines
         */
        public void setGuidelinesColor(int guidelinesColor) {
            mOptions.guidelinesColor = guidelinesColor;
        }

        /**
         * the color of the overlay background around the crop window cover the image parts not in the crop window.
         */
        public void setBackgroundColor(int backgroundColor) {
            mOptions.backgroundColor = backgroundColor;
        }

        /**
         * the min size the crop window is allowed to be.
         */
        public void setMinCropWindowSize(float minCropWindowWidth, float minCropWindowHeight) {
            mOptions.minCropWindowWidth = minCropWindowWidth;
            mOptions.minCropWindowHeight = minCropWindowHeight;
        }

        /**
         * the min size the resulting cropping image is allowed to be, affects the cropping window limits.
         */
        public void setMinCropResultSize(float minCropResultWidth, float minCropResultHeight) {
            mOptions.minCropResultWidth = minCropResultWidth;
            mOptions.minCropResultHeight = minCropResultHeight;
        }

        /**
         * the max size the resulting cropping image is allowed to be, affects the cropping window limits.
         */
        public void setMaxCropResultSize(float maxCropResultWidth, float maxCropResultHeight) {
            mOptions.maxCropResultWidth = maxCropResultWidth;
            mOptions.maxCropResultHeight = maxCropResultHeight;
        }
    }
    //endregion
}