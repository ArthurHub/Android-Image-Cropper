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

package com.theartofdev.edmodo.cropper.util;

import android.content.Context;
import android.util.Pair;
import android.util.TypedValue;

import com.theartofdev.edmodo.cropper.CropShape;
import com.theartofdev.edmodo.cropper.cropwindow.CropOverlayView;
import com.theartofdev.edmodo.cropper.cropwindow.handle.Handle;

/**
 * Utility class to perform basic operations with Handles.
 */
public class HandleUtil {

    // Private Constants ///////////////////////////////////////////////////////

    // The radius (in dp) of the touchable area around the handle. We are basing
    // this value off of the recommended 48dp Rhythm. See:
    // http://developer.android.com/design/style/metrics-grids.html#48dp-rhythm
    private static final int TARGET_RADIUS_DP = 24;

    // Public Methods //////////////////////////////////////////////////////////

    /**
     * Gets the default target radius (in pixels). This is the radius of the
     * circular area that can be touched in order to activate the handle.
     *
     * @param context the Context
     * @return the target radius (in pixels)
     */
    public static float getTargetRadius(Context context) {

        final float targetRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                TARGET_RADIUS_DP,
                context.getResources().getDisplayMetrics());
        return targetRadius;
    }

    /**
     * Determines which, if any, of the handles are pressed given the touch
     * coordinates, the bounding box, and the touch radius.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param left the x-coordinate of the left bound
     * @param top the y-coordinate of the top bound
     * @param right the x-coordinate of the right bound
     * @param bottom the y-coordinate of the bottom bound
     * @param targetRadius the target radius in pixels
     * @return the Handle that was pressed; null if no Handle was pressed
     */
    public static Handle getPressedHandle(float x,
                                          float y,
                                          float left,
                                          float top,
                                          float right,
                                          float bottom,
                                          float targetRadius,
                                          CropShape cropShape) {

        Handle pressedHandle = null;

        if (cropShape == CropShape.RECTANGLE) {
            pressedHandle = getRectanglePressedHandle(x, y, left, top, right, bottom, targetRadius);
        } else if (cropShape == CropShape.OVAL) {
            pressedHandle = getOvalPressedHandle(x, y, left, top, right, bottom);
        }

        return pressedHandle;
    }

    /**
     * Determines which, if any, of the handles are pressed given the touch
     * coordinates, the bounding box, and the touch radius.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param left the x-coordinate of the left bound
     * @param top the y-coordinate of the top bound
     * @param right the x-coordinate of the right bound
     * @param bottom the y-coordinate of the bottom bound
     * @param targetRadius the target radius in pixels
     * @return the Handle that was pressed; null if no Handle was pressed
     */
    private static Handle getRectanglePressedHandle(float x,
                                                    float y,
                                                    float left,
                                                    float top,
                                                    float right,
                                                    float bottom,
                                                    float targetRadius) {
        Handle pressedHandle = null;

        // Note: corner-handles take precedence, then side-handles, then center.
        if (HandleUtil.isInCornerTargetZone(x, y, left, top, targetRadius)) {
            pressedHandle = Handle.TOP_LEFT;
        } else if (HandleUtil.isInCornerTargetZone(x, y, right, top, targetRadius)) {
            pressedHandle = Handle.TOP_RIGHT;
        } else if (HandleUtil.isInCornerTargetZone(x, y, left, bottom, targetRadius)) {
            pressedHandle = Handle.BOTTOM_LEFT;
        } else if (HandleUtil.isInCornerTargetZone(x, y, right, bottom, targetRadius)) {
            pressedHandle = Handle.BOTTOM_RIGHT;
        } else if (HandleUtil.isInCenterTargetZone(x, y, left, top, right, bottom) && focusCenter()) {
            pressedHandle = Handle.CENTER;
        } else if (HandleUtil.isInHorizontalTargetZone(x, y, left, right, top, targetRadius)) {
            pressedHandle = Handle.TOP;
        } else if (HandleUtil.isInHorizontalTargetZone(x, y, left, right, bottom, targetRadius)) {
            pressedHandle = Handle.BOTTOM;
        } else if (HandleUtil.isInVerticalTargetZone(x, y, left, top, bottom, targetRadius)) {
            pressedHandle = Handle.LEFT;
        } else if (HandleUtil.isInVerticalTargetZone(x, y, right, top, bottom, targetRadius)) {
            pressedHandle = Handle.RIGHT;
        } else if (HandleUtil.isInCenterTargetZone(x, y, left, top, right, bottom) && !focusCenter()) {
            pressedHandle = Handle.CENTER;
        }

        return pressedHandle;
    }

    /**
     * Determines which, if any, of the handles are pressed given the touch
     * coordinates, the bounding box/oval, and the touch radius.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param left the x-coordinate of the left bound
     * @param top the y-coordinate of the top bound
     * @param right the x-coordinate of the right bound
     * @param bottom the y-coordinate of the bottom bound
     * @return the Handle that was pressed; null if no Handle was pressed
     */
    private static Handle getOvalPressedHandle(float x,
                                               float y,
                                               float left,
                                               float top,
                                               float right,
                                               float bottom) {

        /*
           Use a 6x6 grid system divided into 9 "handles", with the center the biggest region. While
           this is not perfect, it's a good quick-to-ship approach.

           TL T T T T TR
            L C C C C R
            L C C C C R
            L C C C C R
            L C C C C R
           BL B B B B BR
        */
        final float cellLength = (right - left) / 6;
        final float leftCenter = left + cellLength;
        final float rightCenter = left + (5 * cellLength);

        final float cellHeight = (bottom - top) / 6;
        final float topCenter = top + cellHeight;
        final float bottomCenter = top + 5 * cellHeight;

        final Handle pressedHandle;
        if (x < leftCenter) {
            if (y < topCenter) {
                pressedHandle = Handle.TOP_LEFT;
            } else if(y < bottomCenter) {
                pressedHandle = Handle.LEFT;
            } else {
                pressedHandle = Handle.BOTTOM_LEFT;
            }
        } else if (x < rightCenter) {
            if (y < topCenter) {
                pressedHandle = Handle.TOP;
            } else if(y < bottomCenter) {
                pressedHandle = Handle.CENTER;
            } else {
                pressedHandle = Handle.BOTTOM;
            }
        } else {
            if (y < topCenter) {
                pressedHandle = Handle.TOP_RIGHT;
            } else if(y < bottomCenter) {
                pressedHandle = Handle.RIGHT;
            } else {
                pressedHandle = Handle.BOTTOM_RIGHT;
            }
        }

        return pressedHandle;
    }

    /**
     * Calculates the offset of the touch point from the precise location of the
     * specified handle.
     *
     * @return the offset as a Pair where the x-offset is the first value and
     * the y-offset is the second value; null if the handle is null
     */
    public static Pair<Float, Float> getOffset(Handle handle,
                                               float x,
                                               float y,
                                               float left,
                                               float top,
                                               float right,
                                               float bottom) {

        if (handle == null) {
            return null;
        }

        float touchOffsetX = 0;
        float touchOffsetY = 0;

        // Calculate the offset from the appropriate handle.
        switch (handle) {

            case TOP_LEFT:
                touchOffsetX = left - x;
                touchOffsetY = top - y;
                break;
            case TOP_RIGHT:
                touchOffsetX = right - x;
                touchOffsetY = top - y;
                break;
            case BOTTOM_LEFT:
                touchOffsetX = left - x;
                touchOffsetY = bottom - y;
                break;
            case BOTTOM_RIGHT:
                touchOffsetX = right - x;
                touchOffsetY = bottom - y;
                break;
            case LEFT:
                touchOffsetX = left - x;
                touchOffsetY = 0;
                break;
            case TOP:
                touchOffsetX = 0;
                touchOffsetY = top - y;
                break;
            case RIGHT:
                touchOffsetX = right - x;
                touchOffsetY = 0;
                break;
            case BOTTOM:
                touchOffsetX = 0;
                touchOffsetY = bottom - y;
                break;
            case CENTER:
                final float centerX = (right + left) / 2;
                final float centerY = (top + bottom) / 2;
                touchOffsetX = centerX - x;
                touchOffsetY = centerY - y;
                break;
        }

        final Pair<Float, Float> result = new Pair<Float, Float>(touchOffsetX, touchOffsetY);
        return result;
    }

    // Private Methods /////////////////////////////////////////////////////////

    /**
     * Determines if the specified coordinate is in the target touch zone for a
     * corner handle.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param handleX the x-coordinate of the corner handle
     * @param handleY the y-coordinate of the corner handle
     * @param targetRadius the target radius in pixels
     * @return true if the touch point is in the target touch zone; false
     * otherwise
     */
    private static boolean isInCornerTargetZone(float x,
                                                float y,
                                                float handleX,
                                                float handleY,
                                                float targetRadius) {

        if (Math.abs(x - handleX) <= targetRadius && Math.abs(y - handleY) <= targetRadius) {
            return true;
        }
        return false;
    }

    /**
     * Determines if the specified coordinate is in the target touch zone for a
     * horizontal bar handle.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param handleXStart the left x-coordinate of the horizontal bar handle
     * @param handleXEnd the right x-coordinate of the horizontal bar handle
     * @param handleY the y-coordinate of the horizontal bar handle
     * @param targetRadius the target radius in pixels
     * @return true if the touch point is in the target touch zone; false
     * otherwise
     */
    private static boolean isInHorizontalTargetZone(float x,
                                                    float y,
                                                    float handleXStart,
                                                    float handleXEnd,
                                                    float handleY,
                                                    float targetRadius) {

        if (x > handleXStart && x < handleXEnd && Math.abs(y - handleY) <= targetRadius) {
            return true;
        }
        return false;
    }

    /**
     * Determines if the specified coordinate is in the target touch zone for a
     * vertical bar handle.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param handleX the x-coordinate of the vertical bar handle
     * @param handleYStart the top y-coordinate of the vertical bar handle
     * @param handleYEnd the bottom y-coordinate of the vertical bar handle
     * @param targetRadius the target radius in pixels
     * @return true if the touch point is in the target touch zone; false
     * otherwise
     */
    private static boolean isInVerticalTargetZone(float x,
                                                  float y,
                                                  float handleX,
                                                  float handleYStart,
                                                  float handleYEnd,
                                                  float targetRadius) {

        if (Math.abs(x - handleX) <= targetRadius && y > handleYStart && y < handleYEnd) {
            return true;
        }
        return false;
    }

    /**
     * Determines if the specified coordinate falls anywhere inside the given
     * bounds.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param left the x-coordinate of the left bound
     * @param top the y-coordinate of the top bound
     * @param right the x-coordinate of the right bound
     * @param bottom the y-coordinate of the bottom bound
     * @return true if the touch point is inside the bounding rectangle; false
     * otherwise
     */
    private static boolean isInCenterTargetZone(float x,
                                                float y,
                                                float left,
                                                float top,
                                                float right,
                                                float bottom) {

        if (x > left && x < right && y > top && y < bottom) {
            return true;
        }
        return false;
    }

    /**
     * Determines if the cropper should focus on the center handle or the side
     * handles. If it is a small image, focus on the center handle so the user
     * can move it. If it is a large image, focus on the side handles so user
     * can grab them. Corresponds to the appearance of the
     * RuleOfThirdsGuidelines.
     *
     * @return true if it is small enough such that it should focus on the
     * center; less than show_guidelines limit
     */
    private static boolean focusCenter() {
        return (!CropOverlayView.showGuidelines());
    }
}
