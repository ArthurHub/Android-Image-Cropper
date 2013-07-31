/*
 * Copyright 2013, Edmodo, Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file, or at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License. 
 */

package com.edmodo.cropper.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.TypedValue;

/**
 * Utility class for handling all of the Paint used to draw the CropOverlayView.
 */
public class PaintUtil {

    // Private Constants ///////////////////////////////////////////////////////

    private static final int DEFAULT_CORNER_COLOR = Color.WHITE;
    private static final String SEMI_TRANSPARENT = "#AAFFFFFF";
    private static final String DEFAULT_BACKGROUND_COLOR_ID = "#B0000000";
    private static final float DEFAULT_LINE_THICKNESS_DP = 3;
    private static final float DEFAULT_CORNER_THICKNESS_DP = 5;
    private static final float DEFAULT_GUIDELINE_THICKNESS_PX = 1;

    // Public Methods //////////////////////////////////////////////////////////

    /**
     * Creates the Paint object for drawing the crop window border.
     * 
     * @param context the Context
     * @return new Paint object
     */
    public static Paint newBorderPaint(Context context) {

        // Set the line thickness for the crop window border.
        final float lineThicknessPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                                DEFAULT_LINE_THICKNESS_DP,
                                                                context.getResources().getDisplayMetrics());

        final Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor(SEMI_TRANSPARENT));
        borderPaint.setStrokeWidth(lineThicknessPx);
        borderPaint.setStyle(Paint.Style.STROKE);

        return borderPaint;
    }

    /**
     * Creates the Paint object for drawing the crop window guidelines.
     * 
     * @return the new Paint object
     */
    public static Paint newGuidelinePaint() {

        final Paint paint = new Paint();
        paint.setColor(Color.parseColor(SEMI_TRANSPARENT));
        paint.setStrokeWidth(DEFAULT_GUIDELINE_THICKNESS_PX);

        return paint;
    }

    /**
     * Creates the Paint object for drawing the translucent overlay outside the
     * crop window.
     * 
     * @param context the Context
     * @return the new Paint object
     */
    public static Paint newBackgroundPaint(Context context) {

        final Paint paint = new Paint();
        paint.setColor(Color.parseColor(DEFAULT_BACKGROUND_COLOR_ID));

        return paint;
    }

    /**
     * Creates the Paint object for drawing the corners of the border
     * 
     * @param context the Context
     * @return the new Paint object
     */
    public static Paint newCornerPaint(Context context) {

        // Set the line thickness for the crop window border.
        final float lineThicknessPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                                DEFAULT_CORNER_THICKNESS_DP,
                                                                context.getResources().getDisplayMetrics());

        final Paint cornerPaint = new Paint();
        cornerPaint.setColor(DEFAULT_CORNER_COLOR);
        cornerPaint.setStrokeWidth(lineThicknessPx);
        cornerPaint.setStyle(Paint.Style.STROKE);

        return cornerPaint;
    }

    /**
     * Returns the value of the corner thickness
     * 
     * @return Float equivalent to the corner thickness
     */
    public static float getCornerThickness() {
        return DEFAULT_CORNER_THICKNESS_DP;
    }

    /**
     * Returns the value of the line thickness of the border
     * 
     * @return Float equivalent to the line thickness
     */
    public static float getLineThickness() {
        return DEFAULT_LINE_THICKNESS_DP;
    }

}
