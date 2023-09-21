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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Built-in activity for image cropping.<br>
 * Use {@link CropImages#activity(ArrayList < Uri >)} to create a builder to start this activity.
 */
public class CropImagesActivity extends AppCompatActivity
{
  /** The crop image view library widget used in the activity */
  private ViewPager imageViewPager;
  private TabLayout mTabLayout;
  private ImagePagerAdapter mediaPagerAdapter;
  private ProgressBar pbView;

  private ArrayList<Uri> mCropImageUri;
  private ArrayList<Uri> mSavedCropImageUri;

  /** the options that were set for the crop image */
  private CropImageOptions mOptions;
  int mSelectedPosition = 0;
  HashMap<Integer, ImageView> retainer = new HashMap<>();
  int deviceWidth = getScreenWidth();
  int deviceHeight = getScreenHeight();
  int dp_60 = convertDpToPixels(60);
  int dp_8 = convertDpToPixels(8);

  @Override
  @SuppressLint("NewApi")
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_crop_images);

    pbView = findViewById(R.id.pbView);
    imageViewPager = findViewById(R.id.imageViewPager);
    mTabLayout = findViewById(R.id.filter_tabs);

    pbView.setVisibility(View.GONE);
    Bundle bundle = getIntent().getBundleExtra(CropImages.CROP_IMAGE_EXTRA_BUNDLE);
    mOptions = bundle.getParcelable(CropImages.CROP_IMAGE_EXTRA_OPTIONS);
    mCropImageUri = bundle.getParcelableArrayList(CropImages.CROP_IMAGE_EXTRA_SOURCE);

    mSavedCropImageUri = new ArrayList<>();
    if (mCropImageUri == null)
      mCropImageUri =  new ArrayList<>();

    if (savedInstanceState == null)
    {
      if (mCropImageUri.size() == 0)
      {
        if (CropImages.isExplicitCameraPermissionRequired(this))
          requestPermissions(new String[] {Manifest.permission.CAMERA}, CropImages.CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE);
        else
          CropImages.startPickImageActivity(this);
      }
      else if (CropImages.isReadExternalStoragePermissionsRequired(this, mCropImageUri))
        requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, CropImages.PICK_IMAGE_PERMISSIONS_REQUEST_CODE);
      else
        setupViewPager();
    }

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null)
    {
      CharSequence title = mOptions != null &&
              mOptions.activityTitle != null && mOptions.activityTitle.length() > 0
              ? mOptions.activityTitle
              : getResources().getString(R.string.crop_image_activity_title);
      actionBar.setTitle(title);
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  private void setupViewPager()
  {
    for (int x = 0; x < mCropImageUri.size(); x++)
      mSavedCropImageUri.add((Uri) ParcelableHelper.immediateDeepCopy(mCropImageUri.get(x)));

    mediaPagerAdapter = new ImagePagerAdapter();
    imageViewPager.setAdapter(mediaPagerAdapter);
    imageViewPager.setCurrentItem(mSelectedPosition);
    mTabLayout.setupWithViewPager(imageViewPager);

    if(mCropImageUri.size() == 1)
      mTabLayout.setVisibility(View.GONE);

    for (int i = 0; i < mTabLayout.getTabCount(); i++)
    {
      TabLayout.Tab tab = mTabLayout.getTabAt(i);
      tab.setCustomView(mediaPagerAdapter.getTabView(i));
    }

    imageViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
    {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

      }

      @Override
      public void onPageSelected(int position)
      {
        Log.d("fatal", "Fragment no. : " + position + " is visible now.");
        mSelectedPosition = position;
      }

      @Override
      public void onPageScrollStateChanged(int state)
      {
      }
    });
  }


  public static int getScreenWidth() {
    return Resources.getSystem().getDisplayMetrics().widthPixels;
  }

  public static int getScreenHeight() {
    return Resources.getSystem().getDisplayMetrics().heightPixels;
  }

  public int convertDpToPixels(float dp)
  {
    try {
      return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    } catch (Exception ignored) {
    }

    return (int) dp;
  }

  class ImagePagerAdapter extends PagerAdapter
  {
    @Override
    public int getCount()
    {
      return (null != mSavedCropImageUri) ? mSavedCropImageUri.size() : 0;
    }

    @Override
    public boolean isViewFromObject(View view, Object object)
    {

      return view == object;
    }

    @Override
    public int getItemPosition(@NonNull Object object)
    {

      return super.getItemPosition(object);
    }

    @Override
    public View instantiateItem(ViewGroup container, int position)
    {
      TouchImageView imageView = new TouchImageView(container.getContext());

      setImageInTouchView(position, imageView);

      container.addView(imageView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
      retainer.put(position, imageView);

      Log.d("fatal", "Load image for position" + position);
      return imageView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object)
    {
      retainer.remove(position);
      container.removeView((View) object);
    }

    public View getTabView(int position)
    {
      View v = LayoutInflater.from(CropImagesActivity.this).inflate(R.layout.crop_media_tab, null);
      final ImageView tv =  v.findViewById(R.id.name);

      tv.setImageResource(R.drawable.placeholder);
      ImageLoadTask imageLoadTask = new ImageLoadTask(CropImagesActivity.this, mSavedCropImageUri.get(position), dp_60, dp_60, new ImageLoadTaskListener() {
        @Override
        public void onComplete(Bitmap bitmap) {
          Log.d("fatal", "Bitmap width : "+ bitmap.getWidth() + " height : "+ bitmap.getHeight());
          tv.setImageBitmap(bitmap);
        }

        @Override
        public void onError(Throwable error) {
          Log.d("fatal", "Error : "+ error.getMessage());
          tv.setImageResource(R.drawable.placeholder);
        }
      });
      mExecutorLoadService.execute(imageLoadTask);

      return v;
    }
  }

  public void setImageInTouchView(int position, final ImageView imageView)
  {
    int width = deviceWidth - 2*dp_8;
    int height = deviceHeight - 2*dp_8 - dp_60;

    imageView.setImageResource(R.drawable.placeholder);
    ImageLoadTask imageLoadTask = new ImageLoadTask(CropImagesActivity.this, mSavedCropImageUri.get(position), width, height, new ImageLoadTaskListener() {
      @Override
      public void onComplete(Bitmap bitmap) {
        Log.d("fatal", "Bitmap width : "+ bitmap.getWidth() + " height : "+ bitmap.getHeight());
        imageView.setImageBitmap(bitmap);
      }

      @Override
      public void onError(Throwable error) {
        error.printStackTrace();
        Log.d("fatal", "Error : "+ error.getMessage());
        imageView.setImageResource(R.drawable.placeholder);
      }
    });

    mExecutorLoadService.execute(imageLoadTask);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    getMenuInflater().inflate(R.menu.menu_crop_images, menu);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    if (item.getItemId() == R.id.image_menu_done)
    {
      startCropping();
      return true;
    }

    if (item.getItemId() == R.id.image_menu_crop)
    {
      CropImage.activity(mCropImageUri.get(mSelectedPosition))
              .setGuidelines(mOptions.guidelines)
              .setAllowRotation(mOptions.allowRotation)
              .setAllowCounterRotation(mOptions.allowCounterRotation)
              .setRequestedSize(mOptions.outputRequestWidth, mOptions.outputRequestHeight)
              .setAllowFlipping(mOptions.allowFlipping)
              .setAutoZoomEnabled(mOptions.autoZoomEnabled)
              .setMinCropResultSize(mOptions.minCropResultWidth, mOptions.minCropResultHeight)
              .setMinCropWindowSize(mOptions.minCropWindowWidth, mOptions.minCropWindowHeight)
              .setActivityMenuIconColor(mOptions.activityMenuIconColor)
              .start(this);

      return true;
    }

    if (item.getItemId() == android.R.id.home) {
      setResultCancel();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  private ExecutorService mExecutorService = Executors.newFixedThreadPool(1);
  private ImageCompressTask imageCompressTask;

  private ExecutorService mExecutorLoadService = Executors.newFixedThreadPool(1);

  private void startCropping()
  {
    pbView.setVisibility(View.VISIBLE);
    imageCompressTask = new ImageCompressTask(CropImagesActivity.this, mSavedCropImageUri, mOptions, iImageCompressTaskListener);
    mExecutorService.execute(imageCompressTask);
  }

  //image compress task callback
  private IImageCompressTaskListener iImageCompressTaskListener = new IImageCompressTaskListener()
  {
    @Override
    public void onComplete(ArrayList<Uri> compressed)
    {
      Log.d("fatal", "New photo size ==> " + compressed.toString()); //log new file size.
      pbView.setVisibility(View.GONE);
      setResult(compressed, null);
    }

    @Override
    public void onError(Throwable error)
    {
      error.printStackTrace();
      Log.d("fatal", "Error occurred" );
      pbView.setVisibility(View.GONE);
      setResult(null, new Exception(error));
    }
  };

  @Override
  protected void onDestroy()
  {
    super.onDestroy();

    //clean up!
    mExecutorService.shutdown();
    mExecutorService = null;
    imageCompressTask = null;
  }

  @Override
  public void onBackPressed()
  {
    super.onBackPressed();
    setResultCancel();
  }

  @Override
  @SuppressLint("NewApi")
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    // handle result of pick image chooser
    if (requestCode == CropImages.PICK_IMAGE_CHOOSER_REQUEST_CODE)
    {
      if (resultCode == Activity.RESULT_CANCELED) {
        // User cancelled the picker. We don't have anything to crop
        setResultCancel();
      }

      if (resultCode == Activity.RESULT_OK)
      {
        if(data != null)
        {
          ClipData clipData = data.getClipData();

          if(clipData == null && data.getData() == null)
          {
            String action = data.getAction();
            boolean isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);

            if(isCamera)
              mCropImageUri.add(CropImages.getCaptureImageOutputUri(this));
            else
              setResultCancel();
          }
          else if(clipData == null)
            mCropImageUri.add(data.getData());
          else
          {
            for(int i=0; i<clipData.getItemCount(); i++)
            {
              ClipData.Item item = clipData.getItemAt(i);
              mCropImageUri.add(item.getUri());
            }
          }
        }
        else
          mCropImageUri.add(CropImages.getCaptureImageOutputUri(this));

        // For API >= 23 we need to check specifically that we have permissions to read external storage.
        if (CropImages.isReadExternalStoragePermissionsRequired(this, mCropImageUri))
          requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, CropImages.PICK_IMAGE_PERMISSIONS_REQUEST_CODE);
        else
          setupViewPager();
      }
    }
    else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
    {
      CropImage.ActivityResult result = CropImage.getActivityResult(data);
      if (resultCode == RESULT_OK)
      {
        mSavedCropImageUri.set(mSelectedPosition, result.getUri());
        setImageInTouchView(mSelectedPosition, retainer.get(mSelectedPosition));
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
  {
    if (requestCode == CropImages.PICK_IMAGE_PERMISSIONS_REQUEST_CODE)
    {
      if (mCropImageUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        setupViewPager();
      else
      {
        Toast.makeText(this, R.string.crop_image_activity_no_permissions, Toast.LENGTH_LONG).show();
        setResultCancel();
      }
    }

    if (requestCode == CropImages.CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE)
    {
      // Irrespective of whether camera permission was given or not, we show the picker
      // The picker will not add the camera intent if permission is not available
      CropImages.startPickImageActivity(this);
    }
  }

  /** Result with cropped image data or error if failed. */
  protected void setResult(ArrayList<Uri> uri, Exception error)
  {
    int resultCode = error == null ? RESULT_OK : CropImages.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE;
    setResult(resultCode, getResultIntent(uri, error));
    finish();
  }

  /** Cancel of cropping activity. */
  protected void setResultCancel() {
    setResult(RESULT_CANCELED);
    finish();
  }

  /** Get intent instance to be used for the result of this activity. */
  protected Intent getResultIntent(ArrayList<Uri> resultUri, Exception error)
  {
    CropImages.ActivityResult result = new CropImages.ActivityResult(mSavedCropImageUri, resultUri, error);

    Intent intent = new Intent();
    intent.putExtras(getIntent());
    intent.putExtra(CropImages.CROP_IMAGE_EXTRA_RESULT, result);
    return intent;
  }
}