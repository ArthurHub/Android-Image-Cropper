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

package com.theartofdev.edmodo.cropper.cropwindow.handle;

import android.graphics.Rect;

import com.theartofdev.edmodo.cropper.Edge;
import com.theartofdev.edmodo.cropper.util.AspectRatioUtil;

/**
 * Abstract helper class to handle operations on a crop window Handle.
 */
abstract class HandleHelper {

    //region: Fields and Consts

    private Edge mHorizontalEdge;

    private Edge mVerticalEdge;
    //endregion

    /**
     * Constructor.
     *
     * @param horizontalEdge the horizontal edge associated with this handle;
     * may be null
     * @param verticalEdge the vertical edge associated with this handle; may be
     * null
     */
    HandleHelper(Edge horizontalEdge, Edge verticalEdge) {
        mHorizontalEdge = horizontalEdge;
        mVerticalEdge = verticalEdge;
    }

    // Package-Private Methods /////////////////////////////////////////////////

    /**
     * Updates the crop window by directly setting the Edge coordinates.
     *
     * @param x the new x-coordinate of this handle
     * @param y the new y-coordinate of this handle
     * @param imageRect the bounding rectangle of the image
     * @param parentView the parent View containing the image
     * @param snapRadius the maximum distance (in pixels) at which the crop
     * window should snap to the image
     */
    void updateCropWindow(float x, float y, Rect imageRect, float snapRadius) {

        if (mHorizontalEdge != null) {
            mHorizontalEdge.adjustCoordinate(x, y, imageRect, snapRadius, 0);
        }

        if (mVerticalEdge != null) {
            mVerticalEdge.adjustCoordinate(x, y, imageRect, snapRadius, 0);
        }
    }

    /**
     * Updates the crop window by directly setting the Edge coordinates; this
     * method maintains a given aspect ratio.
     *
     * @param x the new x-coordinate of this handle
     * @param y the new y-coordinate of this handle
     * @param targetAspectRatio the aspect ratio to maintain
     * @param imageRect the bounding rectangle of the image
     * @param parentView the parent View containing the image
     * @param snapRadius the maximum distance (in pixels) at which the crop
     * window should snap to the image
     */
    abstract void updateCropWindow(float x, float y, float targetAspectRatio, Rect imageRect, float snapRadius);

    /**
     * Gets the Edges associated with this handle as an ordered Pair. The
     * <code>primary</code> Edge in the pair is the determining side. This
     * method is used when we need to maintain the aspect ratio.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param targetAspectRatio the aspect ratio that we are maintaining
     * @return the active edges as an ordered pair
     */
    Edge getActivePrimaryEdge(float x, float y, float targetAspectRatio) {

        // Calculate the aspect ratio if this handle were dragged to the given x-y coordinate.
        float potentialAspectRatio = getAspectRatio(x, y);

        // If the touched point is wider than the aspect ratio, then x is the determining side. Else, y is the determining side.
        return potentialAspectRatio > targetAspectRatio ? mVerticalEdge : mHorizontalEdge;
    }

    /**
     * Gets the Edges associated with this handle as an ordered Pair. The
     * <code>primary</code> Edge in the pair is the determining side. This
     * method is used when we need to maintain the aspect ratio.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param targetAspectRatio the aspect ratio that we are maintaining
     * @return the active edges as an ordered pair
     */
    Edge getActiveSecondaryEdge(float x, float y, float targetAspectRatio) {

        // Calculate the aspect ratio if this handle were dragged to the given x-y coordinate.
        float potentialAspectRatio = getAspectRatio(x, y);

        // If the touched point is wider than the aspect ratio, then x is the determining side. Else, y is the determining side.
        return potentialAspectRatio > targetAspectRatio ? mHorizontalEdge : mVerticalEdge;
    }

    // Private Methods /////////////////////////////////////////////////////////

    /**
     * Gets the aspect ratio of the resulting crop window if this handle were
     * dragged to the given point.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @return the aspect ratio
     */
    private float getAspectRatio(float x, float y) {

        // Replace the active edge coordinate with the given touch coordinate.
        float left = mVerticalEdge == Edge.LEFT ? x : Edge.LEFT.getCoordinate();
        float top = mHorizontalEdge == Edge.TOP ? y : Edge.TOP.getCoordinate();
        float right = mVerticalEdge == Edge.RIGHT ? x : Edge.RIGHT.getCoordinate();
        float bottom = mHorizontalEdge == Edge.BOTTOM ? y : Edge.BOTTOM.getCoordinate();

        float aspectRatio = AspectRatioUtil.calculateAspectRatio(left, top, right, bottom);

        return aspectRatio;
    }
}
