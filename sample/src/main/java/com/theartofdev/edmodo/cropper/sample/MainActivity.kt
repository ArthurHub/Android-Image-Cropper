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
package com.theartofdev.edmodo.cropper.sample

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Pair
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.example.croppersample.R
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImage.getPickImageResultUri
import com.theartofdev.edmodo.cropper.CropImage.isExplicitCameraPermissionRequired
import com.theartofdev.edmodo.cropper.CropImage.isReadExternalStoragePermissionsRequired
import com.theartofdev.edmodo.cropper.CropImage.startPickImageActivity
import com.theartofdev.edmodo.cropper.CropImageView
import com.theartofdev.edmodo.cropper.CropImageView.CropShape
import com.theartofdev.edmodo.cropper.CropImageView.Guidelines

class MainActivity : AppCompatActivity() {
    // region: Fields and Consts
    var mDrawerLayout: DrawerLayout? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null
    private var mCurrentFragment: MainFragment? = null
    private var mCropImageUri: Uri? = null
    private var mCropImageViewOptions = CropImageViewOptions()

    // endregion
    fun setCurrentFragment(fragment: MainFragment?) {
        mCurrentFragment = fragment
    }

    fun setCurrentOptions(options: CropImageViewOptions) {
        mCropImageViewOptions = options
        updateDrawerTogglesByOptions(options)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)
        mDrawerLayout = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        mDrawerToggle = ActionBarDrawerToggle(
                this, mDrawerLayout, R.string.main_drawer_open, R.string.main_drawer_close)
        mDrawerToggle!!.isDrawerIndicatorEnabled = true
        mDrawerLayout!!.setDrawerListener(mDrawerToggle)
        if (savedInstanceState == null) {
            setMainFragmentByPreset(CropDemoPreset.RECT)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle!!.syncState()
        mCurrentFragment!!.updateCurrentCropViewOptions()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mDrawerToggle!!.onOptionsItemSelected(item)) {
            return true
        }
        return if (mCurrentFragment != null && mCurrentFragment!!.onOptionsItemSelected(item)) {
            true
        } else super.onOptionsItemSelected(item)
    }

    @SuppressLint("NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE
                && resultCode == RESULT_OK) {
            val imageUri = getPickImageResultUri(this, data)

            // For API >= 23 we need to check specifically that we have permissions to read external
            // storage,
            // but we don't know if we need to for the URI so the simplest is to try open the stream and
            // see if we get error.
            var requirePermissions = false
            if (isReadExternalStoragePermissionsRequired(this, imageUri!!)) {

                // request permissions and handle the result in onRequestPermissionsResult()
                requirePermissions = true
                mCropImageUri = imageUri
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE)
            } else {
                mCurrentFragment!!.setImageUri(imageUri)
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == CropImage.CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPickImageActivity(this)
            } else {
                Toast.makeText(this, "Cancelling, required permissions are not granted", Toast.LENGTH_LONG)
                        .show()
            }
        }
        if (requestCode == CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE) {
            if (mCropImageUri != null && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mCurrentFragment!!.setImageUri(mCropImageUri)
            } else {
                Toast.makeText(this, "Cancelling, required permissions are not granted", Toast.LENGTH_LONG)
                        .show()
            }
        }
    }

    @SuppressLint("NewApi")
    fun onDrawerOptionClicked(view: View) {
        when (view.id) {
            R.id.drawer_option_load -> {
                if (isExplicitCameraPermissionRequired(this)) {
                    requestPermissions(arrayOf(Manifest.permission.CAMERA),
                            CropImage.CAMERA_CAPTURE_PERMISSIONS_REQUEST_CODE)
                } else {
                    startPickImageActivity(this)
                }
                mDrawerLayout!!.closeDrawers()
            }
            R.id.drawer_option_oval -> {
                setMainFragmentByPreset(CropDemoPreset.CIRCULAR)
                mDrawerLayout!!.closeDrawers()
            }
            R.id.drawer_option_rect -> {
                setMainFragmentByPreset(CropDemoPreset.RECT)
                mDrawerLayout!!.closeDrawers()
            }
            R.id.drawer_option_customized_overlay -> {
                setMainFragmentByPreset(CropDemoPreset.CUSTOMIZED_OVERLAY)
                mDrawerLayout!!.closeDrawers()
            }
            R.id.drawer_option_min_max_override -> {
                setMainFragmentByPreset(CropDemoPreset.MIN_MAX_OVERRIDE)
                mDrawerLayout!!.closeDrawers()
            }
            R.id.drawer_option_scale_center -> {
                setMainFragmentByPreset(CropDemoPreset.SCALE_CENTER_INSIDE)
                mDrawerLayout!!.closeDrawers()
            }
            R.id.drawer_option_toggle_scale -> {
                mCropImageViewOptions.scaleType = if (mCropImageViewOptions.scaleType === CropImageView.ScaleType.FIT_CENTER) CropImageView.ScaleType.CENTER_INSIDE else if (mCropImageViewOptions.scaleType === CropImageView.ScaleType.CENTER_INSIDE) CropImageView.ScaleType.CENTER else if (mCropImageViewOptions.scaleType === CropImageView.ScaleType.CENTER) CropImageView.ScaleType.CENTER_CROP else CropImageView.ScaleType.FIT_CENTER
                mCurrentFragment!!.setCropImageViewOptions(mCropImageViewOptions)
                updateDrawerTogglesByOptions(mCropImageViewOptions)
            }
            R.id.drawer_option_toggle_shape -> {
                mCropImageViewOptions.cropShape = if (mCropImageViewOptions.cropShape === CropShape.RECTANGLE) CropShape.OVAL else CropShape.RECTANGLE
                mCurrentFragment!!.setCropImageViewOptions(mCropImageViewOptions)
                updateDrawerTogglesByOptions(mCropImageViewOptions)
            }
            R.id.drawer_option_toggle_guidelines -> {
                mCropImageViewOptions.guidelines = if (mCropImageViewOptions.guidelines === Guidelines.OFF) Guidelines.ON else if (mCropImageViewOptions.guidelines === Guidelines.ON) Guidelines.ON_TOUCH else Guidelines.OFF
                mCurrentFragment!!.setCropImageViewOptions(mCropImageViewOptions)
                updateDrawerTogglesByOptions(mCropImageViewOptions)
            }
            R.id.drawer_option_toggle_aspect_ratio -> {
                if (!mCropImageViewOptions.fixAspectRatio) {
                    mCropImageViewOptions.fixAspectRatio = true
                    mCropImageViewOptions.aspectRatio = Pair(1, 1)
                } else {
                    if (mCropImageViewOptions.aspectRatio.first == 1
                            && mCropImageViewOptions.aspectRatio.second == 1) {
                        mCropImageViewOptions.aspectRatio = Pair(4, 3)
                    } else if (mCropImageViewOptions.aspectRatio.first == 4
                            && mCropImageViewOptions.aspectRatio.second == 3) {
                        mCropImageViewOptions.aspectRatio = Pair(16, 9)
                    } else if (mCropImageViewOptions.aspectRatio.first == 16
                            && mCropImageViewOptions.aspectRatio.second == 9) {
                        mCropImageViewOptions.aspectRatio = Pair(9, 16)
                    } else {
                        mCropImageViewOptions.fixAspectRatio = false
                    }
                }
                mCurrentFragment!!.setCropImageViewOptions(mCropImageViewOptions)
                updateDrawerTogglesByOptions(mCropImageViewOptions)
            }
            R.id.drawer_option_toggle_auto_zoom -> {
                mCropImageViewOptions.autoZoomEnabled = !mCropImageViewOptions.autoZoomEnabled
                mCurrentFragment!!.setCropImageViewOptions(mCropImageViewOptions)
                updateDrawerTogglesByOptions(mCropImageViewOptions)
            }
            R.id.drawer_option_toggle_max_zoom -> {
                mCropImageViewOptions.maxZoomLevel = if (mCropImageViewOptions.maxZoomLevel == 4) 8 else if (mCropImageViewOptions.maxZoomLevel == 8) 2 else 4
                mCurrentFragment!!.setCropImageViewOptions(mCropImageViewOptions)
                updateDrawerTogglesByOptions(mCropImageViewOptions)
            }
            R.id.drawer_option_set_initial_crop_rect -> {
                mCurrentFragment!!.setInitialCropRect()
                mDrawerLayout!!.closeDrawers()
            }
            R.id.drawer_option_reset_crop_rect -> {
                mCurrentFragment!!.resetCropRect()
                mDrawerLayout!!.closeDrawers()
            }
            R.id.drawer_option_toggle_multitouch -> {
                mCropImageViewOptions.multitouch = !mCropImageViewOptions.multitouch
                mCurrentFragment!!.setCropImageViewOptions(mCropImageViewOptions)
                updateDrawerTogglesByOptions(mCropImageViewOptions)
            }
            R.id.drawer_option_toggle_show_overlay -> {
                mCropImageViewOptions.showCropOverlay = !mCropImageViewOptions.showCropOverlay
                mCurrentFragment!!.setCropImageViewOptions(mCropImageViewOptions)
                updateDrawerTogglesByOptions(mCropImageViewOptions)
            }
            R.id.drawer_option_toggle_show_progress_bar -> {
                mCropImageViewOptions.showProgressBar = !mCropImageViewOptions.showProgressBar
                mCurrentFragment!!.setCropImageViewOptions(mCropImageViewOptions)
                updateDrawerTogglesByOptions(mCropImageViewOptions)
            }
            else -> Toast.makeText(this, "Unknown drawer option clicked", Toast.LENGTH_LONG).show()
        }
    }

    private fun setMainFragmentByPreset(demoPreset: CropDemoPreset) {
        val fragmentManager = supportFragmentManager
        fragmentManager
                .beginTransaction()
                .replace(R.id.container, MainFragment.newInstance(demoPreset))
                .commit()
    }

    private fun updateDrawerTogglesByOptions(options: CropImageViewOptions) {
        (findViewById<View>(R.id.drawer_option_toggle_scale) as TextView).text = resources
                .getString(R.string.drawer_option_toggle_scale, options.scaleType.name)
        (findViewById<View>(R.id.drawer_option_toggle_shape) as TextView).text = resources
                .getString(R.string.drawer_option_toggle_shape, options.cropShape.name)
        (findViewById<View>(R.id.drawer_option_toggle_guidelines) as TextView).text = resources
                .getString(R.string.drawer_option_toggle_guidelines, options.guidelines.name)
        (findViewById<View>(R.id.drawer_option_toggle_multitouch) as TextView).text = resources
                .getString(
                        R.string.drawer_option_toggle_multitouch,
                        java.lang.Boolean.toString(options.multitouch))
        (findViewById<View>(R.id.drawer_option_toggle_show_overlay) as TextView).text = resources
                .getString(
                        R.string.drawer_option_toggle_show_overlay,
                        java.lang.Boolean.toString(options.showCropOverlay))
        (findViewById<View>(R.id.drawer_option_toggle_show_progress_bar) as TextView).text = resources
                .getString(
                        R.string.drawer_option_toggle_show_progress_bar,
                        java.lang.Boolean.toString(options.showProgressBar))
        var aspectRatio = "FREE"
        if (options.fixAspectRatio) {
            aspectRatio = options.aspectRatio.first.toString() + ":" + options.aspectRatio.second
        }
        (findViewById<View>(R.id.drawer_option_toggle_aspect_ratio) as TextView).text = resources.getString(R.string.drawer_option_toggle_aspect_ratio, aspectRatio)
        (findViewById<View>(R.id.drawer_option_toggle_auto_zoom) as TextView).text = resources
                .getString(
                        R.string.drawer_option_toggle_auto_zoom,
                        if (options.autoZoomEnabled) "Enabled" else "Disabled")
        (findViewById<View>(R.id.drawer_option_toggle_max_zoom) as TextView).text = resources.getString(R.string.drawer_option_toggle_max_zoom, options.maxZoomLevel)
    }
}