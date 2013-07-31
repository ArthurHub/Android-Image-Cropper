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

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;
import android.widget.ImageView;

/**
 * Utility class that deals with operations with an ImageView.
 */
public class ImageViewUtil {

    /**
     * Gets the rectangular position of a Bitmap if it were placed inside a View
     * with scale type set to {@link ImageView#ScaleType #CENTER_INSIDE}.
     * 
     * @param bitmap the Bitmap
     * @param view the parent View of the Bitmap
     * @return the rectangular position of the Bitmap
     */
    public static Rect getBitmapRectCenterInside(Bitmap bitmap, View view) {

        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();
        final int viewWidth = view.getWidth();
        final int viewHeight = view.getHeight();

        return getBitmapRectCenterInsideHelper(bitmapWidth, bitmapHeight, viewWidth, viewHeight);
    }

    /**
     * Gets the rectangular position of a Bitmap if it were placed inside a View
     * with scale type set to {@link ImageView#ScaleType #CENTER_INSIDE}.
     * 
     * @param bitmapWidth the Bitmap's width
     * @param bitmapHeight the Bitmap's height
     * @param viewWidth the parent View's width
     * @param viewHeight the parent View's height
     * @return the rectangular position of the Bitmap
     */
    public static Rect getBitmapRectCenterInside(int bitmapWidth,
                                                 int bitmapHeight,
                                                 int viewWidth,
                                                 int viewHeight)
    {
        return getBitmapRectCenterInsideHelper(bitmapWidth, bitmapHeight, viewWidth, viewHeight);
    }

    /**
     * Helper that does the work of the above functions. Gets the rectangular
     * position of a Bitmap if it were placed inside a View with scale type set
     * to {@link ImageView#ScaleType #CENTER_INSIDE}.
     * 
     * @param bitmapWidth the Bitmap's width
     * @param bitmapHeight the Bitmap's height
     * @param viewWidth the parent View's width
     * @param viewHeight the parent View's height
     * @return the rectangular position of the Bitmap
     */
    private static Rect getBitmapRectCenterInsideHelper(int bitmapWidth,
                                                        int bitmapHeight,
                                                        int viewWidth,
                                                        int viewHeight) {
        double resultWidth;
        double resultHeight;
        int resultX;
        int resultY;

        double viewToBitmapWidthRatio = Double.POSITIVE_INFINITY;
        double viewToBitmapHeightRatio = Double.POSITIVE_INFINITY;

        // Checks if either width or height needs to be fixed
        if (viewWidth < bitmapWidth) {
            viewToBitmapWidthRatio = (double) viewWidth / (double) bitmapWidth;
        }
        if (viewHeight < bitmapHeight) {
            viewToBitmapHeightRatio = (double) viewHeight / (double) bitmapHeight;
        }

        // If either needs to be fixed, choose smallest ratio and calculate from
        // there
        if (viewToBitmapWidthRatio != Double.POSITIVE_INFINITY || viewToBitmapHeightRatio != Double.POSITIVE_INFINITY)
        {
            if (viewToBitmapWidthRatio <= viewToBitmapHeightRatio) {
                resultWidth = viewWidth;
                resultHeight = (bitmapHeight * resultWidth / bitmapWidth);
            }
            else {
                resultHeight = viewHeight;
                resultWidth = (bitmapWidth * resultHeight / bitmapHeight);
            }
        }
        // Otherwise, the picture is within frame layout bounds. Desired width
        // is simply picture size
        else {
            resultHeight = bitmapHeight;
            resultWidth = bitmapWidth;
        }

        // Calculate the position of the bitmap inside the ImageView.
        if (resultWidth == viewWidth) {
            resultX = 0;
            resultY = (int) Math.round((viewHeight - resultHeight) / 2);
        } else if (resultHeight == viewHeight) {
            resultX = (int) Math.round((viewWidth - resultWidth) / 2);
            resultY = 0;
        }
        else {
            resultX = (int) Math.round((viewWidth - resultWidth) / 2);
            resultY = (int) Math.round((viewHeight - resultHeight) / 2);
        }

        final Rect result = new Rect(resultX,
                                     resultY,
                                     resultX + (int) Math.ceil(resultWidth),
                                     resultY + (int) Math.ceil(resultHeight));

        return result;
    }
}
