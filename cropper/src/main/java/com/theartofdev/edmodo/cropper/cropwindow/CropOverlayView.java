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

package com.theartofdev.edmodo.cropper.cropwindow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.theartofdev.edmodo.cropper.CropImageView;
import com.theartofdev.edmodo.cropper.CropShape;
import com.theartofdev.edmodo.cropper.cropwindow.edge.Edge;
import com.theartofdev.edmodo.cropper.cropwindow.handle.Handle;
import com.theartofdev.edmodo.cropper.util.AspectRatioUtil;
import com.theartofdev.edmodo.cropper.util.HandleUtil;
import com.theartofdev.edmodo.cropper.util.PaintUtil;

/**
 * A custom View representing the crop window and the shaded background outside the crop window.
 */
public class CropOverlayView extends View {

    //region: Fields and Consts

    private static final int SNAP_RADIUS_DP = 6;

    private static final float DEFAULT_SHOW_GUIDELINES_LIMIT = 100;

    // Gets default values from PaintUtil, sets a bunch of values such that the
    // corners will draw correctly
    private static final float DEFAULT_CORNER_THICKNESS_DP = PaintUtil.getCornerThickness();

    private static final float DEFAULT_LINE_THICKNESS_DP = PaintUtil.getLineThickness();

    private static final float DEFAULT_CORNER_OFFSET_DP = (DEFAULT_CORNER_THICKNESS_DP / 2) - (DEFAULT_LINE_THICKNESS_DP / 2);

    private static final float DEFAULT_CORNER_EXTENSION_DP = DEFAULT_CORNER_THICKNESS_DP / 2
            + DEFAULT_CORNER_OFFSET_DP;

    private static final float DEFAULT_CORNER_LENGTH_DP = 20;

    private static final int GUIDELINES_ON_TOUCH = 1;

    private static final int GUIDELINES_ON = 2;

    private static RectF mRectF = new RectF();

    /**
     * The Paint used to draw the white rectangle around the crop area.
     */
    private Paint mBorderPaint;

    /**
     * The Paint used to draw the guidelines within the crop area when pressed.
     */
    private Paint mGuidelinePaint;

    /**
     * The Paint used to draw the corners of the Border
     */
    private Paint mCornerPaint;

    /**
     * The Paint used to darken the surrounding areas outside the crop area.
     */
    private Paint mBackgroundPaint;

    /**
     * The bounding box around the Bitmap that we are cropping.
     */
    private Rect mBitmapRect;

    // The radius of the touch zone (in pixels) around a given Handle.
    private float mHandleRadius;

    // An edge of the crop window will snap to the corresponding edge of a
    // specified bounding box when the crop window edge is less than or equal to
    // this distance (in pixels) away from the bounding box edge.
    private float mSnapRadius;

    // Holds the x and y offset between the exact touch location and the exact
    // handle location that is activated. There may be an offset because we
    // allow for some leeway (specified by mHandleRadius) in activating a
    // handle. However, we want to maintain these offset values while the handle
    // is being dragged so that the handle doesn't jump.
    private Pair<Float, Float> mTouchOffset;

    // The Handle that is currently pressed; null if no Handle is pressed.
    private Handle mPressedHandle;

    // Flag indicating if the crop area should always be a certain aspect ratio
    // (indicated by mTargetAspectRatio).
    private boolean mFixAspectRatio = CropImageView.DEFAULT_FIXED_ASPECT_RATIO;

    // Floats to save the current aspect ratio of the image
    private int mAspectRatioX = CropImageView.DEFAULT_ASPECT_RATIO_X;

    private int mAspectRatioY = CropImageView.DEFAULT_ASPECT_RATIO_Y;

    // The aspect ratio that the crop area should maintain; this variable is
    // only used when mMaintainAspectRatio is true.
    private float mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;

    /**
     * Instance variables for customizable attributes
     */
    private int mGuidelines;

    /**
     * The shape of the cropping area - rectangle/circular.
     */
    private CropShape mCropShape;

    // Whether the Crop View has been initialized for the first time
    private boolean initializedCropWindow = false;

    // Instance variables for the corner values
    private float mCornerExtension;

    private float mCornerOffset;

    private float mCornerLength;
    //endregion

    public CropOverlayView(Context context) {
        super(context);
        init(context);
    }

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Informs the CropOverlayView of the image's position relative to the
     * ImageView. This is necessary to call in order to draw the crop window.
     *
     * @param bitmapRect the image's bounding box
     */
    public void setBitmapRect(Rect bitmapRect) {
        mBitmapRect = bitmapRect;
        initCropWindow(mBitmapRect);
    }

    /**
     * Resets the crop overlay view.
     */
    public void resetCropOverlayView() {

        if (initializedCropWindow) {
            initCropWindow(mBitmapRect);
            invalidate();
        }
    }

    /**
     * The shape of the cropping area - rectangle/circular.
     */
    public void setCropShape(CropShape cropShape) {
        mCropShape = cropShape;
        invalidate();
    }

    /**
     * Sets the guidelines for the CropOverlayView to be either on, off, or to
     * show when resizing the application.
     *
     * @param guidelines Integer that signals whether the guidelines should be
     * on, off, or only showing when resizing.
     */
    public void setGuidelines(int guidelines) {
        if (guidelines < 0 || guidelines > 2)
            throw new IllegalArgumentException("Guideline value must be set between 0 and 2. See documentation.");
        else {
            mGuidelines = guidelines;

            if (initializedCropWindow) {
                initCropWindow(mBitmapRect);
                invalidate();
            }
        }
    }

    /**
     * Sets whether the aspect ratio is fixed or not; true fixes the aspect
     * ratio, while false allows it to be changed.
     *
     * @param fixAspectRatio Boolean that signals whether the aspect ratio
     * should be maintained.
     */
    public void setFixedAspectRatio(boolean fixAspectRatio) {
        mFixAspectRatio = fixAspectRatio;

        if (initializedCropWindow) {
            initCropWindow(mBitmapRect);
            invalidate();
        }
    }

    /**
     * Sets the X value of the aspect ratio; is defaulted to 1.
     *
     * @param aspectRatioX int that specifies the new X value of the aspect
     * ratio
     */
    public void setAspectRatioX(int aspectRatioX) {
        if (aspectRatioX <= 0)
            throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0.");
        else {
            mAspectRatioX = aspectRatioX;
            mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;

            if (initializedCropWindow) {
                initCropWindow(mBitmapRect);
                invalidate();
            }
        }
    }

    /**
     * Sets the Y value of the aspect ratio; is defaulted to 1.
     *
     * @param aspectRatioY int that specifies the new Y value of the aspect
     * ratio
     */
    public void setAspectRatioY(int aspectRatioY) {
        if (aspectRatioY <= 0)
            throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0.");
        else {
            mAspectRatioY = aspectRatioY;
            mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;

            if (initializedCropWindow) {
                initCropWindow(mBitmapRect);
                invalidate();
            }
        }
    }

    /**
     * Sets all initial values, but does not call initCropWindow to reset the
     * views. Used once at the very start to initialize the attributes.
     *
     * @param guidelines Integer that signals whether the guidelines should be
     * on, off, or only showing when resizing.
     * @param fixAspectRatio Boolean that signals whether the aspect ratio
     * should be maintained.
     * @param aspectRatioX float that specifies the new X value of the aspect
     * ratio
     * @param aspectRatioY float that specifies the new Y value of the aspect
     * ratio
     */
    public void setInitialAttributeValues(int guidelines, boolean fixAspectRatio, int aspectRatioX, int aspectRatioY) {
        if (guidelines < 0 || guidelines > 2)
            throw new IllegalArgumentException("Guideline value must be set between 0 and 2. See documentation.");
        else
            mGuidelines = guidelines;

        mFixAspectRatio = fixAspectRatio;

        if (aspectRatioX <= 0)
            throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0.");
        else {
            mAspectRatioX = aspectRatioX;
            mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;
        }

        if (aspectRatioY <= 0)
            throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0.");
        else {
            mAspectRatioY = aspectRatioY;
            mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;
        }

    }

    //region: Private methods
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        // Initialize the crop window here because we need the size of the view
        // to have been determined.
        initCropWindow(mBitmapRect);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        // Draw translucent background for the cropped area.
        drawBackground(canvas, mBitmapRect);

        if (showGuidelines()) {
            // Determines whether guidelines should be drawn or not
            if (mGuidelines == GUIDELINES_ON) {
                drawRuleOfThirdsGuidelines(canvas);
            } else if (mGuidelines == GUIDELINES_ON_TOUCH) {
                // Draw only when resizing
                if (mPressedHandle != null)
                    drawRuleOfThirdsGuidelines(canvas);
            }
        }

        float w = mBorderPaint.getStrokeWidth();
        float l = Edge.LEFT.getCoordinate() + w;
        float t = Edge.TOP.getCoordinate() + w;
        float r = Edge.RIGHT.getCoordinate() - w;
        float b = Edge.BOTTOM.getCoordinate() - w;
        if (mCropShape == CropShape.RECTANGLE) {
            // Draw rectangle crop window border.
            canvas.drawRect(l, t, r, b, mBorderPaint);
            drawCorners(canvas);
        } else {
            // Draw circular crop window border
            mRectF.set(l, t, r, b);
            canvas.drawOval(mRectF, mBorderPaint);
        }
    }

    @Override
    public boolean onTouchEvent(@SuppressWarnings("NullableProblems") MotionEvent event) {

        // If this View is not enabled, don't allow for touch interactions.
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                onActionDown(event.getX(), event.getY());
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                onActionUp();
                return true;

            case MotionEvent.ACTION_MOVE:
                onActionMove(event.getX(), event.getY());
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            default:
                return false;
        }
    }

    private void init(Context context) {

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        mHandleRadius = HandleUtil.getTargetRadius(context);

        mSnapRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                SNAP_RADIUS_DP,
                displayMetrics);

        mBorderPaint = PaintUtil.newBorderPaint(context);
        mGuidelinePaint = PaintUtil.newGuidelinePaint();
        mBackgroundPaint = PaintUtil.newBackgroundPaint(context);
        mCornerPaint = PaintUtil.newCornerPaint(context);

        // Sets the values for the corner sizes
        mCornerOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_CORNER_OFFSET_DP,
                displayMetrics);
        mCornerExtension = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_CORNER_EXTENSION_DP,
                displayMetrics);
        mCornerLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_CORNER_LENGTH_DP,
                displayMetrics);

        // Sets guidelines to default until specified otherwise
        mGuidelines = CropImageView.DEFAULT_GUIDELINES;
    }

    /**
     * Set the initial crop window size and position. This is dependent on the
     * size and position of the image being cropped.
     *
     * @param bitmapRect the bounding box around the image being cropped
     */
    private void initCropWindow(Rect bitmapRect) {

        if (bitmapRect.width() == 0 || bitmapRect.height() == 0) {
            return;
        }

        // Tells the attribute functions the crop window has already been
        // initialized
        if (!initializedCropWindow) {
            initializedCropWindow = true;
        }

        if (mFixAspectRatio
                && (bitmapRect.left != 0 || bitmapRect.right != 0
                || bitmapRect.top != 0 || bitmapRect.bottom != 0)) {

            // If the image aspect ratio is wider than the crop aspect ratio,
            // then the image height is the determining initial length. Else,
            // vice-versa.
            if (AspectRatioUtil.calculateAspectRatio(bitmapRect) > mTargetAspectRatio) {

                Edge.TOP.setCoordinate(bitmapRect.top);
                Edge.BOTTOM.setCoordinate(bitmapRect.bottom);

                final float centerX = getWidth() / 2f;

                //dirty fix for wrong crop overlay aspect ratio when using fixed aspect ratio
                mTargetAspectRatio = (float) mAspectRatioX / mAspectRatioY;

                // Limits the aspect ratio to no less than 40 wide or 40 tall
                final float cropWidth = Math.max(Edge.MIN_CROP_LENGTH_PX,
                        AspectRatioUtil.calculateWidth(Edge.TOP.getCoordinate(),
                                Edge.BOTTOM.getCoordinate(),
                                mTargetAspectRatio));

                // Create new TargetAspectRatio if the original one does not fit
                // the screen
                if (cropWidth == Edge.MIN_CROP_LENGTH_PX) {
                    mTargetAspectRatio = (Edge.MIN_CROP_LENGTH_PX) / (Edge.BOTTOM.getCoordinate() - Edge.TOP.getCoordinate());
                }

                final float halfCropWidth = cropWidth / 2f;
                Edge.LEFT.setCoordinate(centerX - halfCropWidth);
                Edge.RIGHT.setCoordinate(centerX + halfCropWidth);

            } else {

                Edge.LEFT.setCoordinate(bitmapRect.left);
                Edge.RIGHT.setCoordinate(bitmapRect.right);

                final float centerY = getHeight() / 2f;

                // Limits the aspect ratio to no less than 40 wide or 40 tall
                final float cropHeight = Math.max(Edge.MIN_CROP_LENGTH_PX,
                        AspectRatioUtil.calculateHeight(Edge.LEFT.getCoordinate(),
                                Edge.RIGHT.getCoordinate(),
                                mTargetAspectRatio));

                // Create new TargetAspectRatio if the original one does not fit
                // the screen
                if (cropHeight == Edge.MIN_CROP_LENGTH_PX) {
                    mTargetAspectRatio = (Edge.RIGHT.getCoordinate() - Edge.LEFT.getCoordinate()) / Edge.MIN_CROP_LENGTH_PX;
                }

                final float halfCropHeight = cropHeight / 2f;
                Edge.TOP.setCoordinate(centerY - halfCropHeight);
                Edge.BOTTOM.setCoordinate(centerY + halfCropHeight);
            }

        } else { // ... do not fix aspect ratio...

            // Initialize crop window to have 10% padding w/ respect to image.
            final float horizontalPadding = 0.1f * bitmapRect.width();
            final float verticalPadding = 0.1f * bitmapRect.height();

            Edge.LEFT.setCoordinate(bitmapRect.left + horizontalPadding);
            Edge.TOP.setCoordinate(bitmapRect.top + verticalPadding);
            Edge.RIGHT.setCoordinate(bitmapRect.right - horizontalPadding);
            Edge.BOTTOM.setCoordinate(bitmapRect.bottom - verticalPadding);
        }
    }

    /**
     * Indicates whether the crop window is small enough that the guidelines
     * should be shown. Public because this function is also used to determine
     * if the center handle should be focused.
     *
     * @return boolean Whether the guidelines should be shown or not
     */
    public static boolean showGuidelines() {
        if ((Math.abs(Edge.LEFT.getCoordinate() - Edge.RIGHT.getCoordinate()) < DEFAULT_SHOW_GUIDELINES_LIMIT)
                || (Math.abs(Edge.TOP.getCoordinate() - Edge.BOTTOM.getCoordinate()) < DEFAULT_SHOW_GUIDELINES_LIMIT)) {
            return false;
        } else {
            return true;
        }
    }

    private void drawRuleOfThirdsGuidelines(Canvas canvas) {

        float w = mBorderPaint.getStrokeWidth();
        float l = Edge.LEFT.getCoordinate() + w;
        float t = Edge.TOP.getCoordinate() + w;
        float r = Edge.RIGHT.getCoordinate() - w;
        float b = Edge.BOTTOM.getCoordinate() - w;

        if (mCropShape == CropShape.OVAL) {
            l += 15 * mGuidelinePaint.getStrokeWidth();
            t += 15 * mGuidelinePaint.getStrokeWidth();
            r -= 15 * mGuidelinePaint.getStrokeWidth();
            b -= 15 * mGuidelinePaint.getStrokeWidth();
        }

        // Draw vertical guidelines.
        final float oneThirdCropWidth = Edge.getWidth() / 3;

        final float x1 = l + oneThirdCropWidth;
        canvas.drawLine(x1, t, x1, b, mGuidelinePaint);
        final float x2 = r - oneThirdCropWidth;
        canvas.drawLine(x2, t, x2, b, mGuidelinePaint);

        // Draw horizontal guidelines.
        final float oneThirdCropHeight = Edge.getHeight() / 3;

        final float y1 = t + oneThirdCropHeight;
        canvas.drawLine(l, y1, r, y1, mGuidelinePaint);
        final float y2 = b - oneThirdCropHeight;
        canvas.drawLine(l, y2, r, y2, mGuidelinePaint);
    }

    private void drawBackground(Canvas canvas, Rect bitmapRect) {

        final float l = Edge.LEFT.getCoordinate();
        final float t = Edge.TOP.getCoordinate();
        final float r = Edge.RIGHT.getCoordinate();
        final float b = Edge.BOTTOM.getCoordinate();

        if (mCropShape == CropShape.RECTANGLE) {
            canvas.drawRect(bitmapRect.left, bitmapRect.top, bitmapRect.right, t, mBackgroundPaint);
            canvas.drawRect(bitmapRect.left, b, bitmapRect.right, bitmapRect.bottom, mBackgroundPaint);
            canvas.drawRect(bitmapRect.left, t, l, b, mBackgroundPaint);
            canvas.drawRect(r, t, bitmapRect.right, b, mBackgroundPaint);
        } else {
            Path circleSelectionPath = new Path();
            mRectF.set(l, t, r, b);
            circleSelectionPath.addOval(mRectF, Path.Direction.CW);
            canvas.clipPath(circleSelectionPath, Region.Op.XOR);
            canvas.drawRect(bitmapRect.left, bitmapRect.top, bitmapRect.right, bitmapRect.bottom, mBackgroundPaint);
            canvas.restore();
        }
    }

    private void drawCorners(Canvas canvas) {

        float w = mBorderPaint.getStrokeWidth();
        final float l = Edge.LEFT.getCoordinate() + w;
        final float t = Edge.TOP.getCoordinate() + w;
        final float r = Edge.RIGHT.getCoordinate() - w;
        final float b = Edge.BOTTOM.getCoordinate() - w;

        // Top left
        canvas.drawLine(l - mCornerOffset, t - mCornerExtension, l - mCornerOffset, t + mCornerLength, mCornerPaint);
        canvas.drawLine(l, t - mCornerOffset, l + mCornerLength, t - mCornerOffset, mCornerPaint);

        // Top right
        canvas.drawLine(r + mCornerOffset, t - mCornerExtension, r + mCornerOffset, t + mCornerLength, mCornerPaint);
        canvas.drawLine(r, t - mCornerOffset, r - mCornerLength, t - mCornerOffset, mCornerPaint);

        // Bottom left
        canvas.drawLine(l - mCornerOffset, b + mCornerExtension, l - mCornerOffset, b - mCornerLength, mCornerPaint);
        canvas.drawLine(l, b + mCornerOffset, l + mCornerLength, b + mCornerOffset, mCornerPaint);

        // Bottom left
        canvas.drawLine(r + mCornerOffset, b + mCornerExtension, r + mCornerOffset, b - mCornerLength, mCornerPaint);
        canvas.drawLine(r, b + mCornerOffset, r - mCornerLength, b + mCornerOffset, mCornerPaint);
    }

    /**
     * Handles a {@link android.view.MotionEvent#ACTION_DOWN} event.
     *
     * @param x the x-coordinate of the down action
     * @param y the y-coordinate of the down action
     */
    private void onActionDown(float x, float y) {

        final float left = Edge.LEFT.getCoordinate();
        final float top = Edge.TOP.getCoordinate();
        final float right = Edge.RIGHT.getCoordinate();
        final float bottom = Edge.BOTTOM.getCoordinate();

        mPressedHandle = HandleUtil.getPressedHandle(x, y, left, top, right, bottom, mHandleRadius, mCropShape);

        if (mPressedHandle == null) {
            return;
        }

        // Calculate the offset of the touch point from the precise location
        // of the handle. Save these values in a member variable since we want
        // to maintain this offset as we drag the handle.
        mTouchOffset = HandleUtil.getOffset(mPressedHandle, x, y, left, top, right, bottom);

        invalidate();
    }

    /**
     * Handles a {@link android.view.MotionEvent#ACTION_UP} or
     * {@link android.view.MotionEvent#ACTION_CANCEL} event.
     */
    private void onActionUp() {

        if (mPressedHandle == null) {
            return;
        }

        mPressedHandle = null;

        invalidate();
    }

    /**
     * Handles a {@link android.view.MotionEvent#ACTION_MOVE} event.
     *
     * @param x the x-coordinate of the move event
     * @param y the y-coordinate of the move event
     */
    private void onActionMove(float x, float y) {

        if (mPressedHandle == null) {
            return;
        }

        // Adjust the coordinates for the finger position's offset (i.e. the
        // distance from the initial touch to the precise handle location).
        // We want to maintain the initial touch's distance to the pressed
        // handle so that the crop window size does not "jump".
        x += mTouchOffset.first;
        y += mTouchOffset.second;

        // Calculate the new crop window size/position.
        if (mFixAspectRatio) {
            mPressedHandle.updateCropWindow(x, y, mTargetAspectRatio, mBitmapRect, mSnapRadius);
        } else {
            mPressedHandle.updateCropWindow(x, y, mBitmapRect, mSnapRadius);
        }
        invalidate();
    }
    //endregion
}
