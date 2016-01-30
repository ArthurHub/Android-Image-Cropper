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

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.ImageView;

/**
 * Defaults used in the library.
 */
final class CropDefaults {

    public static final Rect EMPTY_RECT = new Rect();

    public static final RectF EMPTY_RECT_F = new RectF();

    public static final boolean DEFAULT_FIXED_ASPECT_RATIO = false;

    public static final int DEFAULT_ASPECT_RATIO_X = 1;

    public static final int DEFAULT_ASPECT_RATIO_Y = 1;

    public static final int DEFAULT_SCALE_TYPE_INDEX = 0;

    public static final int DEFAULT_CROP_SHAPE_INDEX = 0;

    public static final int DEFAULT_GUIDELINES_INDEX = 1;

    public static final float MIN_CROP_WINDOW_SIZE = 42;

    public static final float MIN_CROP_RESULT_SIZE = 80;

    public static final float MAX_CROP_RESULT_SIZE = 99999;

    public static final float SNAP_RADIUS = 3;

    public static final float DEFAULT_SHOW_GUIDELINES_LIMIT = 100;

    public static final ImageView.ScaleType[] VALID_SCALE_TYPES = new ImageView.ScaleType[]{ImageView.ScaleType.CENTER_INSIDE, ImageView.ScaleType.FIT_CENTER};

    public static final CropImageView.CropShape[] VALID_CROP_SHAPES = new CropImageView.CropShape[]{CropImageView.CropShape.RECTANGLE, CropImageView.CropShape.OVAL};

    public static final CropImageView.Guidelines[] VALID_GUIDELINES = new CropImageView.Guidelines[]{CropImageView.Guidelines.OFF, CropImageView.Guidelines.ON_TOUCH, CropImageView.Guidelines.ON};

    // The radius (in dp) of the touchable area around the handle. We are basing
    // this value off of the recommended 48dp Rhythm. See:
    // http://developer.android.com/design/style/metrics-grids.html#48dp-rhythm
    public static final int TOUCH_RADIUS = 24;

    public static final float DEFAULT_INITIAL_CROP_WINDOW_PADDING_RATIO = 0.1f;

    public static final float DEFAULT_BORDER_LINE_THICKNESS = 3;

    public static final float DEFAULT_BORDER_CORNER_THICKNESS = 2;

    public static final float DEFAULT_BORDER_CORNER_OFFSET = 5;

    public static final float DEFAULT_BORDER_CORNER_LENGTH = 14;

    public static final float DEFAULT_GUIDELINE_THICKNESS = 1;

    public static final int DEFAULT_BORDER_LINE_COLOR = Color.argb(170, 255, 255, 255);

    public static final int DEFAULT_BORDER_CORNER_COLOR = Color.WHITE;

    public static final int DEFAULT_GUIDELINE_COLOR = Color.argb(170, 255, 255, 255);

    public static final int DEFAULT_BACKGROUND_COLOR = Color.argb(119, 0, 0, 0);
}
