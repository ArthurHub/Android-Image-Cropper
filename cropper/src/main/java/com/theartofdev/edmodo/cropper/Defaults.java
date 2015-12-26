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

import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.ImageView;

import com.theartofdev.edmodo.cropper.util.PaintUtil;

/**
 * Defaults used in the library.
 */
class Defaults {

    public static final Rect EMPTY_RECT = new Rect();

    public static final RectF EMPTY_RECT_F = new RectF();

    // Sets the default image guidelines to show when resizing
    public static final int DEFAULT_GUIDELINES = 1;

    public static final boolean DEFAULT_FIXED_ASPECT_RATIO = false;

    public static final int DEFAULT_ASPECT_RATIO_X = 1;

    public static final int DEFAULT_ASPECT_RATIO_Y = 1;

    public static final int DEFAULT_SCALE_TYPE_INDEX = 0;

    public static final int DEFAULT_CROP_SHAPE_INDEX = 0;

    public static final float SNAP_RADIUS_DP = 3;

    public static final float DEFAULT_SHOW_GUIDELINES_LIMIT = 100;

    // Gets default values from PaintUtil, sets a bunch of values such that the
    // corners will draw correctly
    public static final float DEFAULT_CORNER_THICKNESS_DP = PaintUtil.getCornerThickness();

    public static final float DEFAULT_LINE_THICKNESS_DP = PaintUtil.getLineThickness();

    public static final float DEFAULT_CORNER_OFFSET_DP = (DEFAULT_CORNER_THICKNESS_DP / 2) - (DEFAULT_LINE_THICKNESS_DP / 2);

    public static final float DEFAULT_CORNER_EXTENSION_DP = DEFAULT_CORNER_THICKNESS_DP / 2 + DEFAULT_CORNER_OFFSET_DP;

    public static final float DEFAULT_CORNER_LENGTH_DP = 15;

    public static final int GUIDELINES_ON_TOUCH = 1;

    public static final int GUIDELINES_ON = 2;

    public static final ImageView.ScaleType[] VALID_SCALE_TYPES = new ImageView.ScaleType[]{ImageView.ScaleType.CENTER_INSIDE, ImageView.ScaleType.FIT_CENTER};

    public static final CropImageView.CropShape[] VALID_CROP_SHAPES = new CropImageView.CropShape[]{CropImageView.CropShape.RECTANGLE, CropImageView.CropShape.OVAL};
}
