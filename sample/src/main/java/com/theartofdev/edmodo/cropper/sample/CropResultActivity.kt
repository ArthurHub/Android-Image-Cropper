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

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.croppersample.R

class CropResultActivity : Activity() {
    private var imageView: ImageView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_crop_result)
        imageView = findViewById<View>(R.id.resultImageView) as ImageView
        imageView!!.setBackgroundResource(R.drawable.backdrop)
        val intent = intent
        if (mImage != null) {
            imageView!!.setImageBitmap(mImage)
            val sampleSize = intent.getIntExtra("SAMPLE_SIZE", 1)
            val ratio = (10 * mImage!!.width / mImage!!.height.toDouble()).toInt() / 10.0
            var byteCount = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                byteCount = mImage!!.byteCount / 1024
            }
            val desc = ("("
                    + mImage!!.width
                    + ", "
                    + mImage!!.height
                    + "), Sample: "
                    + sampleSize
                    + ", Ratio: "
                    + ratio
                    + ", Bytes: "
                    + byteCount
                    + "K")
            (findViewById<View>(R.id.resultImageText) as TextView).text = desc
        } else {
            val imageUri = intent.getParcelableExtra<Uri>("URI")
            if (imageUri != null) {
                imageView!!.setImageURI(imageUri)
            } else {
                Toast.makeText(this, "No image is set to show", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onBackPressed() {
        releaseBitmap()
        super.onBackPressed()
    }

    fun onImageViewClicked(view: View?) {
        releaseBitmap()
        finish()
    }

    private fun releaseBitmap() {
        if (mImage != null) {
            mImage!!.recycle()
            mImage = null
        }
    }

    companion object {
        /** The image to show in the activity.  */
        var mImage: Bitmap? = null
    }
}