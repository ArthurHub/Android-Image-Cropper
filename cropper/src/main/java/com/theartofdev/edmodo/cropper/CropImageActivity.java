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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.IOException;

/**
 * TODO:a add doc
 */
public class CropImageActivity extends Activity implements CropImageView.OnSaveCroppedImageCompleteListener {

    /**
     * The crop image view library widget used in the activity
     */
    private CropImageView mCropImageView;

    /**
     * the options that were set for the crop image
     */
    private CropImageOptions mOptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crop_image_activity);

        mCropImageView = (CropImageView) findViewById(R.id.cropImageView);
        mCropImageView.setOnSaveCroppedImageCompleteListener(this);

        Intent intent = getIntent();
        Uri source = intent.getParcelableExtra(CropImage.CROP_IMAGE_EXTRA_SOURCE);
        mOptions = intent.getParcelableExtra(CropImage.CROP_IMAGE_EXTRA_OPTIONS);

        if (savedInstanceState == null) {
            mCropImageView.setImageUriAsync(source);
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(mOptions.activityTitle);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.crop_image_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.crop_image_menu_crop) {
            Uri outputUri = mOptions.outputUri;
            if (outputUri.equals(Uri.EMPTY)) {
                try {
                    String ext = mOptions.outputCompressFormat == Bitmap.CompressFormat.JPEG ? ".jpg" :
                            mOptions.outputCompressFormat == Bitmap.CompressFormat.PNG ? ".png" : ".wepb";
                    outputUri = Uri.fromFile(File.createTempFile("cropped", ext, getCacheDir()));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create temp file for output image", e);
                }
            }
            mCropImageView.saveCroppedImageAsync(outputUri,
                    mOptions.outputCompressFormat,
                    mOptions.outputCompressQuality,
                    mOptions.reqWidth,
                    mOptions.reqHeight);
            return true;
        }
        if (item.getItemId() == R.id.crop_image_menu_rotate) {
            mCropImageView.rotateImage(90);
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            cancel();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        cancel();
    }

    @Override
    public void onGetCroppedImageComplete(CropImageView view, Uri uri, Exception error) {
        //setResult();
        finish();
    }

    private void cancel() {
        finish();
    }
}

