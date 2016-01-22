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

/**
 * Handler to update crop window edges by the move type - Horizontal, Vertical, Corner or Center.<br/>
 */
class CropWindowMoveHandler {

    //region: Fields and Consts

    /**
     * Horizontal edge of the crop window, can be left or right esge.
     */
    private final Edge mHorizontalEdge;

    /**
     * Vertical edge of the crop window, can be top or bottom esge.
     */
    private final Edge mVerticalEdge;
    //endregion

    /**
     * @param edgeMoveType the type of move this handler is executing
     * @param horizontalEdge the primary edge associated with this handle; may be null
     * @param verticalEdge the secondary edge associated with this handle; may be null
     */
    public CropWindowMoveHandler(Edge horizontalEdge, Edge verticalEdge) {
        mHorizontalEdge = horizontalEdge;
        mVerticalEdge = verticalEdge;
    }

    /**
     * Updates the crop window by directly setting the Edge coordinates.
     *
     * @param x the new x-coordinate of this handle
     * @param y the new y-coordinate of this handle
     * @param imageRect the bounding rectangle of the image
     * @param parentView the parent View containing the image
     * @param snapRadius the maximum distance (in pixels) at which the crop window should snap to the image
     * @param fixAspectRatio is the aspect ration fixed and 'targetAspectRatio' should be used
     * @param targetAspectRatio the aspect ratio to maintain
     */
    public void move(float x, float y, Rect imageRect, float snapRadius, boolean fixAspectRatio, float targetAspectRatio) {
        if (mHorizontalEdge == null && mVerticalEdge == null) {
            moveCenter(x, y, imageRect, snapRadius);
        } else {
            if (fixAspectRatio) {
                if (mHorizontalEdge != null && mVerticalEdge != null) {
                    moveCorner(x, y, imageRect, snapRadius, targetAspectRatio);
                } else if (mHorizontalEdge != null) {
                    moveHorizontal(x, y, imageRect, snapRadius, targetAspectRatio);
                } else {
                    moveVertical(x, y, imageRect, snapRadius, targetAspectRatio);
                }
            } else {
                if (mHorizontalEdge != null) {
                    mHorizontalEdge.adjustCoordinate(x, y, imageRect, snapRadius, 0);
                }
                if (mVerticalEdge != null) {
                    mVerticalEdge.adjustCoordinate(x, y, imageRect, snapRadius, 0);
                }
            }
        }
    }

    //region: Private methods

    private void moveCenter(float x, float y, Rect imageRect, float snapRadius) {

        float left = Edge.LEFT.getCoordinate();
        float top = Edge.TOP.getCoordinate();
        float right = Edge.RIGHT.getCoordinate();
        float bottom = Edge.BOTTOM.getCoordinate();

        final float currentCenterX = (left + right) / 2;
        final float currentCenterY = (top + bottom) / 2;

        final float offsetX = x - currentCenterX;
        final float offsetY = y - currentCenterY;

        // Adjust the crop window.
        Edge.LEFT.offset(offsetX);
        Edge.TOP.offset(offsetY);
        Edge.RIGHT.offset(offsetX);
        Edge.BOTTOM.offset(offsetY);

        // Check if we have gone out of bounds on the sides, and fix.
        if (Edge.LEFT.isOutsideMargin(imageRect, snapRadius)) {
            final float offset = Edge.LEFT.snapToRect(imageRect);
            Edge.RIGHT.offset(offset);
        } else if (Edge.RIGHT.isOutsideMargin(imageRect, snapRadius)) {
            final float offset = Edge.RIGHT.snapToRect(imageRect);
            Edge.LEFT.offset(offset);
        }

        // Check if we have gone out of bounds on the top or bottom, and fix.
        if (Edge.TOP.isOutsideMargin(imageRect, snapRadius)) {
            final float offset = Edge.TOP.snapToRect(imageRect);
            Edge.BOTTOM.offset(offset);
        } else if (Edge.BOTTOM.isOutsideMargin(imageRect, snapRadius)) {
            final float offset = Edge.BOTTOM.snapToRect(imageRect);
            Edge.TOP.offset(offset);
        }
    }

    private void moveCorner(float x, float y, Rect imageRect, float snapRadius, float targetAspectRatio) {

        float potentialAspectRatio = getAspectRatio(x, y);
        Edge primaryEdge = potentialAspectRatio > targetAspectRatio ? mVerticalEdge : mHorizontalEdge;
        Edge secondaryEdge = potentialAspectRatio > targetAspectRatio ? mHorizontalEdge : mVerticalEdge;

        primaryEdge.adjustCoordinate(x, y, imageRect, snapRadius, targetAspectRatio);
        secondaryEdge.adjustCoordinate(targetAspectRatio);

        if (secondaryEdge.isOutsideMargin(imageRect, snapRadius)) {
            secondaryEdge.snapToRect(imageRect);
            primaryEdge.adjustCoordinate(targetAspectRatio);
        }
    }

    private void moveHorizontal(float x, float y, Rect imageRect, float snapRadius, float targetAspectRatio) {

        // Adjust this Edge accordingly.
        mHorizontalEdge.adjustCoordinate(x, y, imageRect, snapRadius, targetAspectRatio);

        float left = Edge.LEFT.getCoordinate();
        float top = Edge.TOP.getCoordinate();
        float right = Edge.RIGHT.getCoordinate();
        float bottom = Edge.BOTTOM.getCoordinate();

        // After this Edge is moved, our crop window is now out of proportion.
        final float targetWidth = AspectRatioUtil.calculateWidth(top, bottom, targetAspectRatio);
        final float currentWidth = right - left;

        // Adjust the crop window so that it maintains the given aspect ratio by
        // moving the adjacent edges symmetrically in or out.
        final float difference = targetWidth - currentWidth;
        final float halfDifference = difference / 2;
        left -= halfDifference;
        right += halfDifference;

        Edge.LEFT.setCoordinate(left);
        Edge.RIGHT.setCoordinate(right);

        // Check if we have gone out of bounds on the sides, and fix.
        if (Edge.LEFT.isOutsideMargin(imageRect, snapRadius) && !mHorizontalEdge.isNewRectangleOutOfBounds(Edge.LEFT,
                imageRect,
                targetAspectRatio)) {
            final float offset = Edge.LEFT.snapToRect(imageRect);
            Edge.RIGHT.offset(-offset);
            mHorizontalEdge.adjustCoordinate(targetAspectRatio);

        }
        if (Edge.RIGHT.isOutsideMargin(imageRect, snapRadius) && !mHorizontalEdge.isNewRectangleOutOfBounds(Edge.RIGHT,
                imageRect,
                targetAspectRatio)) {
            final float offset = Edge.RIGHT.snapToRect(imageRect);
            Edge.LEFT.offset(-offset);
            mHorizontalEdge.adjustCoordinate(targetAspectRatio);
        }
    }

    private void moveVertical(float x, float y, Rect imageRect, float snapRadius, float targetAspectRatio) {

        // Adjust this Edge accordingly.
        mVerticalEdge.adjustCoordinate(x, y, imageRect, snapRadius, targetAspectRatio);

        float left = Edge.LEFT.getCoordinate();
        float top = Edge.TOP.getCoordinate();
        float right = Edge.RIGHT.getCoordinate();
        float bottom = Edge.BOTTOM.getCoordinate();

        // After this Edge is moved, our crop window is now out of proportion.
        final float targetHeight = AspectRatioUtil.calculateHeight(left, right, targetAspectRatio);
        final float currentHeight = bottom - top;

        // Adjust the crop window so that it maintains the given aspect ratio by
        // moving the adjacent edges symmetrically in or out.
        final float difference = targetHeight - currentHeight;
        final float halfDifference = difference / 2;
        top -= halfDifference;
        bottom += halfDifference;

        Edge.TOP.setCoordinate(top);
        Edge.BOTTOM.setCoordinate(bottom);

        // Check if we have gone out of bounds on the top or bottom, and fix.
        if (Edge.TOP.isOutsideMargin(imageRect, snapRadius) &&
                !mVerticalEdge.isNewRectangleOutOfBounds(Edge.TOP, imageRect, targetAspectRatio)) {
            float offset = Edge.TOP.snapToRect(imageRect);
            Edge.BOTTOM.offset(-offset);
            mVerticalEdge.adjustCoordinate(targetAspectRatio);
        }
        if (Edge.BOTTOM.isOutsideMargin(imageRect, snapRadius) &&
                !mVerticalEdge.isNewRectangleOutOfBounds(Edge.BOTTOM, imageRect, targetAspectRatio)) {
            float offset = Edge.BOTTOM.snapToRect(imageRect);
            Edge.TOP.offset(-offset);
            mVerticalEdge.adjustCoordinate(targetAspectRatio);
        }
    }

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
    //endregion
}