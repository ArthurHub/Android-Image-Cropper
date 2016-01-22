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

import android.util.Pair;

/**
 * Handler from crop window stuff, moving and knowing possition.
 */
class CropWindowHandler {

    //region: Fields and Consts

    private final CropWindowMoveHandler mTopLeftMoveHandler = new CropWindowMoveHandler(Edge.TOP, Edge.LEFT);

    private final CropWindowMoveHandler mTopRightMoveHandler = new CropWindowMoveHandler(Edge.TOP, Edge.RIGHT);

    private final CropWindowMoveHandler mBottomLeftMoveHandler = new CropWindowMoveHandler(Edge.BOTTOM, Edge.LEFT);

    private final CropWindowMoveHandler mBottomRightMoveHandler = new CropWindowMoveHandler(Edge.BOTTOM, Edge.RIGHT);

    private final CropWindowMoveHandler mLeftMoveHandler = new CropWindowMoveHandler(null, Edge.LEFT);

    private final CropWindowMoveHandler mTopMoveHandler = new CropWindowMoveHandler(Edge.TOP, null);

    private final CropWindowMoveHandler mRightMoveHandler = new CropWindowMoveHandler(null, Edge.RIGHT);

    private final CropWindowMoveHandler mBottomMoveHandler = new CropWindowMoveHandler(Edge.BOTTOM, null);

    private final CropWindowMoveHandler mCenterMoveHandler = new CropWindowMoveHandler(null, null);

    //private final Edge mLeft = new ;

    //endregion

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
    public CropWindowMoveHandler getPressedHandle(float x, float y, float left, float top, float right, float bottom, float targetRadius, CropImageView.CropShape cropShape) {
        return cropShape == CropImageView.CropShape.OVAL
                ? getOvalPressedHandle(x, y, left, top, right, bottom)
                : getRectanglePressedHandle(x, y, left, top, right, bottom, targetRadius);
    }

    //region: Private methods

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
    private CropWindowMoveHandler getRectanglePressedHandle(float x, float y, float left, float top, float right, float bottom, float targetRadius) {
        CropWindowMoveHandler pressedHandle = null;

        // Note: corner-handles take precedence, then side-handles, then center.
        if (CropWindowHandler.isInCornerTargetZone(x, y, left, top, targetRadius)) {
            pressedHandle = mTopLeftMoveHandler;
        } else if (CropWindowHandler.isInCornerTargetZone(x, y, right, top, targetRadius)) {
            pressedHandle = mTopRightMoveHandler;
        } else if (CropWindowHandler.isInCornerTargetZone(x, y, left, bottom, targetRadius)) {
            pressedHandle = mBottomLeftMoveHandler;
        } else if (CropWindowHandler.isInCornerTargetZone(x, y, right, bottom, targetRadius)) {
            pressedHandle = mBottomRightMoveHandler;
        } else if (CropWindowHandler.isInCenterTargetZone(x, y, left, top, right, bottom) && focusCenter()) {
            pressedHandle = mCenterMoveHandler;
        } else if (CropWindowHandler.isInHorizontalTargetZone(x, y, left, right, top, targetRadius)) {
            pressedHandle = mTopMoveHandler;
        } else if (CropWindowHandler.isInHorizontalTargetZone(x, y, left, right, bottom, targetRadius)) {
            pressedHandle = mBottomMoveHandler;
        } else if (CropWindowHandler.isInVerticalTargetZone(x, y, left, top, bottom, targetRadius)) {
            pressedHandle = mLeftMoveHandler;
        } else if (CropWindowHandler.isInVerticalTargetZone(x, y, right, top, bottom, targetRadius)) {
            pressedHandle = mRightMoveHandler;
        } else if (CropWindowHandler.isInCenterTargetZone(x, y, left, top, right, bottom) && !focusCenter()) {
            pressedHandle = mCenterMoveHandler;
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
    private CropWindowMoveHandler getOvalPressedHandle(float x, float y, float left, float top, float right, float bottom) {

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
        float cellLength = (right - left) / 6;
        float leftCenter = left + cellLength;
        float rightCenter = left + (5 * cellLength);

        float cellHeight = (bottom - top) / 6;
        float topCenter = top + cellHeight;
        float bottomCenter = top + 5 * cellHeight;

        CropWindowMoveHandler pressedHandle;
        if (x < leftCenter) {
            if (y < topCenter) {
                pressedHandle = mTopLeftMoveHandler;
            } else if (y < bottomCenter) {
                pressedHandle = mLeftMoveHandler;
            } else {
                pressedHandle = mBottomLeftMoveHandler;
            }
        } else if (x < rightCenter) {
            if (y < topCenter) {
                pressedHandle = mTopMoveHandler;
            } else if (y < bottomCenter) {
                pressedHandle = mCenterMoveHandler;
            } else {
                pressedHandle = mBottomMoveHandler;
            }
        } else {
            if (y < topCenter) {
                pressedHandle = mTopRightMoveHandler;
            } else if (y < bottomCenter) {
                pressedHandle = mRightMoveHandler;
            } else {
                pressedHandle = mBottomRightMoveHandler;
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
    public Pair<Float, Float> getOffset(CropWindowMoveHandler handler, float x, float y, float left, float top, float right, float bottom) {

        if (handler == null) {
            return null;
        }

        float touchOffsetX = 0;
        float touchOffsetY = 0;

        // Calculate the offset from the appropriate handle.
        if (handler == mTopLeftMoveHandler) {
            touchOffsetX = left - x;
            touchOffsetY = top - y;
        } else if (handler == mTopRightMoveHandler) {
            touchOffsetX = right - x;
            touchOffsetY = top - y;
        } else if (handler == mBottomLeftMoveHandler) {
            touchOffsetX = left - x;
            touchOffsetY = bottom - y;
        } else if (handler == mBottomRightMoveHandler) {
            touchOffsetX = right - x;
            touchOffsetY = bottom - y;
        } else if (handler == mLeftMoveHandler) {
            touchOffsetX = left - x;
            touchOffsetY = 0;
        } else if (handler == mTopMoveHandler) {
            touchOffsetX = 0;
            touchOffsetY = top - y;
        } else if (handler == mRightMoveHandler) {
            touchOffsetX = right - x;
            touchOffsetY = 0;
        } else if (handler == mBottomMoveHandler) {
            touchOffsetX = 0;
            touchOffsetY = bottom - y;
        } else if (handler == mCenterMoveHandler) {
            touchOffsetX = (right + left) / 2 - x;
            touchOffsetY = (top + bottom) / 2 - y;
        }

        return new Pair<Float, Float>(touchOffsetX, touchOffsetY);
    }

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
    private static boolean isInCornerTargetZone(float x, float y, float handleX, float handleY, float targetRadius) {
        return Math.abs(x - handleX) <= targetRadius && Math.abs(y - handleY) <= targetRadius;
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
    private static boolean isInHorizontalTargetZone(float x, float y, float handleXStart, float handleXEnd, float handleY, float targetRadius) {
        return x > handleXStart && x < handleXEnd && Math.abs(y - handleY) <= targetRadius;
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
    private static boolean isInVerticalTargetZone(float x, float y, float handleX, float handleYStart, float handleYEnd, float targetRadius) {
        return Math.abs(x - handleX) <= targetRadius && y > handleYStart && y < handleYEnd;
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
    private static boolean isInCenterTargetZone(float x, float y, float left, float top, float right, float bottom) {
        return x > left && x < right && y > top && y < bottom;
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
        return !CropOverlayView.showGuidelines();
    }
    //endregion
}