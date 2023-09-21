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

import android.graphics.*

/**
 * Handler to update crop window edges by the move type - Horizontal, Vertical, Corner or Center.
 * <br></br>
 */
internal class CropWindowMoveHandler(
        /** The type of crop window move that is handled.  */
        private val mType: Type, cropWindowHandler: CropWindowHandler, touchX: Float, touchY: Float) {
    /** Minimum width in pixels that the crop window can get.  */
    private val mMinCropWidth: Float = cropWindowHandler.minCropWidth

    /** Minimum width in pixels that the crop window can get.  */
    private val mMinCropHeight: Float = cropWindowHandler.minCropHeight

    /** Maximum height in pixels that the crop window can get.  */
    private val mMaxCropWidth: Float = cropWindowHandler.maxCropWidth

    /** Maximum height in pixels that the crop window can get.  */
    private val mMaxCropHeight: Float = cropWindowHandler.maxCropHeight

    /**
     * Holds the x and y offset between the exact touch location and the exact handle location that is
     * activated. There may be an offset because we allow for some leeway (specified by mHandleRadius)
     * in activating a handle. However, we want to maintain these offset values while the handle is
     * being dragged so that the handle doesn't jump.
     */
    private val mTouchOffset = PointF()

    /**
     * Updates the crop window by change in the toch location.<br></br>
     * Move type handled by this instance, as initialized in creation, affects how the change in toch
     * location changes the crop window position and size.<br></br>
     * After the crop window position/size is changed by toch move it may result in values that
     * vialate contraints: outside the bounds of the shown bitmap, smaller/larger than min/max size or
     * missmatch in aspect ratio. So a series of fixes is executed on "secondary" edges to adjust it
     * by the "primary" edge movement.<br></br>
     * Primary is the edge directly affected by move type, secondary is the other edge.<br></br>
     * The crop window is changed by directly setting the Edge coordinates.
     *
     * @param x the new x-coordinate of this handle
     * @param y the new y-coordinate of this handle
     * @param bounds the bounding rectangle of the image
     * @param viewWidth The bounding image view width used to know the crop overlay is at view edges.
     * @param viewHeight The bounding image view height used to know the crop overlay is at view
     * edges.
     * @param parentView the parent View containing the image
     * @param snapMargin the maximum distance (in pixels) at which the crop window should snap to the
     * image
     * @param fixedAspectRatio is the aspect ration fixed and 'targetAspectRatio' should be used
     * @param aspectRatio the aspect ratio to maintain
     */
    fun move(
            rect: RectF?,
            x: Float,
            y: Float,
            bounds: RectF,
            viewWidth: Int,
            viewHeight: Int,
            snapMargin: Float,
            fixedAspectRatio: Boolean,
            aspectRatio: Float) {

        // Adjust the coordinates for the finger position's offset (i.e. the
        // distance from the initial touch to the precise handle location).
        // We want to maintain the initial touch's distance to the pressed
        // handle so that the crop window size does not "jump".
        val adjX = x + mTouchOffset.x
        val adjY = y + mTouchOffset.y
        if (mType == Type.CENTER) {
            moveCenter(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin)
        } else {
            if (fixedAspectRatio) {
                moveSizeWithFixedAspectRatio(
                        rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin, aspectRatio)
            } else {
                moveSizeWithFreeAspectRatio(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin)
            }
        }
    }
    // region: Private methods
    /**
     * Calculates the offset of the touch point from the precise location of the specified handle.<br></br>
     * Save these values in a member variable since we want to maintain this offset as we drag the
     * handle.
     */
    private fun calculateTouchOffset(rect: RectF?, touchX: Float, touchY: Float) {
        var touchOffsetX = 0f
        var touchOffsetY = 0f
        when (mType) {
            Type.TOP_LEFT -> {
                touchOffsetX = rect!!.left - touchX
                touchOffsetY = rect.top - touchY
            }
            Type.TOP_RIGHT -> {
                touchOffsetX = rect!!.right - touchX
                touchOffsetY = rect.top - touchY
            }
            Type.BOTTOM_LEFT -> {
                touchOffsetX = rect!!.left - touchX
                touchOffsetY = rect.bottom - touchY
            }
            Type.BOTTOM_RIGHT -> {
                touchOffsetX = rect!!.right - touchX
                touchOffsetY = rect.bottom - touchY
            }
            Type.LEFT -> {
                touchOffsetX = rect!!.left - touchX
                touchOffsetY = 0f
            }
            Type.TOP -> {
                touchOffsetX = 0f
                touchOffsetY = rect!!.top - touchY
            }
            Type.RIGHT -> {
                touchOffsetX = rect!!.right - touchX
                touchOffsetY = 0f
            }
            Type.BOTTOM -> {
                touchOffsetX = 0f
                touchOffsetY = rect!!.bottom - touchY
            }
            Type.CENTER -> {
                touchOffsetX = rect!!.centerX() - touchX
                touchOffsetY = rect.centerY() - touchY
            }
            else -> {
            }
        }
        mTouchOffset.x = touchOffsetX
        mTouchOffset.y = touchOffsetY
    }

    /** Center move only changes the position of the crop window without changing the size.  */
    private fun moveCenter(
            rect: RectF?, x: Float, y: Float, bounds: RectF, viewWidth: Int, viewHeight: Int, snapRadius: Float) {
        var dx = x - rect!!.centerX()
        var dy = y - rect.centerY()
        if (rect.left + dx < 0 || rect.right + dx > viewWidth || rect.left + dx < bounds.left || rect.right + dx > bounds.right) {
            dx /= 1.05f
            mTouchOffset.x -= dx / 2
        }
        if (rect.top + dy < 0 || rect.bottom + dy > viewHeight || rect.top + dy < bounds.top || rect.bottom + dy > bounds.bottom) {
            dy /= 1.05f
            mTouchOffset.y -= dy / 2
        }
        rect.offset(dx, dy)
        snapEdgesToBounds(rect, bounds, snapRadius)
    }

    /**
     * Change the size of the crop window on the required edge (or edges for corner size move) without
     * affecting "secondary" edges.<br></br>
     * Only the primary edge(s) are fixed to stay within limits.
     */
    private fun moveSizeWithFreeAspectRatio(
            rect: RectF?, x: Float, y: Float, bounds: RectF, viewWidth: Int, viewHeight: Int, snapMargin: Float) {
        when (mType) {
            Type.TOP_LEFT -> {
                adjustTop(rect, y, bounds, snapMargin, 0f, false, false)
                adjustLeft(rect, x, bounds, snapMargin, 0f, false, false)
            }
            Type.TOP_RIGHT -> {
                adjustTop(rect, y, bounds, snapMargin, 0f, false, false)
                adjustRight(rect, x, bounds, viewWidth, snapMargin, 0f, false, false)
            }
            Type.BOTTOM_LEFT -> {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, 0f, false, false)
                adjustLeft(rect, x, bounds, snapMargin, 0f, false, false)
            }
            Type.BOTTOM_RIGHT -> {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, 0f, false, false)
                adjustRight(rect, x, bounds, viewWidth, snapMargin, 0f, false, false)
            }
            Type.LEFT -> adjustLeft(rect, x, bounds, snapMargin, 0f, false, false)
            Type.TOP -> adjustTop(rect, y, bounds, snapMargin, 0f, false, false)
            Type.RIGHT -> adjustRight(rect, x, bounds, viewWidth, snapMargin, 0f, false, false)
            Type.BOTTOM -> adjustBottom(rect, y, bounds, viewHeight, snapMargin, 0f, false, false)
            else -> {
            }
        }
    }

    /**
     * Change the size of the crop window on the required "primary" edge WITH affect to relevant
     * "secondary" edge via aspect ratio.<br></br>
     * Example: change in the left edge (primary) will affect top and bottom edges (secondary) to
     * preserve the given aspect ratio.
     */
    private fun moveSizeWithFixedAspectRatio(
            rect: RectF?,
            x: Float,
            y: Float,
            bounds: RectF,
            viewWidth: Int,
            viewHeight: Int,
            snapMargin: Float,
            aspectRatio: Float) {
        when (mType) {
            Type.TOP_LEFT -> if (calculateAspectRatio(x, y, rect!!.right, rect.bottom) < aspectRatio) {
                adjustTop(rect, y, bounds, snapMargin, aspectRatio, true, false)
                adjustLeftByAspectRatio(rect, aspectRatio)
            } else {
                adjustLeft(rect, x, bounds, snapMargin, aspectRatio, true, false)
                adjustTopByAspectRatio(rect, aspectRatio)
            }
            Type.TOP_RIGHT -> if (calculateAspectRatio(rect!!.left, y, x, rect.bottom) < aspectRatio) {
                adjustTop(rect, y, bounds, snapMargin, aspectRatio, false, true)
                adjustRightByAspectRatio(rect, aspectRatio)
            } else {
                adjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio, true, false)
                adjustTopByAspectRatio(rect, aspectRatio)
            }
            Type.BOTTOM_LEFT -> if (calculateAspectRatio(x, rect!!.top, rect.right, y) < aspectRatio) {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio, true, false)
                adjustLeftByAspectRatio(rect, aspectRatio)
            } else {
                adjustLeft(rect, x, bounds, snapMargin, aspectRatio, false, true)
                adjustBottomByAspectRatio(rect, aspectRatio)
            }
            Type.BOTTOM_RIGHT -> if (calculateAspectRatio(rect!!.left, rect.top, x, y) < aspectRatio) {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio, false, true)
                adjustRightByAspectRatio(rect, aspectRatio)
            } else {
                adjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio, false, true)
                adjustBottomByAspectRatio(rect, aspectRatio)
            }
            Type.LEFT -> {
                adjustLeft(rect, x, bounds, snapMargin, aspectRatio, true, true)
                adjustTopBottomByAspectRatio(rect, bounds, aspectRatio)
            }
            Type.TOP -> {
                adjustTop(rect, y, bounds, snapMargin, aspectRatio, true, true)
                adjustLeftRightByAspectRatio(rect, bounds, aspectRatio)
            }
            Type.RIGHT -> {
                adjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio, true, true)
                adjustTopBottomByAspectRatio(rect, bounds, aspectRatio)
            }
            Type.BOTTOM -> {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio, true, true)
                adjustLeftRightByAspectRatio(rect, bounds, aspectRatio)
            }
            else -> {
            }
        }
    }

    /** Check if edges have gone out of bounds (including snap margin), and fix if needed.  */
    private fun snapEdgesToBounds(edges: RectF?, bounds: RectF, margin: Float) {
        if (edges!!.left < bounds.left + margin) {
            edges.offset(bounds.left - edges.left, 0f)
        }
        if (edges.top < bounds.top + margin) {
            edges.offset(0f, bounds.top - edges.top)
        }
        if (edges.right > bounds.right - margin) {
            edges.offset(bounds.right - edges.right, 0f)
        }
        if (edges.bottom > bounds.bottom - margin) {
            edges.offset(0f, bounds.bottom - edges.bottom)
        }
    }

    /**
     * Get the resulting x-position of the left edge of the crop window given the handle's position
     * and the image's bounding box and snap radius.
     *
     * @param left the position that the left edge is dragged to
     * @param bounds the bounding box of the image that is being cropped
     * @param snapMargin the snap distance to the image edge (in pixels)
     */
    private fun adjustLeft(
            rect: RectF?,
            left: Float,
            bounds: RectF,
            snapMargin: Float,
            aspectRatio: Float,
            topMoves: Boolean,
            bottomMoves: Boolean) {
        var newLeft = left
        if (newLeft < 0) {
            newLeft /= 1.05f
            mTouchOffset.x -= newLeft / 1.1f
        }
        if (newLeft < bounds.left) {
            mTouchOffset.x -= (newLeft - bounds.left) / 2f
        }
        if (newLeft - bounds.left < snapMargin) {
            newLeft = bounds.left
        }

        // Checks if the window is too small horizontally
        if (rect!!.right - newLeft < mMinCropWidth) {
            newLeft = rect.right - mMinCropWidth
        }

        // Checks if the window is too large horizontally
        if (rect.right - newLeft > mMaxCropWidth) {
            newLeft = rect.right - mMaxCropWidth
        }
        if (newLeft - bounds.left < snapMargin) {
            newLeft = bounds.left
        }

        // check vertical bounds if aspect ratio is in play
        if (aspectRatio > 0) {
            var newHeight = (rect.right - newLeft) / aspectRatio

            // Checks if the window is too small vertically
            if (newHeight < mMinCropHeight) {
                newLeft = Math.max(bounds.left, rect.right - mMinCropHeight * aspectRatio)
                newHeight = (rect.right - newLeft) / aspectRatio
            }

            // Checks if the window is too large vertically
            if (newHeight > mMaxCropHeight) {
                newLeft = Math.max(bounds.left, rect.right - mMaxCropHeight * aspectRatio)
                newHeight = (rect.right - newLeft) / aspectRatio
            }

            // if top AND bottom edge moves by aspect ratio check that it is within full height bounds
            if (topMoves && bottomMoves) {
                newLeft = Math.max(newLeft, Math.max(bounds.left, rect.right - bounds.height() * aspectRatio))
            } else {
                // if top edge moves by aspect ratio check that it is within bounds
                if (topMoves && rect.bottom - newHeight < bounds.top) {
                    newLeft = Math.max(bounds.left, rect.right - (rect.bottom - bounds.top) * aspectRatio)
                    newHeight = (rect.right - newLeft) / aspectRatio
                }

                // if bottom edge moves by aspect ratio check that it is within bounds
                if (bottomMoves && rect.top + newHeight > bounds.bottom) {
                    newLeft = Math.max(
                            newLeft,
                            Math.max(bounds.left, rect.right - (bounds.bottom - rect.top) * aspectRatio))
                }
            }
        }
        rect.left = newLeft
    }

    /**
     * Get the resulting x-position of the right edge of the crop window given the handle's position
     * and the image's bounding box and snap radius.
     *
     * @param right the position that the right edge is dragged to
     * @param bounds the bounding box of the image that is being cropped
     * @param viewWidth
     * @param snapMargin the snap distance to the image edge (in pixels)
     */
    private fun adjustRight(
            rect: RectF?,
            right: Float,
            bounds: RectF,
            viewWidth: Int,
            snapMargin: Float,
            aspectRatio: Float,
            topMoves: Boolean,
            bottomMoves: Boolean) {
        var newRight = right
        if (newRight > viewWidth) {
            newRight = viewWidth + (newRight - viewWidth) / 1.05f
            mTouchOffset.x -= (newRight - viewWidth) / 1.1f
        }
        if (newRight > bounds.right) {
            mTouchOffset.x -= (newRight - bounds.right) / 2f
        }

        // If close to the edge
        if (bounds.right - newRight < snapMargin) {
            newRight = bounds.right
        }

        // Checks if the window is too small horizontally
        if (newRight - rect!!.left < mMinCropWidth) {
            newRight = rect.left + mMinCropWidth
        }

        // Checks if the window is too large horizontally
        if (newRight - rect.left > mMaxCropWidth) {
            newRight = rect.left + mMaxCropWidth
        }

        // If close to the edge
        if (bounds.right - newRight < snapMargin) {
            newRight = bounds.right
        }

        // check vertical bounds if aspect ratio is in play
        if (aspectRatio > 0) {
            var newHeight = (newRight - rect.left) / aspectRatio

            // Checks if the window is too small vertically
            if (newHeight < mMinCropHeight) {
                newRight = Math.min(bounds.right, rect.left + mMinCropHeight * aspectRatio)
                newHeight = (newRight - rect.left) / aspectRatio
            }

            // Checks if the window is too large vertically
            if (newHeight > mMaxCropHeight) {
                newRight = Math.min(bounds.right, rect.left + mMaxCropHeight * aspectRatio)
                newHeight = (newRight - rect.left) / aspectRatio
            }

            // if top AND bottom edge moves by aspect ratio check that it is within full height bounds
            if (topMoves && bottomMoves) {
                newRight = Math.min(newRight, Math.min(bounds.right, rect.left + bounds.height() * aspectRatio))
            } else {
                // if top edge moves by aspect ratio check that it is within bounds
                if (topMoves && rect.bottom - newHeight < bounds.top) {
                    newRight = Math.min(bounds.right, rect.left + (rect.bottom - bounds.top) * aspectRatio)
                    newHeight = (newRight - rect.left) / aspectRatio
                }

                // if bottom edge moves by aspect ratio check that it is within bounds
                if (bottomMoves && rect.top + newHeight > bounds.bottom) {
                    newRight = Math.min(
                            newRight,
                            Math.min(bounds.right, rect.left + (bounds.bottom - rect.top) * aspectRatio))
                }
            }
        }
        rect.right = newRight
    }

    /**
     * Get the resulting y-position of the top edge of the crop window given the handle's position and
     * the image's bounding box and snap radius.
     *
     * @param top the x-position that the top edge is dragged to
     * @param bounds the bounding box of the image that is being cropped
     * @param snapMargin the snap distance to the image edge (in pixels)
     */
    private fun adjustTop(
            rect: RectF?,
            top: Float,
            bounds: RectF,
            snapMargin: Float,
            aspectRatio: Float,
            leftMoves: Boolean,
            rightMoves: Boolean) {
        var newTop = top
        if (newTop < 0) {
            newTop /= 1.05f
            mTouchOffset.y -= newTop / 1.1f
        }
        if (newTop < bounds.top) {
            mTouchOffset.y -= (newTop - bounds.top) / 2f
        }
        if (newTop - bounds.top < snapMargin) {
            newTop = bounds.top
        }

        // Checks if the window is too small vertically
        if (rect!!.bottom - newTop < mMinCropHeight) {
            newTop = rect.bottom - mMinCropHeight
        }

        // Checks if the window is too large vertically
        if (rect.bottom - newTop > mMaxCropHeight) {
            newTop = rect.bottom - mMaxCropHeight
        }
        if (newTop - bounds.top < snapMargin) {
            newTop = bounds.top
        }

        // check horizontal bounds if aspect ratio is in play
        if (aspectRatio > 0) {
            var newWidth = (rect.bottom - newTop) * aspectRatio

            // Checks if the crop window is too small horizontally due to aspect ratio adjustment
            if (newWidth < mMinCropWidth) {
                newTop = Math.max(bounds.top, rect.bottom - mMinCropWidth / aspectRatio)
                newWidth = (rect.bottom - newTop) * aspectRatio
            }

            // Checks if the crop window is too large horizontally due to aspect ratio adjustment
            if (newWidth > mMaxCropWidth) {
                newTop = Math.max(bounds.top, rect.bottom - mMaxCropWidth / aspectRatio)
                newWidth = (rect.bottom - newTop) * aspectRatio
            }

            // if left AND right edge moves by aspect ratio check that it is within full width bounds
            if (leftMoves && rightMoves) {
                newTop = Math.max(newTop, Math.max(bounds.top, rect.bottom - bounds.width() / aspectRatio))
            } else {
                // if left edge moves by aspect ratio check that it is within bounds
                if (leftMoves && rect.right - newWidth < bounds.left) {
                    newTop = Math.max(bounds.top, rect.bottom - (rect.right - bounds.left) / aspectRatio)
                    newWidth = (rect.bottom - newTop) * aspectRatio
                }

                // if right edge moves by aspect ratio check that it is within bounds
                if (rightMoves && rect.left + newWidth > bounds.right) {
                    newTop = Math.max(
                            newTop,
                            Math.max(bounds.top, rect.bottom - (bounds.right - rect.left) / aspectRatio))
                }
            }
        }
        rect.top = newTop
    }

    /**
     * Get the resulting y-position of the bottom edge of the crop window given the handle's position
     * and the image's bounding box and snap radius.
     *
     * @param bottom the position that the bottom edge is dragged to
     * @param bounds the bounding box of the image that is being cropped
     * @param viewHeight
     * @param snapMargin the snap distance to the image edge (in pixels)
     */
    private fun adjustBottom(
            rect: RectF?,
            bottom: Float,
            bounds: RectF,
            viewHeight: Int,
            snapMargin: Float,
            aspectRatio: Float,
            leftMoves: Boolean,
            rightMoves: Boolean) {
        var newBottom = bottom
        if (newBottom > viewHeight) {
            newBottom = viewHeight + (newBottom - viewHeight) / 1.05f
            mTouchOffset.y -= (newBottom - viewHeight) / 1.1f
        }
        if (newBottom > bounds.bottom) {
            mTouchOffset.y -= (newBottom - bounds.bottom) / 2f
        }
        if (bounds.bottom - newBottom < snapMargin) {
            newBottom = bounds.bottom
        }

        // Checks if the window is too small vertically
        if (newBottom - rect!!.top < mMinCropHeight) {
            newBottom = rect.top + mMinCropHeight
        }

        // Checks if the window is too small vertically
        if (newBottom - rect.top > mMaxCropHeight) {
            newBottom = rect.top + mMaxCropHeight
        }
        if (bounds.bottom - newBottom < snapMargin) {
            newBottom = bounds.bottom
        }

        // check horizontal bounds if aspect ratio is in play
        if (aspectRatio > 0) {
            var newWidth = (newBottom - rect.top) * aspectRatio

            // Checks if the window is too small horizontally
            if (newWidth < mMinCropWidth) {
                newBottom = Math.min(bounds.bottom, rect.top + mMinCropWidth / aspectRatio)
                newWidth = (newBottom - rect.top) * aspectRatio
            }

            // Checks if the window is too large horizontally
            if (newWidth > mMaxCropWidth) {
                newBottom = Math.min(bounds.bottom, rect.top + mMaxCropWidth / aspectRatio)
                newWidth = (newBottom - rect.top) * aspectRatio
            }

            // if left AND right edge moves by aspect ratio check that it is within full width bounds
            if (leftMoves && rightMoves) {
                newBottom = Math.min(newBottom, Math.min(bounds.bottom, rect.top + bounds.width() / aspectRatio))
            } else {
                // if left edge moves by aspect ratio check that it is within bounds
                if (leftMoves && rect.right - newWidth < bounds.left) {
                    newBottom = Math.min(bounds.bottom, rect.top + (rect.right - bounds.left) / aspectRatio)
                    newWidth = (newBottom - rect.top) * aspectRatio
                }

                // if right edge moves by aspect ratio check that it is within bounds
                if (rightMoves && rect.left + newWidth > bounds.right) {
                    newBottom = Math.min(
                            newBottom,
                            Math.min(bounds.bottom, rect.top + (bounds.right - rect.left) / aspectRatio))
                }
            }
        }
        rect.bottom = newBottom
    }

    /**
     * Adjust left edge by current crop window height and the given aspect ratio, the right edge
     * remains in possition while the left adjusts to keep aspect ratio to the height.
     */
    private fun adjustLeftByAspectRatio(rect: RectF?, aspectRatio: Float) {
        rect!!.left = rect.right - rect.height() * aspectRatio
    }

    /**
     * Adjust top edge by current crop window width and the given aspect ratio, the bottom edge
     * remains in possition while the top adjusts to keep aspect ratio to the width.
     */
    private fun adjustTopByAspectRatio(rect: RectF?, aspectRatio: Float) {
        rect!!.top = rect.bottom - rect.width() / aspectRatio
    }

    /**
     * Adjust right edge by current crop window height and the given aspect ratio, the left edge
     * remains in possition while the left adjusts to keep aspect ratio to the height.
     */
    private fun adjustRightByAspectRatio(rect: RectF?, aspectRatio: Float) {
        rect!!.right = rect.left + rect.height() * aspectRatio
    }

    /**
     * Adjust bottom edge by current crop window width and the given aspect ratio, the top edge
     * remains in possition while the top adjusts to keep aspect ratio to the width.
     */
    private fun adjustBottomByAspectRatio(rect: RectF?, aspectRatio: Float) {
        rect!!.bottom = rect.top + rect.width() / aspectRatio
    }

    /**
     * Adjust left and right edges by current crop window height and the given aspect ratio, both
     * right and left edges adjusts equally relative to center to keep aspect ratio to the height.
     */
    private fun adjustLeftRightByAspectRatio(rect: RectF?, bounds: RectF, aspectRatio: Float) {
        rect!!.inset((rect.width() - rect.height() * aspectRatio) / 2, 0f)
        if (rect.left < bounds.left) {
            rect.offset(bounds.left - rect.left, 0f)
        }
        if (rect.right > bounds.right) {
            rect.offset(bounds.right - rect.right, 0f)
        }
    }

    /**
     * Adjust top and bottom edges by current crop window width and the given aspect ratio, both top
     * and bottom edges adjusts equally relative to center to keep aspect ratio to the width.
     */
    private fun adjustTopBottomByAspectRatio(rect: RectF?, bounds: RectF, aspectRatio: Float) {
        rect!!.inset(0f, (rect.height() - rect.width() / aspectRatio) / 2)
        if (rect.top < bounds.top) {
            rect.offset(0f, bounds.top - rect.top)
        }
        if (rect.bottom > bounds.bottom) {
            rect.offset(0f, bounds.bottom - rect.bottom)
        }
    }
    // endregion
    // region: Inner class: Type
    /** The type of crop window move that is handled.  */
    enum class Type {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LEFT, TOP, RIGHT, BOTTOM, CENTER
    } // endregion

    companion object {
        // region: Fields and Consts
        /** Matrix used for rectangle rotation handling  */
        private val MATRIX = Matrix()

        /** Calculates the aspect ratio given a rectangle.  */
        private fun calculateAspectRatio(left: Float, top: Float, right: Float, bottom: Float): Float {
            return (right - left) / (bottom - top)
        }
    }
    // endregion
    /**
     * @param edgeMoveType the type of move this handler is executing
     * @param horizontalEdge the primary edge associated with this handle; may be null
     * @param verticalEdge the secondary edge associated with this handle; may be null
     * @param cropWindowHandler main crop window handle to get and update the crop window edges
     * @param touchX the location of the initial toch possition to measure move distance
     * @param touchY the location of the initial toch possition to measure move distance
     */
    init {
        calculateTouchOffset(cropWindowHandler.rect, touchX, touchY)
    }
}