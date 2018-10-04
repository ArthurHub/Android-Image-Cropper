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
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

/**
 * Helper to simplify crop image work like starting pick-image acitvity and handling camera/gallery
 * intents.<br>
 * The goal of the helper is to simplify the starting and most-common usage of image cropping and
 * not all porpose all possible scenario one-to-rule-them-all code base. So feel free to use it as
 * is and as a wiki to make your own.<br>
 * Added value you get out-of-the-box is some edge case handling that you may miss otherwise, like
 * the stupid-ass Android camera result URI that may differ from version to version and from device
 * to device.
 */
@SuppressWarnings("WeakerAccess, unused")
public final class CropImage {

  // region: Fields and Consts

  /** The key used to pass crop image source URI to {@link CropImageActivity}. */
  public static final String CROP_IMAGE_EXTRA_SOURCE = "CROP_IMAGE_EXTRA_SOURCE";

  /** The key used to pass crop image options to {@link CropImageActivity}. */
  public static final String CROP_IMAGE_EXTRA_OPTIONS = "CROP_IMAGE_EXTRA_OPTIONS";

  /** The key used to pass crop image bundle data to {@link CropImageActivity}. */
  public static final String CROP_IMAGE_EXTRA_BUNDLE = "CROP_IMAGE_EXTRA_BUNDLE";

  /** The key used to pass crop image result data back from {@link CropImageActivity}. */
  public static final String CROP_IMAGE_EXTRA_RESULT = "CROP_IMAGE_EXTRA_RESULT";

  /**
   * The request code used to start pick image activity to be used on result to identify the this
   * specific request.
   */
  public static final int PICK_IMAGE_CHOOSER_REQUEST_CODE = 200;

  /** The request code used to request permission to pick image from external storage. */
  public static final int PICK_IMAGE_PERMISSIONS_REQUEST_CODE = 201;

  /** The request code used to request permission to capture image from camera. */
  public static final int CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE = 2011;

  /**
   * The request code used to start {@link CropImageActivity} to be used on result to identify the
   * this specific request.
   */
  public static final int CROP_IMAGE_ACTIVITY_REQUEST_CODE = 203;

  /** The result code used to return error from {@link CropImageActivity}. */
  public static final int CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE = 204;
  // endregion

  private CropImage() {}

  /**
   * Create a new bitmap that has all pixels beyond the oval shape transparent. Old bitmap is
   * recycled.
   */
  public static Bitmap toOvalBitmap(@NonNull Bitmap bitmap) {
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
   * Start an activity to get image for cropping using chooser intent that will have all the
   * available applications for the device like camera (MyCamera), galery (Photos), store apps
   * (Dropbox), etc.<br>
   * Use "pick_image_intent_chooser_title" string resource to override pick chooser title.
   *
   * @param activity the activity to be used to start activity from
   */
  public static void startPickImageActivity(@NonNull Activity activity) {
    activity.startActivityForResult(
        getPickImageChooserIntent(activity), PICK_IMAGE_CHOOSER_REQUEST_CODE);
  }

  /**
   * Same as {@link #startPickImageActivity(Activity) startPickImageActivity} method but instead of
   * being called and returning to an Activity, this method can be called and return to a Fragment.
   *
   * @param context The Fragments context. Use getContext()
   * @param fragment The calling Fragment to start and return the image to
   */
  public static void startPickImageActivity(@NonNull Context context, @NonNull Fragment fragment) {
    fragment.startActivityForResult(
        getPickImageChooserIntent(context), PICK_IMAGE_CHOOSER_REQUEST_CODE);
  }

  /**
   * Create a chooser intent to select the source to get image from.<br>
   * The source can be camera's (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br>
   * All possible sources are added to the intent chooser.<br>
   * Use "pick_image_intent_chooser_title" string resource to override chooser title.
   *
   * @param context used to access Android APIs, like content resolve, it is your
   *     activity/fragment/widget.
   */
  public static Intent getPickImageChooserIntent(@NonNull Context context) {
    return getPickImageChooserIntent(
        context, context.getString(R.string.pick_image_intent_chooser_title), false, true);
  }

  /**
   * Create a chooser intent to select the source to get image from.<br>
   * The source can be camera's (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br>
   * All possible sources are added to the intent chooser.
   *
   * @param context used to access Android APIs, like content resolve, it is your
   *     activity/fragment/widget.
   * @param title the title to use for the chooser UI
   * @param includeDocuments if to include KitKat documents activity containing all sources
   * @param includeCamera if to include camera intents
   */
  public static Intent getPickImageChooserIntent(
      @NonNull Context context,
      CharSequence title,
      boolean includeDocuments,
      boolean includeCamera) {

    List<Intent> allIntents = new ArrayList<>();
    PackageManager packageManager = context.getPackageManager();

    // collect all camera intents if Camera permission is available
    if (!isExplicitCameraPermissionRequired(context) && includeCamera) {
      allIntents.addAll(getCameraIntents(context, packageManager));
    }

    List<Intent> galleryIntents =
        getGalleryIntents(packageManager, Intent.ACTION_GET_CONTENT, includeDocuments);
    if (galleryIntents.size() == 0) {
      // if no intents found for get-content try pick intent action (Huawei P9).
      galleryIntents = getGalleryIntents(packageManager, Intent.ACTION_PICK, includeDocuments);
    }
    allIntents.addAll(galleryIntents);

    Intent target;
    if (allIntents.isEmpty()) {
      target = new Intent();
    } else {
      target = allIntents.get(allIntents.size() - 1);
      allIntents.remove(allIntents.size() - 1);
    }

    // Create a chooser from the main  intent
    Intent chooserIntent = Intent.createChooser(target, title);

    // Add all other intents
    chooserIntent.putExtra(
        Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

    return chooserIntent;
  }

  /**
   * Get the main Camera intent for capturing image using device camera app. If the outputFileUri is
   * null, a default Uri will be created with {@link #getCaptureImageOutputUri(Context)}, so then
   * you will be able to get the pictureUri using {@link #getPickImageResultUri(Context, Intent)}.
   * Otherwise, it is just you use the Uri passed to this method.
   *
   * @param context used to access Android APIs, like content resolve, it is your
   *     activity/fragment/widget.
   * @param outputFileUri the Uri where the picture will be placed.
   */
  public static Intent getCameraIntent(@NonNull Context context, Uri outputFileUri) {
    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (outputFileUri == null) {
      outputFileUri = getCaptureImageOutputUri(context);
    }
    intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
    return intent;
  }

  /** Get all Camera intents for capturing image using device camera apps. */
  public static List<Intent> getCameraIntents(
      @NonNull Context context, @NonNull PackageManager packageManager) {

    List<Intent> allIntents = new ArrayList<>();

    // Determine Uri of camera image to  save.
    Uri outputFileUri = getCaptureImageOutputUri(context);

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

    return allIntents;
  }

  /**
   * Get all Gallery intents for getting image from one of the apps of the device that handle
   * images.
   */
  public static List<Intent> getGalleryIntents(
      @NonNull PackageManager packageManager, String action, boolean includeDocuments) {
    List<Intent> intents = new ArrayList<>();
    Intent galleryIntent =
        action == Intent.ACTION_GET_CONTENT
            ? new Intent(action)
            : new Intent(action, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    galleryIntent.setType("image/*");
    List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
    for (ResolveInfo res : listGallery) {
      Intent intent = new Intent(galleryIntent);
      intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
      intent.setPackage(res.activityInfo.packageName);
      intents.add(intent);
    }

    // remove documents intent
    if (!includeDocuments) {
      for (Intent intent : intents) {
        if (intent
            .getComponent()
            .getClassName()
            .equals("com.android.documentsui.DocumentsActivity")) {
          intents.remove(intent);
          break;
        }
      }
    }
    return intents;
  }

  /**
   * Check if explicetly requesting camera permission is required.<br>
   * It is required in Android Marshmellow and above if "CAMERA" permission is requested in the
   * manifest.<br>
   * See <a
   * href="http://stackoverflow.com/questions/32789027/android-m-camera-intent-permission-bug">StackOverflow
   * question</a>.
   */
  public static boolean isExplicitCameraPermissionRequired(@NonNull Context context) {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        && hasPermissionInManifest(context, "android.permission.CAMERA")
        && context.checkSelfPermission(Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED;
  }

  /**
   * Check if the app requests a specific permission in the manifest.
   *
   * @param permissionName the permission to check
   * @return true - the permission in requested in manifest, false - not.
   */
  public static boolean hasPermissionInManifest(
      @NonNull Context context, @NonNull String permissionName) {
    String packageName = context.getPackageName();
    try {
      PackageInfo packageInfo =
          context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
      final String[] declaredPermisisons = packageInfo.requestedPermissions;
      if (declaredPermisisons != null && declaredPermisisons.length > 0) {
        for (String p : declaredPermisisons) {
          if (p.equalsIgnoreCase(permissionName)) {
            return true;
          }
        }
      }
    } catch (PackageManager.NameNotFoundException e) {
    }
    return false;
  }

  /**
   * Get URI to image received from capture by camera.
   *
   * @param context used to access Android APIs, like content resolve, it is your
   *     activity/fragment/widget.
   */
  public static Uri getCaptureImageOutputUri(@NonNull Context context) {
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
   * @param context used to access Android APIs, like content resolve, it is your
   *     activity/fragment/widget.
   * @param data the returned data of the activity result
   */
  public static Uri getPickImageResultUri(@NonNull Context context, @Nullable Intent data) {
    boolean isCamera = true;
    if (data != null && data.getData() != null) {
      String action = data.getAction();
      isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
    }
    return isCamera || data.getData() == null ? getCaptureImageOutputUri(context) : data.getData();
  }

  /**
   * Check if the given picked image URI requires READ_EXTERNAL_STORAGE permissions.<br>
   * Only relevant for API version 23 and above and not required for all URI's depends on the
   * implementation of the app that was used for picking the image. So we just test if we can open
   * the stream or do we get an exception when we try, Android is awesome.
   *
   * @param context used to access Android APIs, like content resolve, it is your
   *     activity/fragment/widget.
   * @param uri the result URI of image pick.
   * @return true - required permission are not granted, false - either no need for permissions or
   *     they are granted
   */
  public static boolean isReadExternalStoragePermissionsRequired(
      @NonNull Context context, @NonNull Uri uri) {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        && context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        && isUriRequiresPermissions(context, uri);
  }

  /**
   * Test if we can open the given Android URI to test if permission required error is thrown.<br>
   * Only relevant for API version 23 and above.
   *
   * @param context used to access Android APIs, like content resolve, it is your
   *     activity/fragment/widget.
   * @param uri the result URI of image pick.
   */
  public static boolean isUriRequiresPermissions(@NonNull Context context, @NonNull Uri uri) {
    try {
      ContentResolver resolver = context.getContentResolver();
      InputStream stream = resolver.openInputStream(uri);
      if (stream != null) {
        stream.close();
      }
      return false;
    } catch (Exception e) {
      return true;
    }
  }

  /**
   * Create {@link ActivityBuilder} instance to open image picker for cropping and then start {@link
   * CropImageActivity} to crop the selected image.<br>
   * Result will be received in {@link Activity#onActivityResult(int, int, Intent)} and can be
   * retrieved using {@link #getActivityResult(Intent)}.
   *
   * @return builder for Crop Image Activity
   */
  public static ActivityBuilder activity() {
    return new ActivityBuilder(null);
  }

  /**
   * Create {@link ActivityBuilder} instance to start {@link CropImageActivity} to crop the given
   * image.<br>
   * Result will be received in {@link Activity#onActivityResult(int, int, Intent)} and can be
   * retrieved using {@link #getActivityResult(Intent)}.
   *
   * @param uri the image Android uri source to crop or null to start a picker
   * @return builder for Crop Image Activity
   */
  public static ActivityBuilder activity(@Nullable Uri uri) {
    return new ActivityBuilder(uri);
  }

  /**
   * Get {@link CropImageActivity} result data object for crop image activity started using {@link
   * #activity(Uri)}.
   *
   * @param data result data intent as received in {@link Activity#onActivityResult(int, int,
   *     Intent)}.
   * @return Crop Image Activity Result object or null if none exists
   */
  public static ActivityResult getActivityResult(@Nullable Intent data) {
    return data != null ? (ActivityResult) data.getParcelableExtra(CROP_IMAGE_EXTRA_RESULT) : null;
  }

  // region: Inner class: ActivityBuilder

  /** Builder used for creating Image Crop Activity by user request. */
  public static final class ActivityBuilder {

    /** The image to crop source Android uri. */
    @Nullable private final Uri mSource;

    /** Options for image crop UX */
    private final CropImageOptions mOptions;

    private ActivityBuilder(@Nullable Uri source) {
      mSource = source;
      mOptions = new CropImageOptions();
    }

    /** Get {@link CropImageActivity} intent to start the activity. */
    public Intent getIntent(@NonNull Context context) {
      return getIntent(context, CropImageActivity.class);
    }

    /** Get {@link CropImageActivity} intent to start the activity. */
    public Intent getIntent(@NonNull Context context, @Nullable Class<?> cls) {
      mOptions.validate();

      Intent intent = new Intent();
      intent.setClass(context, cls);
      Bundle bundle = new Bundle();
      bundle.putParcelable(CROP_IMAGE_EXTRA_SOURCE, mSource);
      bundle.putParcelable(CROP_IMAGE_EXTRA_OPTIONS, mOptions);
      intent.putExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE, bundle);
      return intent;
    }

    /**
     * Start {@link CropImageActivity}.
     *
     * @param activity activity to receive result
     */
    public void start(@NonNull Activity activity) {
      mOptions.validate();
      activity.startActivityForResult(getIntent(activity), CROP_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    /**
     * Start {@link CropImageActivity}.
     *
     * @param activity activity to receive result
     */
    public void start(@NonNull Activity activity, @Nullable Class<?> cls) {
      mOptions.validate();
      activity.startActivityForResult(getIntent(activity, cls), CROP_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    /**
     * Start {@link CropImageActivity}.
     *
     * @param fragment fragment to receive result
     */
    public void start(@NonNull Context context, @NonNull Fragment fragment) {
      fragment.startActivityForResult(getIntent(context), CROP_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    /**
     * Start {@link CropImageActivity}.
     *
     * @param fragment fragment to receive result
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public void start(@NonNull Context context, @NonNull android.app.Fragment fragment) {
      fragment.startActivityForResult(getIntent(context), CROP_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    /**
     * Start {@link CropImageActivity}.
     *
     * @param fragment fragment to receive result
     */
    public void start(
        @NonNull Context context, @NonNull Fragment fragment, @Nullable Class<?> cls) {
      fragment.startActivityForResult(getIntent(context, cls), CROP_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    /**
     * Start {@link CropImageActivity}.
     *
     * @param fragment fragment to receive result
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public void start(
        @NonNull Context context, @NonNull android.app.Fragment fragment, @Nullable Class<?> cls) {
      fragment.startActivityForResult(getIntent(context, cls), CROP_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    /**
     * The shape of the cropping window.<br>
     * To set square/circle crop shape set aspect ratio to 1:1.<br>
     * <i>Default: RECTANGLE</i>
     */
    public ActivityBuilder setCropShape(@NonNull CropImageView.CropShape cropShape) {
      mOptions.cropShape = cropShape;
      return this;
    }

    /**
     * An edge of the crop window will snap to the corresponding edge of a specified bounding box
     * when the crop window edge is less than or equal to this distance (in pixels) away from the
     * bounding box edge (in pixels).<br>
     * <i>Default: 3dp</i>
     */
    public ActivityBuilder setSnapRadius(float snapRadius) {
      mOptions.snapRadius = snapRadius;
      return this;
    }

    /**
     * The radius of the touchable area around the handle (in pixels).<br>
     * We are basing this value off of the recommended 48dp Rhythm.<br>
     * See: http://developer.android.com/design/style/metrics-grids.html#48dp-rhythm<br>
     * <i>Default: 48dp</i>
     */
    public ActivityBuilder setTouchRadius(float touchRadius) {
      mOptions.touchRadius = touchRadius;
      return this;
    }

    /**
     * whether the guidelines should be on, off, or only showing when resizing.<br>
     * <i>Default: ON_TOUCH</i>
     */
    public ActivityBuilder setGuidelines(@NonNull CropImageView.Guidelines guidelines) {
      mOptions.guidelines = guidelines;
      return this;
    }

    /**
     * The initial scale type of the image in the crop image view<br>
     * <i>Default: FIT_CENTER</i>
     */
    public ActivityBuilder setScaleType(@NonNull CropImageView.ScaleType scaleType) {
      mOptions.scaleType = scaleType;
      return this;
    }

    /**
     * if to show crop overlay UI what contains the crop window UI surrounded by background over the
     * cropping image.<br>
     * <i>default: true, may disable for animation or frame transition.</i>
     */
    public ActivityBuilder setShowCropOverlay(boolean showCropOverlay) {
      mOptions.showCropOverlay = showCropOverlay;
      return this;
    }

    /**
     * if auto-zoom functionality is enabled.<br>
     * default: true.
     */
    public ActivityBuilder setAutoZoomEnabled(boolean autoZoomEnabled) {
      mOptions.autoZoomEnabled = autoZoomEnabled;
      return this;
    }

    /**
     * if multi touch functionality is enabled.<br>
     * default: true.
     */
    public ActivityBuilder setMultiTouchEnabled(boolean multiTouchEnabled) {
      mOptions.multiTouchEnabled = multiTouchEnabled;
      return this;
    }

    /**
     * The max zoom allowed during cropping.<br>
     * <i>Default: 4</i>
     */
    public ActivityBuilder setMaxZoom(int maxZoom) {
      mOptions.maxZoom = maxZoom;
      return this;
    }

    /**
     * The initial crop window padding from image borders in percentage of the cropping image
     * dimensions.<br>
     * <i>Default: 0.1</i>
     */
    public ActivityBuilder setInitialCropWindowPaddingRatio(float initialCropWindowPaddingRatio) {
      mOptions.initialCropWindowPaddingRatio = initialCropWindowPaddingRatio;
      return this;
    }

    /**
     * whether the width to height aspect ratio should be maintained or free to change.<br>
     * <i>Default: false</i>
     */
    public ActivityBuilder setFixAspectRatio(boolean fixAspectRatio) {
      mOptions.fixAspectRatio = fixAspectRatio;
      return this;
    }

    /**
     * the X,Y value of the aspect ratio.<br>
     * Also sets fixes aspect ratio to TRUE.<br>
     * <i>Default: 1/1</i>
     *
     * @param aspectRatioX the width
     * @param aspectRatioY the height
     */
    public ActivityBuilder setAspectRatio(int aspectRatioX, int aspectRatioY) {
      mOptions.aspectRatioX = aspectRatioX;
      mOptions.aspectRatioY = aspectRatioY;
      mOptions.fixAspectRatio = true;
      return this;
    }

    /**
     * the thickness of the guidelines lines (in pixels).<br>
     * <i>Default: 3dp</i>
     */
    public ActivityBuilder setBorderLineThickness(float borderLineThickness) {
      mOptions.borderLineThickness = borderLineThickness;
      return this;
    }

    /**
     * the color of the guidelines lines.<br>
     * <i>Default: Color.argb(170, 255, 255, 255)</i>
     */
    public ActivityBuilder setBorderLineColor(int borderLineColor) {
      mOptions.borderLineColor = borderLineColor;
      return this;
    }

    /**
     * thickness of the corner line (in pixels).<br>
     * <i>Default: 2dp</i>
     */
    public ActivityBuilder setBorderCornerThickness(float borderCornerThickness) {
      mOptions.borderCornerThickness = borderCornerThickness;
      return this;
    }

    /**
     * the offset of corner line from crop window border (in pixels).<br>
     * <i>Default: 5dp</i>
     */
    public ActivityBuilder setBorderCornerOffset(float borderCornerOffset) {
      mOptions.borderCornerOffset = borderCornerOffset;
      return this;
    }

    /**
     * the length of the corner line away from the corner (in pixels).<br>
     * <i>Default: 14dp</i>
     */
    public ActivityBuilder setBorderCornerLength(float borderCornerLength) {
      mOptions.borderCornerLength = borderCornerLength;
      return this;
    }

    /**
     * the color of the corner line.<br>
     * <i>Default: WHITE</i>
     */
    public ActivityBuilder setBorderCornerColor(int borderCornerColor) {
      mOptions.borderCornerColor = borderCornerColor;
      return this;
    }

    /**
     * the thickness of the guidelines lines (in pixels).<br>
     * <i>Default: 1dp</i>
     */
    public ActivityBuilder setGuidelinesThickness(float guidelinesThickness) {
      mOptions.guidelinesThickness = guidelinesThickness;
      return this;
    }

    /**
     * the color of the guidelines lines.<br>
     * <i>Default: Color.argb(170, 255, 255, 255)</i>
     */
    public ActivityBuilder setGuidelinesColor(int guidelinesColor) {
      mOptions.guidelinesColor = guidelinesColor;
      return this;
    }

    /**
     * the color of the overlay background around the crop window cover the image parts not in the
     * crop window.<br>
     * <i>Default: Color.argb(119, 0, 0, 0)</i>
     */
    public ActivityBuilder setBackgroundColor(int backgroundColor) {
      mOptions.backgroundColor = backgroundColor;
      return this;
    }

    /**
     * the min size the crop window is allowed to be (in pixels).<br>
     * <i>Default: 42dp, 42dp</i>
     */
    public ActivityBuilder setMinCropWindowSize(int minCropWindowWidth, int minCropWindowHeight) {
      mOptions.minCropWindowWidth = minCropWindowWidth;
      mOptions.minCropWindowHeight = minCropWindowHeight;
      return this;
    }

    /**
     * the min size the resulting cropping image is allowed to be, affects the cropping window
     * limits (in pixels).<br>
     * <i>Default: 40px, 40px</i>
     */
    public ActivityBuilder setMinCropResultSize(int minCropResultWidth, int minCropResultHeight) {
      mOptions.minCropResultWidth = minCropResultWidth;
      mOptions.minCropResultHeight = minCropResultHeight;
      return this;
    }

    /**
     * the max size the resulting cropping image is allowed to be, affects the cropping window
     * limits (in pixels).<br>
     * <i>Default: 99999, 99999</i>
     */
    public ActivityBuilder setMaxCropResultSize(int maxCropResultWidth, int maxCropResultHeight) {
      mOptions.maxCropResultWidth = maxCropResultWidth;
      mOptions.maxCropResultHeight = maxCropResultHeight;
      return this;
    }

    /**
     * the title of the {@link CropImageActivity}.<br>
     * <i>Default: ""</i>
     */
    public ActivityBuilder setActivityTitle(CharSequence activityTitle) {
      mOptions.activityTitle = activityTitle;
      return this;
    }

    /**
     * the color to use for action bar items icons.<br>
     * <i>Default: NONE</i>
     */
    public ActivityBuilder setActivityMenuIconColor(int activityMenuIconColor) {
      mOptions.activityMenuIconColor = activityMenuIconColor;
      return this;
    }

    /**
     * the Android Uri to save the cropped image to.<br>
     * <i>Default: NONE, will create a temp file</i>
     */
    public ActivityBuilder setOutputUri(Uri outputUri) {
      mOptions.outputUri = outputUri;
      return this;
    }

    /**
     * the compression format to use when writting the image.<br>
     * <i>Default: JPEG</i>
     */
    public ActivityBuilder setOutputCompressFormat(Bitmap.CompressFormat outputCompressFormat) {
      mOptions.outputCompressFormat = outputCompressFormat;
      return this;
    }

    /**
     * the quility (if applicable) to use when writting the image (0 - 100).<br>
     * <i>Default: 90</i>
     */
    public ActivityBuilder setOutputCompressQuality(int outputCompressQuality) {
      mOptions.outputCompressQuality = outputCompressQuality;
      return this;
    }

    /**
     * the size to resize the cropped image to.<br>
     * Uses {@link CropImageView.RequestSizeOptions#RESIZE_INSIDE} option.<br>
     * <i>Default: 0, 0 - not set, will not resize</i>
     */
    public ActivityBuilder setRequestedSize(int reqWidth, int reqHeight) {
      return setRequestedSize(reqWidth, reqHeight, CropImageView.RequestSizeOptions.RESIZE_INSIDE);
    }

    /**
     * the size to resize the cropped image to.<br>
     * <i>Default: 0, 0 - not set, will not resize</i>
     */
    public ActivityBuilder setRequestedSize(
        int reqWidth, int reqHeight, CropImageView.RequestSizeOptions options) {
      mOptions.outputRequestWidth = reqWidth;
      mOptions.outputRequestHeight = reqHeight;
      mOptions.outputRequestSizeOptions = options;
      return this;
    }

    /**
     * if the result of crop image activity should not save the cropped image bitmap.<br>
     * Used if you want to crop the image manually and need only the crop rectangle and rotation
     * data.<br>
     * <i>Default: false</i>
     */
    public ActivityBuilder setNoOutputImage(boolean noOutputImage) {
      mOptions.noOutputImage = noOutputImage;
      return this;
    }

    /**
     * the initial rectangle to set on the cropping image after loading.<br>
     * <i>Default: NONE - will initialize using initial crop window padding ratio</i>
     */
    public ActivityBuilder setInitialCropWindowRectangle(Rect initialCropWindowRectangle) {
      mOptions.initialCropWindowRectangle = initialCropWindowRectangle;
      return this;
    }

    /**
     * the initial rotation to set on the cropping image after loading (0-360 degrees clockwise).
     * <br>
     * <i>Default: NONE - will read image exif data</i>
     */
    public ActivityBuilder setInitialRotation(int initialRotation) {
      mOptions.initialRotation = (initialRotation + 360) % 360;
      return this;
    }

    /**
     * if to allow rotation during cropping.<br>
     * <i>Default: true</i>
     */
    public ActivityBuilder setAllowRotation(boolean allowRotation) {
      mOptions.allowRotation = allowRotation;
      return this;
    }

    /**
     * if to allow flipping during cropping.<br>
     * <i>Default: true</i>
     */
    public ActivityBuilder setAllowFlipping(boolean allowFlipping) {
      mOptions.allowFlipping = allowFlipping;
      return this;
    }

    /**
     * if to allow counter-clockwise rotation during cropping.<br>
     * Note: if rotation is disabled this option has no effect.<br>
     * <i>Default: false</i>
     */
    public ActivityBuilder setAllowCounterRotation(boolean allowCounterRotation) {
      mOptions.allowCounterRotation = allowCounterRotation;
      return this;
    }

    /**
     * The amount of degreees to rotate clockwise or counter-clockwise (0-360).<br>
     * <i>Default: 90</i>
     */
    public ActivityBuilder setRotationDegrees(int rotationDegrees) {
      mOptions.rotationDegrees = (rotationDegrees + 360) % 360;
      return this;
    }

    /**
     * whether the image should be flipped horizontally.<br>
     * <i>Default: false</i>
     */
    public ActivityBuilder setFlipHorizontally(boolean flipHorizontally) {
      mOptions.flipHorizontally = flipHorizontally;
      return this;
    }

    /**
     * whether the image should be flipped vertically.<br>
     * <i>Default: false</i>
     */
    public ActivityBuilder setFlipVertically(boolean flipVertically) {
      mOptions.flipVertically = flipVertically;
      return this;
    }

    /**
     * optional, set crop menu crop button title.<br>
     * <i>Default: null, will use resource string: crop_image_menu_crop</i>
     */
    public ActivityBuilder setCropMenuCropButtonTitle(CharSequence title) {
      mOptions.cropMenuCropButtonTitle = title;
      return this;
    }

    /**
     * Image resource id to use for crop icon instead of text.<br>
     * <i>Default: 0</i>
     */
    public ActivityBuilder setCropMenuCropButtonIcon(@DrawableRes int drawableResource) {
      mOptions.cropMenuCropButtonIcon = drawableResource;
      return this;
    }
  }
  // endregion

  // region: Inner class: ActivityResult

  /** Result data of Crop Image Activity. */
  public static final class ActivityResult extends CropImageView.CropResult implements Parcelable {

    public static final Creator<ActivityResult> CREATOR =
        new Creator<ActivityResult>() {
          @Override
          public ActivityResult createFromParcel(Parcel in) {
            return new ActivityResult(in);
          }

          @Override
          public ActivityResult[] newArray(int size) {
            return new ActivityResult[size];
          }
        };

    public ActivityResult(
        Uri originalUri,
        Uri uri,
        Exception error,
        float[] cropPoints,
        Rect cropRect,
        int rotation,
        Rect wholeImageRect,
        int sampleSize) {
      super(
          null,
          originalUri,
          null,
          uri,
          error,
          cropPoints,
          cropRect,
          wholeImageRect,
          rotation,
          sampleSize);
    }

    protected ActivityResult(Parcel in) {
      super(
          null,
          (Uri) in.readParcelable(Uri.class.getClassLoader()),
          null,
          (Uri) in.readParcelable(Uri.class.getClassLoader()),
          (Exception) in.readSerializable(),
          in.createFloatArray(),
          (Rect) in.readParcelable(Rect.class.getClassLoader()),
          (Rect) in.readParcelable(Rect.class.getClassLoader()),
          in.readInt(),
          in.readInt());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeParcelable(getOriginalUri(), flags);
      dest.writeParcelable(getUri(), flags);
      dest.writeSerializable(getError());
      dest.writeFloatArray(getCropPoints());
      dest.writeParcelable(getCropRect(), flags);
      dest.writeParcelable(getWholeImageRect(), flags);
      dest.writeInt(getRotation());
      dest.writeInt(getSampleSize());
    }

    @Override
    public int describeContents() {
      return 0;
    }
  }
  // endregion
}
