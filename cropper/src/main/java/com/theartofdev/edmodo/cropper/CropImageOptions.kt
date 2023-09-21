// "Therefore those skilled at the unorthodox
// are infinite as heaven and earth;
// inexhaustible as the great rivers.
// When they come to an end;
// they begin again;
// like the days and months;
// they die and are reborn;
// like the four seasons."
//
// - Sun Tsu;
// "The Art of War"
package com.theartofdev.edmodo.cropper

import android.content.res.Resources
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.TypedValue
import com.theartofdev.edmodo.cropper.CropImageView
import com.theartofdev.edmodo.cropper.CropImageView.*

/**
 * All the possible options that can be set to customize crop image.<br></br>
 * Initialized with default values.
 */
class CropImageOptions : Parcelable {
    /** The shape of the cropping window.  */
    var cropShape: CropShape

    /**
     * An edge of the crop window will snap to the corresponding edge of a specified bounding box when
     * the crop window edge is less than or equal to this distance (in pixels) away from the bounding
     * box edge. (in pixels)
     */
    var snapRadius: Float

    /**
     * The radius of the touchable area around the handle. (in pixels)<br></br>
     * We are basing this value off of the recommended 48dp Rhythm.<br></br>
     * See: http://developer.android.com/design/style/metrics-grids.html#48dp-rhythm
     */
    var touchRadius: Float

    /** whether the guidelines should be on, off, or only showing when resizing.  */
    var guidelines: Guidelines

    /** The initial scale type of the image in the crop image view  */
    var scaleType: CropImageView.ScaleType

    /**
     * if to show crop overlay UI what contains the crop window UI surrounded by background over the
     * cropping image.<br></br>
     * default: true, may disable for animation or frame transition.
     */
    var showCropOverlay: Boolean

    /**
     * if to show progress bar when image async loading/cropping is in progress.<br></br>
     * default: true, disable to provide custom progress bar UI.
     */
    var showProgressBar: Boolean

    /**
     * if auto-zoom functionality is enabled.<br></br>
     * default: true.
     */
    var autoZoomEnabled: Boolean

    /** if multi-touch should be enabled on the crop box default: false  */
    var multiTouchEnabled: Boolean

    /** The max zoom allowed during cropping.  */
    var maxZoom: Int

    /**
     * The initial crop window padding from image borders in percentage of the cropping image
     * dimensions.
     */
    var initialCropWindowPaddingRatio: Float

    /** whether the width to height aspect ratio should be maintained or free to change.  */
    var fixAspectRatio: Boolean

    /** the X value of the aspect ratio.  */
    var aspectRatioX: Int

    /** the Y value of the aspect ratio.  */
    var aspectRatioY: Int

    /** the thickness of the guidelines lines in pixels. (in pixels)  */
    var borderLineThickness: Float

    /** the color of the guidelines lines  */
    var borderLineColor: Int

    /** thickness of the corner line. (in pixels)  */
    var borderCornerThickness: Float

    /** the offset of corner line from crop window border. (in pixels)  */
    var borderCornerOffset: Float

    /** the length of the corner line away from the corner. (in pixels)  */
    var borderCornerLength: Float

    /** the color of the corner line  */
    var borderCornerColor: Int

    /** the thickness of the guidelines lines. (in pixels)  */
    var guidelinesThickness: Float

    /** the color of the guidelines lines  */
    var guidelinesColor: Int

    /**
     * the color of the overlay background around the crop window cover the image parts not in the
     * crop window.
     */
    var backgroundColor: Int

    /** the min width the crop window is allowed to be. (in pixels)  */
    var minCropWindowWidth: Int

    /** the min height the crop window is allowed to be. (in pixels)  */
    var minCropWindowHeight: Int

    /**
     * the min width the resulting cropping image is allowed to be, affects the cropping window
     * limits. (in pixels)
     */
    var minCropResultWidth: Int

    /**
     * the min height the resulting cropping image is allowed to be, affects the cropping window
     * limits. (in pixels)
     */
    var minCropResultHeight: Int

    /**
     * the max width the resulting cropping image is allowed to be, affects the cropping window
     * limits. (in pixels)
     */
    var maxCropResultWidth: Int

    /**
     * the max height the resulting cropping image is allowed to be, affects the cropping window
     * limits. (in pixels)
     */
    var maxCropResultHeight: Int

    /** the title of the [CropImageActivity]  */
    var activityTitle: CharSequence?

    /** the color to use for action bar items icons  */
    var activityMenuIconColor: Int

    /** the Android Uri to save the cropped image to  */
    var outputUri: Uri?

    /** the compression format to use when writing the image  */
    var outputCompressFormat: CompressFormat

    /** the quality (if applicable) to use when writing the image (0 - 100)  */
    var outputCompressQuality: Int

    /** the width to resize the cropped image to (see options)  */
    var outputRequestWidth: Int

    /** the height to resize the cropped image to (see options)  */
    var outputRequestHeight: Int

    /** the resize method to use on the cropped bitmap (see options documentation)  */
    var outputRequestSizeOptions: RequestSizeOptions

    /** if the result of crop image activity should not save the cropped image bitmap  */
    var noOutputImage: Boolean

    /** the initial rectangle to set on the cropping image after loading  */
    var initialCropWindowRectangle: Rect?

    /** the initial rotation to set on the cropping image after loading (0-360 degrees clockwise)  */
    var initialRotation: Int

    /** if to allow (all) rotation during cropping (activity)  */
    var allowRotation: Boolean

    /** if to allow (all) flipping during cropping (activity)  */
    var allowFlipping: Boolean

    /** if to allow counter-clockwise rotation during cropping (activity)  */
    var allowCounterRotation: Boolean

    /** the amount of degrees to rotate clockwise or counter-clockwise  */
    var rotationDegrees: Int

    /** whether the image should be flipped horizontally  */
    var flipHorizontally: Boolean

    /** whether the image should be flipped vertically  */
    var flipVertically: Boolean

    /** optional, the text of the crop menu crop button  */
    var cropMenuCropButtonTitle: CharSequence?

    /** optional image resource to be used for crop menu crop icon instead of text  */
    var cropMenuCropButtonIcon: Int

    /** Init options with defaults.  */
    constructor() {
        val dm = Resources.getSystem().displayMetrics
        cropShape = CropShape.RECTANGLE
        snapRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm)
        touchRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, dm)
        guidelines = Guidelines.ON_TOUCH
        scaleType = CropImageView.ScaleType.FIT_CENTER
        showCropOverlay = true
        showProgressBar = true
        autoZoomEnabled = true
        multiTouchEnabled = false
        maxZoom = 4
        initialCropWindowPaddingRatio = 0.1f
        fixAspectRatio = false
        aspectRatioX = 1
        aspectRatioY = 1
        borderLineThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm)
        borderLineColor = Color.argb(170, 255, 255, 255)
        borderCornerThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, dm)
        borderCornerOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, dm)
        borderCornerLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f, dm)
        borderCornerColor = Color.WHITE
        guidelinesThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, dm)
        guidelinesColor = Color.argb(170, 255, 255, 255)
        backgroundColor = Color.argb(119, 0, 0, 0)
        minCropWindowWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42f, dm).toInt()
        minCropWindowHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42f, dm).toInt()
        minCropResultWidth = 40
        minCropResultHeight = 40
        maxCropResultWidth = 99999
        maxCropResultHeight = 99999
        activityTitle = ""
        activityMenuIconColor = 0
        outputUri = Uri.EMPTY
        outputCompressFormat = CompressFormat.JPEG
        outputCompressQuality = 90
        outputRequestWidth = 0
        outputRequestHeight = 0
        outputRequestSizeOptions = RequestSizeOptions.NONE
        noOutputImage = false
        initialCropWindowRectangle = null
        initialRotation = -1
        allowRotation = true
        allowFlipping = true
        allowCounterRotation = false
        rotationDegrees = 90
        flipHorizontally = false
        flipVertically = false
        cropMenuCropButtonTitle = null
        cropMenuCropButtonIcon = 0
    }

    /** Create object from parcel.  */
    protected constructor(`in`: Parcel) {
        cropShape = CropShape.values()[`in`.readInt()]
        snapRadius = `in`.readFloat()
        touchRadius = `in`.readFloat()
        guidelines = Guidelines.values()[`in`.readInt()]
        scaleType = CropImageView.ScaleType.values()[`in`.readInt()]
        showCropOverlay = `in`.readByte().toInt() != 0
        showProgressBar = `in`.readByte().toInt() != 0
        autoZoomEnabled = `in`.readByte().toInt() != 0
        multiTouchEnabled = `in`.readByte().toInt() != 0
        maxZoom = `in`.readInt()
        initialCropWindowPaddingRatio = `in`.readFloat()
        fixAspectRatio = `in`.readByte().toInt() != 0
        aspectRatioX = `in`.readInt()
        aspectRatioY = `in`.readInt()
        borderLineThickness = `in`.readFloat()
        borderLineColor = `in`.readInt()
        borderCornerThickness = `in`.readFloat()
        borderCornerOffset = `in`.readFloat()
        borderCornerLength = `in`.readFloat()
        borderCornerColor = `in`.readInt()
        guidelinesThickness = `in`.readFloat()
        guidelinesColor = `in`.readInt()
        backgroundColor = `in`.readInt()
        minCropWindowWidth = `in`.readInt()
        minCropWindowHeight = `in`.readInt()
        minCropResultWidth = `in`.readInt()
        minCropResultHeight = `in`.readInt()
        maxCropResultWidth = `in`.readInt()
        maxCropResultHeight = `in`.readInt()
        activityTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(`in`)
        activityMenuIconColor = `in`.readInt()
        outputUri = `in`.readParcelable(Uri::class.java.classLoader)
        outputCompressFormat = CompressFormat.valueOf(`in`.readString()!!)
        outputCompressQuality = `in`.readInt()
        outputRequestWidth = `in`.readInt()
        outputRequestHeight = `in`.readInt()
        outputRequestSizeOptions = RequestSizeOptions.values()[`in`.readInt()]
        noOutputImage = `in`.readByte().toInt() != 0
        initialCropWindowRectangle = `in`.readParcelable(Rect::class.java.classLoader)
        initialRotation = `in`.readInt()
        allowRotation = `in`.readByte().toInt() != 0
        allowFlipping = `in`.readByte().toInt() != 0
        allowCounterRotation = `in`.readByte().toInt() != 0
        rotationDegrees = `in`.readInt()
        flipHorizontally = `in`.readByte().toInt() != 0
        flipVertically = `in`.readByte().toInt() != 0
        cropMenuCropButtonTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(`in`)
        cropMenuCropButtonIcon = `in`.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(cropShape.ordinal)
        dest.writeFloat(snapRadius)
        dest.writeFloat(touchRadius)
        dest.writeInt(guidelines.ordinal)
        dest.writeInt(scaleType.ordinal)
        dest.writeByte((if (showCropOverlay) 1 else 0).toByte())
        dest.writeByte((if (showProgressBar) 1 else 0).toByte())
        dest.writeByte((if (autoZoomEnabled) 1 else 0).toByte())
        dest.writeByte((if (multiTouchEnabled) 1 else 0).toByte())
        dest.writeInt(maxZoom)
        dest.writeFloat(initialCropWindowPaddingRatio)
        dest.writeByte((if (fixAspectRatio) 1 else 0).toByte())
        dest.writeInt(aspectRatioX)
        dest.writeInt(aspectRatioY)
        dest.writeFloat(borderLineThickness)
        dest.writeInt(borderLineColor)
        dest.writeFloat(borderCornerThickness)
        dest.writeFloat(borderCornerOffset)
        dest.writeFloat(borderCornerLength)
        dest.writeInt(borderCornerColor)
        dest.writeFloat(guidelinesThickness)
        dest.writeInt(guidelinesColor)
        dest.writeInt(backgroundColor)
        dest.writeInt(minCropWindowWidth)
        dest.writeInt(minCropWindowHeight)
        dest.writeInt(minCropResultWidth)
        dest.writeInt(minCropResultHeight)
        dest.writeInt(maxCropResultWidth)
        dest.writeInt(maxCropResultHeight)
        TextUtils.writeToParcel(activityTitle, dest, flags)
        dest.writeInt(activityMenuIconColor)
        dest.writeParcelable(outputUri, flags)
        dest.writeString(outputCompressFormat.name)
        dest.writeInt(outputCompressQuality)
        dest.writeInt(outputRequestWidth)
        dest.writeInt(outputRequestHeight)
        dest.writeInt(outputRequestSizeOptions.ordinal)
        dest.writeInt(if (noOutputImage) 1 else 0)
        dest.writeParcelable(initialCropWindowRectangle, flags)
        dest.writeInt(initialRotation)
        dest.writeByte((if (allowRotation) 1 else 0).toByte())
        dest.writeByte((if (allowFlipping) 1 else 0).toByte())
        dest.writeByte((if (allowCounterRotation) 1 else 0).toByte())
        dest.writeInt(rotationDegrees)
        dest.writeByte((if (flipHorizontally) 1 else 0).toByte())
        dest.writeByte((if (flipVertically) 1 else 0).toByte())
        TextUtils.writeToParcel(cropMenuCropButtonTitle, dest, flags)
        dest.writeInt(cropMenuCropButtonIcon)
    }

    override fun describeContents(): Int {
        return 0
    }

    /**
     * Validate all the options are withing valid range.
     *
     * @throws IllegalArgumentException if any of the options is not valid
     */
    fun validate() {
        require(maxZoom >= 0) { "Cannot set max zoom to a number < 1" }
        require(touchRadius >= 0) { "Cannot set touch radius value to a number <= 0 " }
        require(!(initialCropWindowPaddingRatio < 0 || initialCropWindowPaddingRatio >= 0.5)) { "Cannot set initial crop window padding value to a number < 0 or >= 0.5" }
        require(aspectRatioX > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
        require(aspectRatioY > 0) { "Cannot set aspect ratio value to a number less than or equal to 0." }
        require(borderLineThickness >= 0) { "Cannot set line thickness value to a number less than 0." }
        require(borderCornerThickness >= 0) { "Cannot set corner thickness value to a number less than 0." }
        require(guidelinesThickness >= 0) { "Cannot set guidelines thickness value to a number less than 0." }
        require(minCropWindowHeight >= 0) { "Cannot set min crop window height value to a number < 0 " }
        require(minCropResultWidth >= 0) { "Cannot set min crop result width value to a number < 0 " }
        require(minCropResultHeight >= 0) { "Cannot set min crop result height value to a number < 0 " }
        require(maxCropResultWidth >= minCropResultWidth) { "Cannot set max crop result width to smaller value than min crop result width" }
        require(maxCropResultHeight >= minCropResultHeight) { "Cannot set max crop result height to smaller value than min crop result height" }
        require(outputRequestWidth >= 0) { "Cannot set request width value to a number < 0 " }
        require(outputRequestHeight >= 0) { "Cannot set request height value to a number < 0 " }
        require(!(rotationDegrees < 0 || rotationDegrees > 360)) { "Cannot set rotation degrees value to a number < 0 or > 360" }
    }

    companion object {
        val CREATOR: Parcelable.Creator<CropImageOptions?> = object : Parcelable.Creator<CropImageOptions?> {
            override fun createFromParcel(`in`: Parcel): CropImageOptions {
                return CropImageOptions(`in`)
            }

            override fun newArray(size: Int): Array<CropImageOptions?> {
                return arrayOfNulls(size)
            }
        }
    }
}