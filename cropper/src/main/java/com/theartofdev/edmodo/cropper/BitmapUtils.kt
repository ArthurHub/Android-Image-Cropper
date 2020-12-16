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
package com.theartofdev.edmodo.cropper

import android.content.ContentResolver
import android.content.Context
import android.graphics.*
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.util.Log
import android.util.Pair
import androidx.exifinterface.media.ExifInterface
import com.theartofdev.edmodo.cropper.CropImageView.RequestSizeOptions
import java.io.*
import java.lang.ref.WeakReference
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext

/** Utility class that deals with operations with an ImageView.  */
internal object BitmapUtils {
    val EMPTY_RECT = Rect()
    val EMPTY_RECT_F = RectF()

    /** Reusable rectangle for general internal usage  */
    val RECT = RectF()

    /** Reusable point for general internal usage  */
    val POINTS = FloatArray(6)

    /** Reusable point for general internal usage  */
    val POINTS2 = FloatArray(6)

    /** Used to know the max texture size allowed to be rendered  */
    private var mMaxTextureSize = 0

    /** used to save bitmaps during state save and restore so not to reload them.  */
    var mStateBitmap: Pair<String, WeakReference<Bitmap?>>? = null

    /**
     * Rotate the given image by reading the Exif value of the image (uri).<br></br>
     * If no rotation is required the image will not be rotated.<br></br>
     * New bitmap is created and the old one is recycled.
     */
    fun rotateBitmapByExif(bitmap: Bitmap?, context: Context, uri: Uri?): RotateBitmapResult {
        var ei: ExifInterface? = null
        try {
            val `is` = context.contentResolver.openInputStream(uri!!)
            if (`is` != null) {
                ei = ExifInterface(`is`)
                `is`.close()
            }
        } catch (ignored: Exception) {
        }
        return if (ei != null) rotateBitmapByExif(bitmap, ei) else RotateBitmapResult(bitmap, 0)
    }

    /**
     * Rotate the given image by given Exif value.<br></br>
     * If no rotation is required the image will not be rotated.<br></br>
     * New bitmap is created and the old one is recycled.
     */
    fun rotateBitmapByExif(bitmap: Bitmap?, exif: ExifInterface): RotateBitmapResult {
        val degrees: Int
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        return RotateBitmapResult(bitmap, degrees)
    }

    /** Decode bitmap from stream using sampling to get bitmap with the requested limit.  */
    fun decodeSampledBitmap(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): BitmapSampled {
        return try {
            val resolver = context.contentResolver

            // First decode with inJustDecodeBounds=true to check dimensions
            val options = decodeImageForOption(resolver, uri)
            if (options.outWidth == -1 && options.outHeight == -1) throw RuntimeException("File is not a picture")

            // Calculate inSampleSize
            options.inSampleSize = Math.max(
                    calculateInSampleSizeByReqestedSize(
                            options.outWidth, options.outHeight, reqWidth, reqHeight),
                    calculateInSampleSizeByMaxTextureSize(options.outWidth, options.outHeight))

            // Decode bitmap with inSampleSize set
            val bitmap = decodeImage(resolver, uri, options)
            BitmapSampled(bitmap, options.inSampleSize)
        } catch (e: Exception) {
            throw RuntimeException(
                    """
                        Failed to load sampled bitmap: $uri
                        ${e.message}
                        """.trimIndent(), e)
        }
    }

    /**
     * Crop image bitmap from given bitmap using the given points in the original bitmap and the given
     * rotation.<br></br>
     * if the rotation is not 0,90,180 or 270 degrees then we must first crop a larger area of the
     * image that contains the requires rectangle, rotate and then crop again a sub rectangle.<br></br>
     * If crop fails due to OOM we scale the cropping image by 0.5 every time it fails until it is
     * small enough.
     */
    fun cropBitmapObjectHandleOOM(
            bitmap: Bitmap,
            points: FloatArray,
            degreesRotated: Int,
            fixAspectRatio: Boolean,
            aspectRatioX: Int,
            aspectRatioY: Int,
            flipHorizontally: Boolean,
            flipVertically: Boolean): BitmapSampled {
        var scale = 1
        while (true) {
            try {
                val cropBitmap = cropBitmapObjectWithScale(
                        bitmap,
                        points,
                        degreesRotated,
                        fixAspectRatio,
                        aspectRatioX,
                        aspectRatioY,
                        1 / scale.toFloat(),
                        flipHorizontally,
                        flipVertically)
                return BitmapSampled(cropBitmap, scale)
            } catch (e: OutOfMemoryError) {
                scale *= 2
                if (scale > 8) {
                    throw e
                }
            }
        }
    }

    /**
     * Crop image bitmap from given bitmap using the given points in the original bitmap and the given
     * rotation.<br></br>
     * if the rotation is not 0,90,180 or 270 degrees then we must first crop a larger area of the
     * image that contains the requires rectangle, rotate and then crop again a sub rectangle.
     *
     * @param scale how much to scale the cropped image part, use 0.5 to lower the image by half (OOM
     * handling)
     */
    private fun cropBitmapObjectWithScale(
            bitmap: Bitmap,
            points: FloatArray,
            degreesRotated: Int,
            fixAspectRatio: Boolean,
            aspectRatioX: Int,
            aspectRatioY: Int,
            scale: Float,
            flipHorizontally: Boolean,
            flipVertically: Boolean): Bitmap? {

        // get the rectangle in original image that contains the required cropped area (larger for non
        // rectangular crop)
        val rect = getRectFromPoints(
                points,
                bitmap.width,
                bitmap.height,
                fixAspectRatio,
                aspectRatioX,
                aspectRatioY)

        // crop and rotate the cropped image in one operation
        val matrix = Matrix()
        matrix.setRotate(degreesRotated.toFloat(), (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat())
        matrix.postScale(if (flipHorizontally) -scale else scale, if (flipVertically) -scale else scale)
        var result = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height(), matrix, true)
        if (result == bitmap) {
            // corner case when all bitmap is selected, no worth optimizing for it
            result = bitmap.copy(bitmap.config, false)
        }

        // rotating by 0, 90, 180 or 270 degrees doesn't require extra cropping
        if (degreesRotated % 90 != 0) {

            // extra crop because non rectangular crop cannot be done directly on the image without
            // rotating first
            result = cropForRotatedImage(
                    result, points, rect, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY)
        }
        return result
    }

    /**
     * Crop image bitmap from URI by decoding it with specific width and height to down-sample if
     * required.<br></br>
     * Additionally if OOM is thrown try to increase the sampling (2,4,8).
     */
    fun cropBitmap(
            context: Context,
            loadedImageUri: Uri,
            points: FloatArray,
            degreesRotated: Int,
            orgWidth: Int,
            orgHeight: Int,
            fixAspectRatio: Boolean,
            aspectRatioX: Int,
            aspectRatioY: Int,
            reqWidth: Int,
            reqHeight: Int,
            flipHorizontally: Boolean,
            flipVertically: Boolean): BitmapSampled {
        var sampleMulti = 1
        while (true) {
            try {
                // if successful, just return the resulting bitmap
                return cropBitmap(
                        context,
                        loadedImageUri,
                        points,
                        degreesRotated,
                        orgWidth,
                        orgHeight,
                        fixAspectRatio,
                        aspectRatioX,
                        aspectRatioY,
                        reqWidth,
                        reqHeight,
                        flipHorizontally,
                        flipVertically,
                        sampleMulti)
            } catch (e: OutOfMemoryError) {
                // if OOM try to increase the sampling to lower the memory usage
                sampleMulti *= 2
                if (sampleMulti > 16) {
                    throw RuntimeException(
                            """
                                Failed to handle OOM by sampling ($sampleMulti): $loadedImageUri
                                ${e.message}
                                """.trimIndent(),
                            e)
                }
            }
        }
    }

    /** Get left value of the bounding rectangle of the given points.  */
    fun getRectLeft(points: FloatArray): Float {
        return Math.min(Math.min(Math.min(points[0], points[2]), points[4]), points[6])
    }

    /** Get top value of the bounding rectangle of the given points.  */
    fun getRectTop(points: FloatArray): Float {
        return Math.min(Math.min(Math.min(points[1], points[3]), points[5]), points[7])
    }

    /** Get right value of the bounding rectangle of the given points.  */
    fun getRectRight(points: FloatArray): Float {
        return Math.max(Math.max(Math.max(points[0], points[2]), points[4]), points[6])
    }

    /** Get bottom value of the bounding rectangle of the given points.  */
    fun getRectBottom(points: FloatArray): Float {
        return Math.max(Math.max(Math.max(points[1], points[3]), points[5]), points[7])
    }

    /** Get width of the bounding rectangle of the given points.  */
    fun getRectWidth(points: FloatArray): Float {
        return getRectRight(points) - getRectLeft(points)
    }

    /** Get height of the bounding rectangle of the given points.  */
    fun getRectHeight(points: FloatArray): Float {
        return getRectBottom(points) - getRectTop(points)
    }

    /** Get horizontal center value of the bounding rectangle of the given points.  */
    fun getRectCenterX(points: FloatArray): Float {
        return (getRectRight(points) + getRectLeft(points)) / 2f
    }

    /** Get vertical center value of the bounding rectangle of the given points.  */
    fun getRectCenterY(points: FloatArray): Float {
        return (getRectBottom(points) + getRectTop(points)) / 2f
    }

    /**
     * Get a rectangle for the given 4 points (x0,y0,x1,y1,x2,y2,x3,y3) by finding the min/max 2
     * points that contains the given 4 points and is a straight rectangle.
     */
    fun getRectFromPoints(
            points: FloatArray,
            imageWidth: Int,
            imageHeight: Int,
            fixAspectRatio: Boolean,
            aspectRatioX: Int,
            aspectRatioY: Int): Rect {
        val left = Math.round(Math.max(0f, getRectLeft(points)))
        val top = Math.round(Math.max(0f, getRectTop(points)))
        val right = Math.round(Math.min(imageWidth.toFloat(), getRectRight(points)))
        val bottom = Math.round(Math.min(imageHeight.toFloat(), getRectBottom(points)))
        val rect = Rect(left, top, right, bottom)
        if (fixAspectRatio) {
            fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY)
        }
        return rect
    }

    /**
     * Fix the given rectangle if it doesn't confirm to aspect ration rule.<br></br>
     * Make sure that width and height are equal if 1:1 fixed aspect ratio is requested.
     */
    private fun fixRectForAspectRatio(rect: Rect, aspectRatioX: Int, aspectRatioY: Int) {
        if (aspectRatioX == aspectRatioY && rect.width() != rect.height()) {
            if (rect.height() > rect.width()) {
                rect.bottom -= rect.height() - rect.width()
            } else {
                rect.right -= rect.width() - rect.height()
            }
        }
    }

    /**
     * Write given bitmap to a temp file. If file already exists no-op as we already saved the file in
     * this session. Uses JPEG 95% compression.
     *
     * @param uri the uri to write the bitmap to, if null
     * @return the uri where the image was saved in, either the given uri or new pointing to temp
     * file.
     */
    fun writeTempStateStoreBitmap(context: Context, bitmap: Bitmap?, uri: Uri?): Uri? {
        var uri = uri
        return try {
            var needSave = true
            if (uri == null) {
                uri = Uri.fromFile(
                        File.createTempFile("aic_state_store_temp", ".jpg", context.cacheDir))
            } else if (File(uri.path).exists()) {
                needSave = false
            }
            if (needSave) {
                writeBitmapToUri(context, bitmap, uri, CompressFormat.JPEG, 95)
            }
            uri
        } catch (e: Exception) {
            Log.w("AIC", "Failed to write bitmap to temp file for image-cropper save instance state", e)
            null
        }
    }

    /** Write the given bitmap to the given uri using the given compression.  */
    @Throws(FileNotFoundException::class)
    fun writeBitmapToUri(
            context: Context,
            bitmap: Bitmap?,
            uri: Uri?,
            compressFormat: CompressFormat?,
            compressQuality: Int) {
        var outputStream: OutputStream? = null
        try {
            outputStream = context.contentResolver.openOutputStream(uri!!)
            bitmap!!.compress(compressFormat, compressQuality, outputStream)
        } finally {
            closeSafe(outputStream)
        }
    }

    /** Resize the given bitmap to the given width/height by the given option.<br></br>  */
    fun resizeBitmap(
            bitmap: Bitmap?, reqWidth: Int, reqHeight: Int, options: RequestSizeOptions?): Bitmap? {
        try {
            if (reqWidth > 0 && reqHeight > 0 && (options == RequestSizeOptions.RESIZE_FIT || options == RequestSizeOptions.RESIZE_INSIDE || options == RequestSizeOptions.RESIZE_EXACT)) {
                var resized: Bitmap? = null
                if (options == RequestSizeOptions.RESIZE_EXACT) {
                    resized = Bitmap.createScaledBitmap(bitmap!!, reqWidth, reqHeight, false)
                } else {
                    val width = bitmap!!.width
                    val height = bitmap.height
                    val scale = Math.max(width / reqWidth.toFloat(), height / reqHeight.toFloat())
                    if (scale > 1 || options == RequestSizeOptions.RESIZE_FIT) {
                        resized = Bitmap.createScaledBitmap(
                                bitmap, (width / scale).toInt(), (height / scale).toInt(), false)
                    }
                }
                if (resized != null) {
                    if (resized != bitmap) {
                        bitmap.recycle()
                    }
                    return resized
                }
            }
        } catch (e: Exception) {
            Log.w("AIC", "Failed to resize cropped image, return bitmap before resize", e)
        }
        return bitmap
    }
    // region: Private methods
    /**
     * Crop image bitmap from URI by decoding it with specific width and height to down-sample if
     * required.
     *
     * @param orgWidth used to get rectangle from points (handle edge cases to limit rectangle)
     * @param orgHeight used to get rectangle from points (handle edge cases to limit rectangle)
     * @param sampleMulti used to increase the sampling of the image to handle memory issues.
     */
    private fun cropBitmap(
            context: Context,
            loadedImageUri: Uri,
            points: FloatArray,
            degreesRotated: Int,
            orgWidth: Int,
            orgHeight: Int,
            fixAspectRatio: Boolean,
            aspectRatioX: Int,
            aspectRatioY: Int,
            reqWidth: Int,
            reqHeight: Int,
            flipHorizontally: Boolean,
            flipVertically: Boolean,
            sampleMulti: Int): BitmapSampled {

        // get the rectangle in original image that contains the required cropped area (larger for non
        // rectangular crop)
        val rect = getRectFromPoints(points, orgWidth, orgHeight, fixAspectRatio, aspectRatioX, aspectRatioY)
        val width = if (reqWidth > 0) reqWidth else rect.width()
        val height = if (reqHeight > 0) reqHeight else rect.height()
        var result: Bitmap? = null
        var sampleSize = 1
        try {
            // decode only the required image from URI, optionally sub-sampling if reqWidth/reqHeight is
            // given.
            val bitmapSampled = decodeSampledBitmapRegion(context, loadedImageUri, rect, width, height, sampleMulti)
            result = bitmapSampled.bitmap
            sampleSize = bitmapSampled.sampleSize
        } catch (ignored: Exception) {
        }
        return if (result != null) {
            try {
                // rotate the decoded region by the required amount
                result = rotateAndFlipBitmapInt(result, degreesRotated, flipHorizontally, flipVertically)

                // rotating by 0, 90, 180 or 270 degrees doesn't require extra cropping
                if (degreesRotated % 90 != 0) {

                    // extra crop because non rectangular crop cannot be done directly on the image without
                    // rotating first
                    result = cropForRotatedImage(
                            result, points, rect, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY)
                }
            } catch (e: OutOfMemoryError) {
                if (result != null) {
                    result.recycle()
                }
                throw e
            }
            BitmapSampled(result, sampleSize)
        } else {
            // failed to decode region, may be skia issue, try full decode and then crop
            cropBitmap(
                    context,
                    loadedImageUri,
                    points,
                    degreesRotated,
                    fixAspectRatio,
                    aspectRatioX,
                    aspectRatioY,
                    sampleMulti,
                    rect,
                    width,
                    height,
                    flipHorizontally,
                    flipVertically)
        }
    }

    /**
     * Crop bitmap by fully loading the original and then cropping it, fallback in case cropping
     * region failed.
     */
    private fun cropBitmap(
            context: Context,
            loadedImageUri: Uri,
            points: FloatArray,
            degreesRotated: Int,
            fixAspectRatio: Boolean,
            aspectRatioX: Int,
            aspectRatioY: Int,
            sampleMulti: Int,
            rect: Rect,
            width: Int,
            height: Int,
            flipHorizontally: Boolean,
            flipVertically: Boolean): BitmapSampled {
        var result: Bitmap? = null
        val sampleSize: Int
        try {
            val options = BitmapFactory.Options()
            sampleSize = (sampleMulti
                    * calculateInSampleSizeByReqestedSize(rect.width(), rect.height(), width, height))
            options.inSampleSize = sampleSize
            val fullBitmap = decodeImage(context.contentResolver, loadedImageUri, options)
            if (fullBitmap != null) {
                try {
                    // adjust crop points by the sampling because the image is smaller
                    val points2 = FloatArray(points.size)
                    System.arraycopy(points, 0, points2, 0, points.size)
                    for (i in points2.indices) {
                        points2[i] = points2[i] / options.inSampleSize
                    }
                    result = cropBitmapObjectWithScale(
                            fullBitmap,
                            points2,
                            degreesRotated,
                            fixAspectRatio,
                            aspectRatioX,
                            aspectRatioY, 1f,
                            flipHorizontally,
                            flipVertically)
                } finally {
                    if (result != fullBitmap) {
                        fullBitmap.recycle()
                    }
                }
            }
        } catch (e: OutOfMemoryError) {
            result?.recycle()
            throw e
        } catch (e: Exception) {
            throw RuntimeException(
                    """
                        Failed to load sampled bitmap: $loadedImageUri
                        ${e.message}
                        """.trimIndent(), e)
        }
        return BitmapSampled(result, sampleSize)
    }

    /** Decode image from uri using "inJustDecodeBounds" to get the image dimensions.  */
    @Throws(FileNotFoundException::class)
    private fun decodeImageForOption(resolver: ContentResolver, uri: Uri): BitmapFactory.Options {
        var stream: InputStream? = null
        return try {
            stream = resolver.openInputStream(uri)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(stream, EMPTY_RECT, options)
            options.inJustDecodeBounds = false
            options
        } finally {
            closeSafe(stream)
        }
    }

    /**
     * Decode image from uri using given "inSampleSize", but if failed due to out-of-memory then raise
     * the inSampleSize until success.
     */
    @Throws(FileNotFoundException::class)
    private fun decodeImage(
            resolver: ContentResolver, uri: Uri, options: BitmapFactory.Options): Bitmap? {
        do {
            var stream: InputStream? = null
            try {
                stream = resolver.openInputStream(uri)
                return BitmapFactory.decodeStream(stream, EMPTY_RECT, options)
            } catch (e: OutOfMemoryError) {
                options.inSampleSize *= 2
            } finally {
                closeSafe(stream)
            }
        } while (options.inSampleSize <= 512)
        throw RuntimeException("Failed to decode image: $uri")
    }

    /**
     * Decode specific rectangle bitmap from stream using sampling to get bitmap with the requested
     * limit.
     *
     * @param sampleMulti used to increase the sampling of the image to handle memory issues.
     */
    private fun decodeSampledBitmapRegion(
            context: Context, uri: Uri, rect: Rect, reqWidth: Int, reqHeight: Int, sampleMulti: Int): BitmapSampled {
        var stream: InputStream? = null
        var decoder: BitmapRegionDecoder? = null
        try {
            val options = BitmapFactory.Options()
            options.inSampleSize = (sampleMulti
                    * calculateInSampleSizeByReqestedSize(
                    rect.width(), rect.height(), reqWidth, reqHeight))
            stream = context.contentResolver.openInputStream(uri)
            decoder = BitmapRegionDecoder.newInstance(stream, false)
            do {
                try {
                    return BitmapSampled(decoder.decodeRegion(rect, options), options.inSampleSize)
                } catch (e: OutOfMemoryError) {
                    options.inSampleSize *= 2
                }
            } while (options.inSampleSize <= 512)
        } catch (e: Exception) {
            throw RuntimeException(
                    """
                        Failed to load sampled bitmap: $uri
                        ${e.message}
                        """.trimIndent(), e)
        } finally {
            closeSafe(stream)
            decoder?.recycle()
        }
        return BitmapSampled(null, 1)
    }

    /**
     * Special crop of bitmap rotated by not stright angle, in this case the original crop bitmap
     * contains parts beyond the required crop area, this method crops the already cropped and rotated
     * bitmap to the final rectangle.<br></br>
     * Note: rotating by 0, 90, 180 or 270 degrees doesn't require extra cropping.
     */
    private fun cropForRotatedImage(
            bitmap: Bitmap?,
            points: FloatArray,
            rect: Rect,
            degreesRotated: Int,
            fixAspectRatio: Boolean,
            aspectRatioX: Int,
            aspectRatioY: Int): Bitmap? {
        var bitmap = bitmap
        if (degreesRotated % 90 != 0) {
            var adjLeft = 0
            var adjTop = 0
            var width = 0
            var height = 0
            val rads = Math.toRadians(degreesRotated.toDouble())
            val compareTo = if (degreesRotated < 90 || degreesRotated > 180 && degreesRotated < 270) rect.left else rect.right
            var i = 0
            while (i < points.size) {
                if (points[i] >= compareTo - 1 && points[i] <= compareTo + 1) {
                    adjLeft = Math.abs(Math.sin(rads) * (rect.bottom - points[i + 1])).toInt()
                    adjTop = Math.abs(Math.cos(rads) * (points[i + 1] - rect.top)).toInt()
                    width = Math.abs((points[i + 1] - rect.top) / Math.sin(rads)).toInt()
                    height = Math.abs((rect.bottom - points[i + 1]) / Math.cos(rads)).toInt()
                    break
                }
                i += 2
            }
            rect[adjLeft, adjTop, adjLeft + width] = adjTop + height
            if (fixAspectRatio) {
                fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY)
            }
            val bitmapTmp = bitmap
            bitmap = Bitmap.createBitmap(bitmap!!, rect.left, rect.top, rect.width(), rect.height())
            if (bitmapTmp != bitmap) {
                bitmapTmp!!.recycle()
            }
        }
        return bitmap
    }

    /**
     * Calculate the largest inSampleSize value that is a power of 2 and keeps both height and width
     * larger than the requested height and width.
     */
    private fun calculateInSampleSizeByReqestedSize(
            width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            while (height / 2 / inSampleSize > reqHeight && width / 2 / inSampleSize > reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Calculate the largest inSampleSize value that is a power of 2 and keeps both height and width
     * smaller than max texture size allowed for the device.
     */
    private fun calculateInSampleSizeByMaxTextureSize(width: Int, height: Int): Int {
        var inSampleSize = 1
        if (mMaxTextureSize == 0) {
            mMaxTextureSize = maxTextureSize
        }
        if (mMaxTextureSize > 0) {
            while (height / inSampleSize > mMaxTextureSize
                    || width / inSampleSize > mMaxTextureSize) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Rotate the given bitmap by the given degrees.<br></br>
     * New bitmap is created and the old one is recycled.
     */
    private fun rotateAndFlipBitmapInt(
            bitmap: Bitmap, degrees: Int, flipHorizontally: Boolean, flipVertically: Boolean): Bitmap {
        return if (degrees > 0 || flipHorizontally || flipVertically) {
            val matrix = Matrix()
            matrix.setRotate(degrees.toFloat())
            matrix.postScale(if (flipHorizontally) (-1).toFloat() else 1.toFloat(), if (flipVertically) (-1).toFloat() else 1.toFloat())
            val newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
            if (newBitmap != bitmap) {
                bitmap.recycle()
            }
            newBitmap
        } else {
            bitmap
        }
    }// Only need to check for width since opengl textures are always squared

    // Keep track of the maximum texture size

    // Release

    // Return largest texture size found, or default
// Get EGL Display

    // Initialise

    // Query total number of configurations

    // Query actual list configurations

    // Iterate through all the configurations to located the maximum texture size
// Safe minimum default size
    /**
     * Get the max size of bitmap allowed to be rendered on the device.<br></br>
     * http://stackoverflow.com/questions/7428996/hw-accelerated-activity-how-to-get-opengl-texture-size-limit.
     */
    private val maxTextureSize: Int get() {
            // Safe minimum default size
            val IMAGE_MAX_BITMAP_DIMENSION = 2048
            return try {
                // Get EGL Display
                val egl = EGLContext.getEGL() as EGL10
                val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

                // Initialise
                val version = IntArray(2)
                egl.eglInitialize(display, version)

                // Query total number of configurations
                val totalConfigurations = IntArray(1)
                egl.eglGetConfigs(display, null, 0, totalConfigurations)

                // Query actual list configurations
                val configurationsList = arrayOfNulls<EGLConfig>(totalConfigurations[0])
                egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations)
                val textureSize = IntArray(1)
                var maximumTextureSize = 0

                // Iterate through all the configurations to located the maximum texture size
                for (i in 0 until totalConfigurations[0]) {
                    // Only need to check for width since opengl textures are always squared
                    egl.eglGetConfigAttrib(
                            display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize)

                    // Keep track of the maximum texture size
                    if (maximumTextureSize < textureSize[0]) {
                        maximumTextureSize = textureSize[0]
                    }
                }

                // Release
                egl.eglTerminate(display)

                // Return largest texture size found, or default
                Math.max(maximumTextureSize, IMAGE_MAX_BITMAP_DIMENSION)
            } catch (e: Exception) {
                IMAGE_MAX_BITMAP_DIMENSION
            }
        }

    /**
     * Close the given closeable object (Stream) in a safe way: check if it is null and catch-log
     * exception thrown.
     *
     * @param closeable the closable object to close
     */
    private fun closeSafe(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (ignored: IOException) {
            }
        }
    }
    // endregion
    // region: Inner class: BitmapSampled
    /** Holds bitmap instance and the sample size that the bitmap was loaded/cropped with.  */
    internal class BitmapSampled(
            /** The bitmap instance  */
            val bitmap: Bitmap?,
            /** The sample size used to lower the size of the bitmap (1,2,4,8,...)  */
            val sampleSize: Int)
    // endregion
    // region: Inner class: RotateBitmapResult
    /** The result of [.rotateBitmapByExif].  */
    internal class RotateBitmapResult(
            /** The loaded bitmap  */
            val bitmap: Bitmap?,
            /** The degrees the image was rotated  */
            val degrees: Int) // endregion
}