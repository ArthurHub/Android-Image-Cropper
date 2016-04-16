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

package com.theartofdev.edmodo.cropper.sample;

import android.Manifest;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.croppersample.R;
import com.theartofdev.edmodo.cropper.CropImageHelper;
import com.theartofdev.edmodo.cropper.CropImageView;

public class MainActivity extends Activity {
    private static final int ON_PERMISSIONS_REQUESTED = 0x02;

    //region: Fields and Consts

    DrawerLayout mDrawerLayout;

    private ActionBarDrawerToggle mDrawerToggle;

    private MainFragment mCurrentFragment;

    private Uri mCropImageUri;

    private CropImageViewOptions mCropImageViewOptions = new CropImageViewOptions();
    //endregion

    public void setCurrentFragment(MainFragment fragment) {
        mCurrentFragment = fragment;
    }

    public void setCurrentOptions(CropImageViewOptions options) {
        mCropImageViewOptions = options;
        updateDrawerTogglesByOptions(options);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.main_drawer_open, R.string.main_drawer_close);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            setMainFragmentByPreset(CropDemoPreset.RECT);
        }
    }

    /**
     * Called when activity resume is complete (after {@link #onResume} has
     * been called). Applications will generally not implement this method;
     * it is intended for system classes to do final setup after application
     * resume code has run.
     * <p/>
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @see #onResume
     */
    @Override
    protected void onPostResume() {
        super.onPostResume();

        // XXX Quick and dirty permission granting for the sample app.
        // XXX The existing code did it too late.

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(getApplicationContext())
                                .setCancelable(true)
                                .setMessage("The sample app needs permission: " +
                                            Manifest.permission.READ_EXTERNAL_STORAGE)
                                .setPositiveButton("Request", new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        doRequestReadStorage();
                                    }
                                })
                                .setNegativeButton("Cancel", new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                                .create().show();
                    }
                });
            } else {
                // No explanation needed, we can request the permission.
                doRequestReadStorage();
            }
        }

    }

    private void doRequestReadStorage() {
        ActivityCompat.requestPermissions(this,
                new String[]{permission.READ_EXTERNAL_STORAGE},
                ON_PERMISSIONS_REQUESTED);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        if (mCurrentFragment != null && mCurrentFragment.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    @SuppressLint("NewApi")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImageHelper.PICK_IMAGE_CHOOSER_REQUEST_CODE &&
            resultCode == Activity.RESULT_OK) {
            final Uri imageUri = CropImageHelper.getPickImageResultUri(this, data);

            // For API >= 23 we need to check specifically that we have permissions to read external storage,
            // but we don't know if we need to for the URI so the simplest is to try open the stream and see if we get error.
            final boolean[] requirePermissions = {false};
            if (CropImageHelper.isReadExternalStoragePermissionsRequired(this, imageUri)) {

                doRequestPermission(imageUri, requirePermissions);
                //// request permissions and handle the result in onRequestPermissionsResult()
                //requirePermissions = true;
                //mCropImageUri = imageUri;
                //requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            } else {

                mCurrentFragment.setImageUri(imageUri);
            }
        }
    }

    private void doRequestPermission(Uri imageUri, boolean[] requirePermissions) {
        // No explanation needed, we can request the permission.
        // request permissions and handle the result in onRequestPermissionsResult()
        requirePermissions[0] = true;
        mCropImageUri = imageUri;
        doRequestReadStorage();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (ON_PERMISSIONS_REQUESTED == requestCode) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // permission denied, boo! Disable the functionality that depends on this permission.
                finish();
            }
        } else {
            if (mCropImageUri != null && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mCurrentFragment.setImageUri(mCropImageUri);
            } else {
                Toast.makeText(this, "Cancelling, required permissions are not granted", Toast.LENGTH_LONG)
                     .show();
            }
        }
    }

    public void onDrawerOptionClicked(View view) {
        switch (view.getId()) {
            case R.id.drawer_option_load:
                CropImageHelper.startPickImageActivity(this);
                mDrawerLayout.closeDrawers();
                break;
            case R.id.drawer_option_oval:
                setMainFragmentByPreset(CropDemoPreset.CIRCULAR);
                mDrawerLayout.closeDrawers();
                break;
            case R.id.drawer_option_rect:
                setMainFragmentByPreset(CropDemoPreset.RECT);
                mDrawerLayout.closeDrawers();
                break;
            case R.id.drawer_option_customized_overlay:
                setMainFragmentByPreset(CropDemoPreset.CUSTOMIZED_OVERLAY);
                mDrawerLayout.closeDrawers();
                break;
            case R.id.drawer_option_min_max_override:
                setMainFragmentByPreset(CropDemoPreset.MIN_MAX_OVERRIDE);
                mDrawerLayout.closeDrawers();
                break;
            case R.id.drawer_option_scale_center:
                setMainFragmentByPreset(CropDemoPreset.SCALE_CENTER_INSIDE);
                mDrawerLayout.closeDrawers();
                break;
            case R.id.drawer_option_toggle_scale:
                mCropImageViewOptions.scaleType =
                        mCropImageViewOptions.scaleType == ImageView.ScaleType.FIT_CENTER
                        ? ImageView.ScaleType.CENTER_INSIDE : ImageView.ScaleType.FIT_CENTER;
                mCurrentFragment.setCropImageViewOptions(mCropImageViewOptions);
                updateDrawerTogglesByOptions(mCropImageViewOptions);
                break;
            case R.id.drawer_option_toggle_shape:
                mCropImageViewOptions.cropShape =
                        mCropImageViewOptions.cropShape == CropImageView.CropShape.RECTANGLE
                        ? CropImageView.CropShape.OVAL : CropImageView.CropShape.RECTANGLE;
                mCurrentFragment.setCropImageViewOptions(mCropImageViewOptions);
                updateDrawerTogglesByOptions(mCropImageViewOptions);
                break;
            case R.id.drawer_option_toggle_guidelines:
                mCropImageViewOptions.guidelines =
                        mCropImageViewOptions.guidelines == CropImageView.Guidelines.OFF
                        ? CropImageView.Guidelines.ON
                        : mCropImageViewOptions.guidelines == CropImageView.Guidelines.ON
                          ? CropImageView.Guidelines.ON_TOUCH : CropImageView.Guidelines.OFF;
                mCurrentFragment.setCropImageViewOptions(mCropImageViewOptions);
                updateDrawerTogglesByOptions(mCropImageViewOptions);
                break;
            case R.id.drawer_option_toggle_aspect_ratio:
                if (!mCropImageViewOptions.fixAspectRatio) {
                    mCropImageViewOptions.fixAspectRatio = true;
                    mCropImageViewOptions.aspectRatio = new Pair<>(1, 1);
                } else {
                    if (mCropImageViewOptions.aspectRatio.first == 1 &&
                        mCropImageViewOptions.aspectRatio.second == 1) {
                        mCropImageViewOptions.aspectRatio = new Pair<>(4, 3);
                    } else if (mCropImageViewOptions.aspectRatio.first == 4 &&
                               mCropImageViewOptions.aspectRatio.second == 3) {
                        mCropImageViewOptions.aspectRatio = new Pair<>(16, 9);
                    } else if (mCropImageViewOptions.aspectRatio.first == 16 &&
                               mCropImageViewOptions.aspectRatio.second == 9) {
                        mCropImageViewOptions.aspectRatio = new Pair<>(9, 16);
                    } else {
                        mCropImageViewOptions.fixAspectRatio = false;
                    }
                }
                mCurrentFragment.setCropImageViewOptions(mCropImageViewOptions);
                updateDrawerTogglesByOptions(mCropImageViewOptions);
                break;
            case R.id.drawer_option_toggle_show_overlay:
                mCropImageViewOptions.showCropOverlay = !mCropImageViewOptions.showCropOverlay;
                mCurrentFragment.setCropImageViewOptions(mCropImageViewOptions);
                updateDrawerTogglesByOptions(mCropImageViewOptions);
                break;
            case R.id.drawer_option_toggle_show_progress_bar:
                mCropImageViewOptions.showProgressBar = !mCropImageViewOptions.showProgressBar;
                mCurrentFragment.setCropImageViewOptions(mCropImageViewOptions);
                updateDrawerTogglesByOptions(mCropImageViewOptions);
                break;
            default:
                Toast.makeText(this, "Unknown drawer option clicked", Toast.LENGTH_LONG).show();
        }
    }

    private void setMainFragmentByPreset(CropDemoPreset demoPreset) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                       .replace(R.id.container, MainFragment.newInstance(demoPreset))
                       .commit();
    }

    private void updateDrawerTogglesByOptions(CropImageViewOptions options) {
        ((TextView) findViewById(R.id.drawer_option_toggle_scale)).setText(getResources()
                .getString(R.string.drawer_option_toggle_scale, options.scaleType.name()));
        ((TextView) findViewById(R.id.drawer_option_toggle_shape)).setText(getResources()
                .getString(R.string.drawer_option_toggle_shape, options.cropShape.name()));
        ((TextView) findViewById(R.id.drawer_option_toggle_guidelines)).setText(getResources()
                .getString(R.string.drawer_option_toggle_guidelines, options.guidelines.name()));
        ((TextView) findViewById(R.id.drawer_option_toggle_show_overlay)).setText(getResources()
                .getString(R.string.drawer_option_toggle_show_overlay, Boolean
                        .toString(options.showCropOverlay)));
        ((TextView) findViewById(R.id.drawer_option_toggle_show_progress_bar))
                .setText(getResources()
                        .getString(R.string.drawer_option_toggle_show_progress_bar, Boolean
                                .toString(options.showProgressBar)));

        String aspectRatio = "FREE";
        if (options.fixAspectRatio) {
            aspectRatio = options.aspectRatio.first + ":" + options.aspectRatio.second;
        }
        ((TextView) findViewById(R.id.drawer_option_toggle_aspect_ratio)).setText(getResources()
                .getString(R.string.drawer_option_toggle_aspect_ratio, aspectRatio));
    }
}
