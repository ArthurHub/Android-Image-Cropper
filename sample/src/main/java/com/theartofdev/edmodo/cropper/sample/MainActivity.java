
package com.theartofdev.edmodo.cropper.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.croppersample.R;
import com.theartofdev.edmodo.cropper.CropImageHelper;
import com.theartofdev.edmodo.cropper.CropImageView;

public class MainActivity extends Activity implements CropImageView.OnSetImageUriCompleteListener, CropImageView.OnGetCroppedImageCompleteListener {

    //region: Fields and Consts

    /**
     * The crop image view widget used in the crop image activity.<br>
     */
    protected CropImageView mCropImageView;

    /**
     * Stores the result image URI to crop after it was choosen but requires additional permission from the user.
     */
    protected Uri mCropImageUri;

    private static final int DEFAULT_ASPECT_RATIO_VALUES = 20;

    private static final int ROTATE_NINETY_DEGREES = 90;

    private static final String ASPECT_RATIO_X = "ASPECT_RATIO_X";

    private static final String ASPECT_RATIO_Y = "ASPECT_RATIO_Y";

    private static final int ON_TOUCH = 1;

    private int mAspectRatioX = DEFAULT_ASPECT_RATIO_VALUES;

    private int mAspectRatioY = DEFAULT_ASPECT_RATIO_VALUES;

    Bitmap croppedImage;
    //endregion

    // Saves the state upon rotating the screen/restarting the activity
    @Override
    protected void onSaveInstanceState(@SuppressWarnings("NullableProblems") Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt(ASPECT_RATIO_X, mAspectRatioX);
        bundle.putInt(ASPECT_RATIO_Y, mAspectRatioY);
    }

    // Restores the state upon rotating the screen/restarting the activity
    @Override
    protected void onRestoreInstanceState(@SuppressWarnings("NullableProblems") Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        mAspectRatioX = bundle.getInt(ASPECT_RATIO_X);
        mAspectRatioY = bundle.getInt(ASPECT_RATIO_Y);
    }

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        // Initialize components of the app
        mCropImageView = (CropImageView) findViewById(R.id.cropImageView);
        final SeekBar aspectRatioXSeek = (SeekBar) findViewById(R.id.aspectRatioXSeek);
        final SeekBar aspectRatioYSeek = (SeekBar) findViewById(R.id.aspectRatioYSeek);
        Spinner showGuidelinesSpin = (Spinner) findViewById(R.id.showGuidelinesSpin);
        final TextView aspectRatioNum = (TextView) findViewById(R.id.aspectRatioNum);

        if (savedInstanceState == null) {
            mCropImageView.setImageResource(R.drawable.butterfly);
        }

        // Sets sliders to be disabled until fixedAspectRatio is set
        aspectRatioXSeek.setEnabled(false);
        aspectRatioYSeek.setEnabled(false);

        // Set initial spinner value
        showGuidelinesSpin.setSelection(ON_TOUCH);

        //Sets the rotate button
        final Button rotateButton = (Button) findViewById(R.id.Button_rotate);
        rotateButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mCropImageView.rotateImage(ROTATE_NINETY_DEGREES);
            }
        });

        // Sets fixedAspectRatio
        ((ToggleButton) findViewById(R.id.fixedAspectRatioToggle)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCropImageView.setFixedAspectRatio(isChecked);
                if (isChecked) {
                    aspectRatioXSeek.setEnabled(true);
                    aspectRatioYSeek.setEnabled(true);
                } else {
                    aspectRatioXSeek.setEnabled(false);
                    aspectRatioYSeek.setEnabled(false);
                }
            }
        });

        // Sets crop shape
        ((ToggleButton) findViewById(R.id.cropShapeToggle)).setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCropImageView.setCropShape(isChecked ? CropImageView.CropShape.OVAL : CropImageView.CropShape.RECTANGLE);
            }
        });

        // Sets initial aspect ratio to 10/10, for demonstration purposes
        mCropImageView.setAspectRatio(DEFAULT_ASPECT_RATIO_VALUES, DEFAULT_ASPECT_RATIO_VALUES);

        aspectRatioXSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar aspectRatioXSeek, int progress, boolean fromUser) {
                mAspectRatioX = Math.max(1, progress);
                mCropImageView.setAspectRatio(mAspectRatioX, mAspectRatioY);
                aspectRatioNum.setText("(" + mAspectRatioX + ", " + mAspectRatioY + ")");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        aspectRatioYSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar aspectRatioYSeek, int progress, boolean fromUser) {
                mAspectRatioY = Math.max(1, progress);
                mCropImageView.setAspectRatio(mAspectRatioX, mAspectRatioY);
                aspectRatioNum.setText("(" + mAspectRatioX + ", " + mAspectRatioY + ")");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Sets up the Spinner
        showGuidelinesSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        mCropImageView.setGuidelines(CropImageView.Guidelines.OFF);
                        break;
                    case 1:
                        mCropImageView.setGuidelines(CropImageView.Guidelines.ON_TOUCH);
                        break;
                    case 2:
                        mCropImageView.setGuidelines(CropImageView.Guidelines.ON);
                        break;
                }
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            }
        });

        final TextView snapRadiusNum = (TextView) findViewById(R.id.snapRadiusNum);
        ((SeekBar) findViewById(R.id.snapRadiusSeek)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCropImageView.setSnapRadius(progress);
                snapRadiusNum.setText(Integer.toString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        findViewById(R.id.Button_crop).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mCropImageView.getCroppedImageAsync(mCropImageView.getCropShape(), 0, 0);
            }
        });

        findViewById(R.id.Button_load).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                CropImageHelper.startPickImageActivity(MainActivity.this);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCropImageView.setOnSetImageUriCompleteListener(this);
        mCropImageView.setOnGetCroppedImageCompleteListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCropImageView.setOnSetImageUriCompleteListener(null);
        mCropImageView.setOnGetCroppedImageCompleteListener(null);
    }

    @Override
    @SuppressLint("NewApi")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImageHelper.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri imageUri = CropImageHelper.getPickImageResultUri(this, data);

            // For API >= 23 we need to check specifically that we have permissions to read external storage,
            // but we don't know if we need to for the URI so the simplest is to try open the stream and see if we get error.
            boolean requirePermissions = false;
            if (CropImageHelper.isReadExternalStoragePermissionsRequired(this, imageUri)) {

                // request permissions and handle the result in onRequestPermissionsResult()
                requirePermissions = true;
                mCropImageUri = imageUri;
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            } else {

                mCropImageView.setImageUriAsync(imageUri);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (mCropImageUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mCropImageView.setImageUriAsync(mCropImageUri);
        } else {
            Toast.makeText(this, "Cancelling, required permissions are not granted", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSetImageUriComplete(CropImageView view, Uri uri, Exception error) {
        if (error == null) {
            Toast.makeText(this, "Image load successful", Toast.LENGTH_SHORT).show();
        } else {
            Log.e("AIC", "Failed to load image by URI", error);
            Toast.makeText(this, "Image load failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onGetCroppedImageComplete(CropImageView view, Bitmap bitmap, Exception error) {
        if (error == null) {
            croppedImage = bitmap;
            ImageView croppedImageView = (ImageView) findViewById(R.id.croppedImageView);
            croppedImageView.setImageBitmap(croppedImage);
        } else {
            Log.e("AIC", "Failed to crop image", error);
            Toast.makeText(this, "Image crop failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}