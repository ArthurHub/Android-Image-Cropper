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

import android.util.Pair
import com.theartofdev.edmodo.cropper.CropImageView
import com.theartofdev.edmodo.cropper.CropImageView.CropShape
import com.theartofdev.edmodo.cropper.CropImageView.Guidelines

/** The crop image view options that can be changed live.  */
class CropImageViewOptions {
    var scaleType = CropImageView.ScaleType.CENTER_INSIDE
    var cropShape = CropShape.RECTANGLE
    var guidelines = Guidelines.ON_TOUCH
    var aspectRatio = Pair(1, 1)
    var autoZoomEnabled = false
    var maxZoomLevel = 0
    var fixAspectRatio = false
    var multitouch = false
    var showCropOverlay = false
    var showProgressBar = false
    var flipHorizontally = false
    var flipVertically = false
}