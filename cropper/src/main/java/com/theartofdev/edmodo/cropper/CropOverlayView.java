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

package com.theartofdev.edmodo.cropper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * A custom View representing the crop window and the shaded background outside the crop window.
 */
public class CropOverlayView extends View {

    //region: Fields and Consts

    /**
     * Handler from crop window stuff, moving and knowing possition.
     */
    private final CropWindowHandler mCropWindowHandler = new CropWindowHandler();

    /**
     * The Paint used to draw the white rectangle around the crop area.
     */
    private Paint mBorderPaint;

    /**
     * The Paint used to draw the corners of the Border
     */
    private Paint mBorderCornerPaint;

    /**
     * The Paint used to draw the guidelines within the crop area when pressed.
     */
    private Paint mGuidelinePaint;

    /**
     * The Paint used to darken the surrounding areas outside the crop area.
     */
    private Paint mBackgroundPaint;

    /**
     * The bounding box around the Bitmap that we are cropping.
     */
    private Rect mBitmapRect;

    /**
     * The offset to draw the border corener from the border
     */
    private float mBorderCornerOffset;

    /**
     * the length of the border corner to draw
     */
    private float mBorderCornerLength;

    /**
     * The initial crop window padding from image borders
     */
    private float mInitialCropWindowPaddingRatio;

    /**
     * The radius of the touch zone (in pixels) around a given Handle.
     */
    private float mTouchRadius;

    /**
     * An edge of the crop window will snap to the corresponding edge of a specified bounding box
     * when the crop window edge is less than or equal to this distance (in pixels) away from the bounding box edge.
     */
    private float mSnapRadius;

    /**
     * The Handle that is currently pressed; null if no Handle is pressed.
     */
    private CropWindowMoveHandler mMoveHandler;

    /**
     * Flag indicating if the crop area should always be a certain aspect ratio (indicated by mTargetAspectRatio).
     */
    private boolean mFixAspectRatio = CropDefaults.DEFAULT_FIXED_ASPECT_RATIO;

    /**
     * Floats to save the current aspect ratio of the image
     */
    private int mAspectRatioX = CropDefaults.DEFAULT_ASPECT_RATIO_X;

    private int mAspectRatioY = CropDefaults.DEFAULT_ASPECT_RATIO_Y;

    /**
     * The aspect ratio that the crop area should maintain;
     * this variable is only used when mMaintainAspectRatio is true.
     */
    private float mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;

    /**
     * Instance variables for customizable attributes
     */
    private CropImageView.Guidelines mGuidelines;

    /**
     * The shape of the cropping area - rectangle/circular.
     */
    private CropImageView.CropShape mCropShape;

    /**
     * the initial crop window rectangle to set
     */
    private Rect mInitialCropWindowRect;

    /**
     * Whether the Crop View has been initialized for the first time
     */
    private boolean initializedCropWindow;

    /**
     * Used to set back LayerType after changing to software.
     */
    private Integer mOriginalLayerType;
    //endregion

    public CropOverlayView(Context context) {
        this(context, null);
    }

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Get the left/top/right/bottom coordinates of the crop window.
     */
    public RectF getCropWindowRect() {
        return mCropWindowHandler.getRect();
    }

    /**
     * Informs the CropOverlayView of the image's position relative to the
     * ImageView. This is necessary to call in order to draw the crop window.
     *
     * @param bitmapRect the image's bounding box
     */
    public void setBitmapRect(Rect bitmapRect) {
        if (mBitmapRect == null || !bitmapRect.equals(mBitmapRect)) {
            mBitmapRect = bitmapRect;
            initCropWindow();
        }
    }

    /**
     * Resets the crop overlay view.
     */
    public void resetCropOverlayView() {
        if (initializedCropWindow) {
            initCropWindow();
            invalidate();
        }
    }

    /**
     * The shape of the cropping area - rectangle/circular.
     */
    public CropImageView.CropShape getCropShape() {
        return mCropShape;
    }

    /**
     * The shape of the cropping area - rectangle/circular.
     */
    public void setCropShape(CropImageView.CropShape cropShape) {
        if (mCropShape != cropShape) {
            mCropShape = cropShape;
            if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT <= 17) {
                if (mCropShape == CropImageView.CropShape.OVAL) {
                    mOriginalLayerType = getLayerType();
                    if (mOriginalLayerType != View.LAYER_TYPE_SOFTWARE) {
                        // TURN off hardware acceleration
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    } else {
                        mOriginalLayerType = null;
                    }
                } else if (mOriginalLayerType != null) {
                    // return hardware acceleration back
                    setLayerType(mOriginalLayerType, null);
                    mOriginalLayerType = null;
                }
            }
            invalidate();
        }
    }

    /**
     * Get the current guidelines option set.
     */
    public CropImageView.Guidelines getGuidelines() {
        return mGuidelines;
    }

    /**
     * Sets the guidelines for the CropOverlayView to be either on, off, or to show when resizing the application.
     */
    public void setGuidelines(CropImageView.Guidelines guidelines) {
        if (mGuidelines != guidelines) {
            mGuidelines = guidelines;
            if (initializedCropWindow) {
                invalidate();
            }
        }
    }

    /**
     * whether the aspect ratio is fixed or not; true fixes the aspect ratio, while false allows it to be changed.
     */
    public boolean isFixAspectRatio() {
        return mFixAspectRatio;
    }

    /**
     * Sets whether the aspect ratio is fixed or not; true fixes the aspect ratio, while false allows it to be changed.
     */
    public void setFixedAspectRatio(boolean fixAspectRatio) {
        if (mFixAspectRatio != fixAspectRatio) {
            mFixAspectRatio = fixAspectRatio;
            if (initializedCropWindow) {
                initCropWindow();
                invalidate();
            }
        }
    }

    /**
     * the X value of the aspect ratio;
     */
    public int getAspectRatioX() {
        return mAspectRatioX;
    }

    /**
     * Sets the X value of the aspect ratio; is defaulted to 1.
     */
    public void setAspectRatioX(int aspectRatioX) {
        if (aspectRatioX <= 0) {
            throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0.");
        } else if (mAspectRatioX != aspectRatioX) {
            mAspectRatioX = aspectRatioX;
            mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;

            if (initializedCropWindow) {
                initCropWindow();
                invalidate();
            }
        }
    }

    /**
     * the Y value of the aspect ratio;
     */
    public int getAspectRatioY() {
        return mAspectRatioY;
    }

    /**
     * Sets the Y value of the aspect ratio; is defaulted to 1.
     *
     * @param aspectRatioY int that specifies the new Y value of the aspect
     * ratio
     */
    public void setAspectRatioY(int aspectRatioY) {
        if (aspectRatioY <= 0) {
            throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0.");
        } else if (mAspectRatioY != aspectRatioY) {
            mAspectRatioY = aspectRatioY;
            mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;

            if (initializedCropWindow) {
                initCropWindow();
                invalidate();
            }
        }
    }

    /**
     * An edge of the crop window will snap to the corresponding edge of a
     * specified bounding box when the crop window edge is less than or equal to
     * this distance (in pixels) away from the bounding box edge. (default: 3)
     */
    public void setSnapRadius(float snapRadius) {
        mSnapRadius = snapRadius;
    }

    /**
     * set the scale factor of the showen image to original image to scale the limits appropriately.
     */
    public void setScaleFactor(float scaleFactorWidth, float scaleFactorHeight) {
        mCropWindowHandler.setScaleFactor(scaleFactorWidth, scaleFactorHeight);
    }

    /**
     * Get crop window initial rectangle.
     */
    public Rect getInitialCropWindowRect() {
        return mInitialCropWindowRect;
    }

    /**
     * Set crop window initial rectangle to be used instead of default.
     */
    public void setInitialCropWindowRect(Rect rect) {
        mInitialCropWindowRect = rect;
        if (initializedCropWindow) {
            initCropWindow();
            invalidate();
        }
    }

    /**
     * Sets all initial values, but does not call initCropWindow to reset the
     * views. Used once at the very start to initialize the attributes.
     *
     * @param cropShape
     * @param snapRadius
     * @param guidelines Integer that signals whether the guidelines should be on, off, or only showing when resizing.
     * @param fixAspectRatio Boolean that signals whether the aspect ratio should be maintained.
     * @param aspectRatioX float that specifies the new X value of the aspect ratio
     * @param aspectRatioY float that specifies the new Y value of the aspect
     * @param guidelinesThickness
     * @param guidelinesColor
     * @param minCropResultWidth
     * @param minCropResultHeight
     * @param maxCropResultWidth
     * @param maxCropResultHeight
     */
    public void setInitialAttributeValues(CropImageView.CropShape cropShape,
                                          float snapRadius,
                                          float touchRadius,
                                          CropImageView.Guidelines guidelines,
                                          boolean fixAspectRatio,
                                          int aspectRatioX,
                                          int aspectRatioY,
                                          float initialCropWindowPaddingRatio,
                                          float borderLineThickness,
                                          int borderLineColor,
                                          float borderCornerThickness,
                                          float borderCornerOffset,
                                          float borderCornerLength,
                                          int borderCornerColor,
                                          float guidelinesThickness,
                                          int guidelinesColor,
                                          int backgroundColor,
                                          float minCropWindowWidth,
                                          float minCropWindowHeight,
                                          float minCropResultWidth,
                                          float minCropResultHeight,
                                          float maxCropResultWidth,
                                          float maxCropResultHeight) {

        mCropWindowHandler.setInitialAttributeValues(minCropWindowWidth, minCropWindowHeight,
                minCropResultWidth, minCropResultHeight,
                maxCropResultWidth, maxCropResultHeight);

        setCropShape(cropShape);

        setSnapRadius(snapRadius);

        setGuidelines(guidelines);

        setFixedAspectRatio(fixAspectRatio);

        setAspectRatioX(aspectRatioX);

        setAspectRatioY(aspectRatioY);

        if (touchRadius < 0) {
            throw new IllegalArgumentException("Cannot set touch radius value to a number <= 0 ");
        }
        mTouchRadius = touchRadius;

        if (initialCropWindowPaddingRatio < 0 || initialCropWindowPaddingRatio >= 0.5) {
            throw new IllegalArgumentException("Cannot set initial crop window padding value to a number < 0 or >= 0.5");
        }
        mInitialCropWindowPaddingRatio = initialCropWindowPaddingRatio;

        if (borderLineThickness < 0) {
            throw new IllegalArgumentException("Cannot set line thickness value to a number less than 0.");
        }
        mBorderPaint = getNewPaintOrNull(borderLineThickness, borderLineColor);

        if (borderCornerThickness < 0) {
            throw new IllegalArgumentException("Cannot set corner thickness value to a number less than 0.");
        }
        mBorderCornerOffset = borderCornerOffset;
        mBorderCornerLength = borderCornerLength;
        mBorderCornerPaint = getNewPaintOrNull(borderCornerThickness, borderCornerColor);

        if (guidelinesThickness < 0) {
            throw new IllegalArgumentException("Cannot set guidelines thickness value to a number less than 0.");
        }
        mGuidelinePaint = getNewPaintOrNull(guidelinesThickness, guidelinesColor);

        mBackgroundPaint = getNewPaint(backgroundColor);
    }

    //region: Private methods

    /**
     * Set the initial crop window size and position. This is dependent on the
     * size and position of the image being cropped.
     *
     * @param mBitmapRect the bounding box around the image being cropped
     */
    private void initCropWindow() {

        if (mBitmapRect.width() == 0 || mBitmapRect.height() == 0) {
            return;
        }

        RectF rect = new RectF();

        // Tells the attribute functions the crop window has already been initialized
        initializedCropWindow = true;

        float horizontalPadding = mInitialCropWindowPaddingRatio * mBitmapRect.width();
        float verticalPadding = mInitialCropWindowPaddingRatio * mBitmapRect.height();

        if (mInitialCropWindowRect != null && mInitialCropWindowRect.width() > 0 && mInitialCropWindowRect.height() > 0) {
            // Get crop window position relative to the displayed image.
            rect.left = mBitmapRect.left + mInitialCropWindowRect.left / mCropWindowHandler.getScaleFactorWidth();
            rect.top = mBitmapRect.top + mInitialCropWindowRect.top / mCropWindowHandler.getScaleFactorHeight();
            rect.right = rect.left + mInitialCropWindowRect.width() / mCropWindowHandler.getScaleFactorWidth();
            rect.bottom = rect.top + mInitialCropWindowRect.height() / mCropWindowHandler.getScaleFactorHeight();

            // Correct for floating point errors. Crop rect boundaries should not exceed the source Bitmap bounds.
            rect.left = Math.max(mBitmapRect.left, rect.left);
            rect.top = Math.max(mBitmapRect.top, rect.top);
            rect.right = Math.min(mBitmapRect.right, rect.right);
            rect.bottom = Math.min(mBitmapRect.bottom, rect.bottom);

        } else if (mFixAspectRatio && !mBitmapRect.isEmpty()) {

            // If the image aspect ratio is wider than the crop aspect ratio,
            // then the image height is the determining initial length. Else, vice-versa.
            float bitmapAspectRatio = (float) mBitmapRect.width() / (float) mBitmapRect.height();
            if (bitmapAspectRatio > mTargetAspectRatio) {

                rect.top = mBitmapRect.top + verticalPadding;
                rect.bottom = mBitmapRect.bottom - verticalPadding;

                float centerX = getWidth() / 2f;

                //dirty fix for wrong crop overlay aspect ratio when using fixed aspect ratio
                mTargetAspectRatio = (float) mAspectRatioX / mAspectRatioY;

                // Limits the aspect ratio to no less than 40 wide or 40 tall
                float cropWidth = Math.max(mCropWindowHandler.getMinCropWidth(), rect.height() * mTargetAspectRatio);

                float halfCropWidth = cropWidth / 2f;
                rect.left = centerX - halfCropWidth;
                rect.right = centerX + halfCropWidth;

            } else {

                rect.left = mBitmapRect.left + horizontalPadding;
                rect.right = mBitmapRect.right - horizontalPadding;

                float centerY = getHeight() / 2f;

                // Limits the aspect ratio to no less than 40 wide or 40 tall
                float cropHeight = Math.max(mCropWindowHandler.getMinCropHeight(), rect.width() / mTargetAspectRatio);

                float halfCropHeight = cropHeight / 2f;
                rect.top = centerY - halfCropHeight;
                rect.bottom = centerY + halfCropHeight;
            }
        } else {
            // Initialize crop window to have 10% padding w/ respect to image.
            rect.left = mBitmapRect.left + horizontalPadding;
            rect.top = mBitmapRect.top + verticalPadding;
            rect.right = mBitmapRect.right - horizontalPadding;
            rect.bottom = mBitmapRect.bottom - verticalPadding;
        }

        fixCropWindowRectByRules(rect);

        mCropWindowHandler.setRect(rect);
    }

    /**
     * Fix the given rect to fit into bitmap rect and follow min, max and aspect ratio rules.
     */
    private void fixCropWindowRectByRules(RectF rect) {
        if (rect.width() < mCropWindowHandler.getMinCropWidth()) {
            float adj = (mCropWindowHandler.getMinCropWidth() - rect.width()) / 2;
            rect.left -= adj;
            rect.right += adj;
        }
        if (rect.height() < mCropWindowHandler.getMinCropHeight()) {
            float adj = (mCropWindowHandler.getMinCropHeight() - rect.height()) / 2;
            rect.top -= adj;
            rect.bottom += adj;
        }
        if (rect.width() > mCropWindowHandler.getMaxCropWidth()) {
            float adj = (rect.width() - mCropWindowHandler.getMaxCropWidth()) / 2;
            rect.left += adj;
            rect.right -= adj;
        }
        if (rect.height() > mCropWindowHandler.getMaxCropHeight()) {
            float adj = (rect.height() - mCropWindowHandler.getMaxCropHeight()) / 2;
            rect.top += adj;
            rect.bottom -= adj;
        }
        if (mBitmapRect != null && mBitmapRect.width() > 0 && mBitmapRect.height() > 0) {
            if (rect.left < mBitmapRect.left) {
                rect.left = mBitmapRect.left;
            }
            if (rect.top < mBitmapRect.top) {
                rect.top = mBitmapRect.top;
            }
            if (rect.right > mBitmapRect.right) {
                rect.right = mBitmapRect.right;
            }
            if (rect.bottom > mBitmapRect.bottom) {
                rect.bottom = mBitmapRect.bottom;
            }
        }
        if (mFixAspectRatio && Math.abs(rect.width() - rect.height() * mTargetAspectRatio) > 0.1) {
            if (rect.width() > rect.height() * mTargetAspectRatio) {
                float adj = Math.abs(rect.height() * mTargetAspectRatio - rect.width()) / 2;
                rect.left += adj;
                rect.right -= adj;
            } else {
                float adj = Math.abs(rect.width() / mTargetAspectRatio - rect.height()) / 2;
                rect.top += adj;
                rect.bottom -= adj;
            }
        }
    }

    /**
     * Initialize the crop window here because we need the size of the view to have been determined.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        initCropWindow();
    }

    /**
     * Draw crop overview by drawing background over image not in the cripping area, then borders and guidelines.
     */
    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        // Draw translucent background for the cropped area.
        drawBackground(canvas, mBitmapRect);

        if (mCropWindowHandler.showGuidelines()) {
            // Determines whether guidelines should be drawn or not
            if (mGuidelines == CropImageView.Guidelines.ON) {
                drawGuidelines(canvas);
            } else if (mGuidelines == CropImageView.Guidelines.ON_TOUCH && mMoveHandler != null) {
                // Draw only when resizing
                drawGuidelines(canvas);
            }
        }

        drawBorders(canvas);

        if (mCropShape == CropImageView.CropShape.RECTANGLE) {
            drawCorners(canvas);
        }
    }

    /**
     * Draw shadow background over the image not including the crop area.
     */
    private void drawBackground(Canvas canvas, Rect bitmapRect) {

        RectF rect = mCropWindowHandler.getRect();

        if (mCropShape == CropImageView.CropShape.RECTANGLE) {
            canvas.drawRect(bitmapRect.left, bitmapRect.top, bitmapRect.right, rect.top, mBackgroundPaint);
            canvas.drawRect(bitmapRect.left, rect.bottom, bitmapRect.right, bitmapRect.bottom, mBackgroundPaint);
            canvas.drawRect(bitmapRect.left, rect.top, rect.left, rect.bottom, mBackgroundPaint);
            canvas.drawRect(rect.right, rect.top, bitmapRect.right, rect.bottom, mBackgroundPaint);
        } else {
            Path circleSelectionPath = new Path();
            if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT <= 17 && mCropShape == CropImageView.CropShape.OVAL) {
                CropDefaults.EMPTY_RECT_F.set(rect.left + 2, rect.top + 2, rect.right - 2, rect.bottom - 2);
            } else {
                CropDefaults.EMPTY_RECT_F.set(rect.left, rect.top, rect.right, rect.bottom);
            }
            circleSelectionPath.addOval(CropDefaults.EMPTY_RECT_F, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(circleSelectionPath, Region.Op.XOR);
            canvas.drawRect(bitmapRect.left, bitmapRect.top, bitmapRect.right, bitmapRect.bottom, mBackgroundPaint);
            canvas.restore();
        }
    }

    /**
     * Draw 2 veritcal and 2 horizontal guidelines inside the cropping area to split it into 9 equal parts.
     */
    private void drawGuidelines(Canvas canvas) {
        if (mGuidelinePaint != null) {
            float sw = mBorderPaint != null ? mBorderPaint.getStrokeWidth() : 0;
            RectF rect = mCropWindowHandler.getRect();
            rect.inset(sw, sw);

            float oneThirdCropWidth = rect.width() / 3;
            float oneThirdCropHeight = rect.height() / 3;

            if (mCropShape == CropImageView.CropShape.OVAL) {

                float w = rect.width() / 2 - sw;
                float h = rect.height() / 2 - sw;

                // Draw vertical guidelines.
                float x1 = rect.left + oneThirdCropWidth;
                float x2 = rect.right - oneThirdCropWidth;
                float yv = (float) (h * Math.sin(Math.acos((w - oneThirdCropWidth) / w)));
                canvas.drawLine(x1, rect.top + h - yv, x1, rect.bottom - h + yv, mGuidelinePaint);
                canvas.drawLine(x2, rect.top + h - yv, x2, rect.bottom - h + yv, mGuidelinePaint);

                // Draw horizontal guidelines.
                float y1 = rect.top + oneThirdCropHeight;
                float y2 = rect.bottom - oneThirdCropHeight;
                float xv = (float) (w * Math.cos(Math.asin((h - oneThirdCropHeight) / h)));
                canvas.drawLine(rect.left + w - xv, y1, rect.right - w + xv, y1, mGuidelinePaint);
                canvas.drawLine(rect.left + w - xv, y2, rect.right - w + xv, y2, mGuidelinePaint);
            } else {

                // Draw vertical guidelines.
                float x1 = rect.left + oneThirdCropWidth;
                float x2 = rect.right - oneThirdCropWidth;
                canvas.drawLine(x1, rect.top, x1, rect.bottom, mGuidelinePaint);
                canvas.drawLine(x2, rect.top, x2, rect.bottom, mGuidelinePaint);

                // Draw horizontal guidelines.
                float y1 = rect.top + oneThirdCropHeight;
                float y2 = rect.bottom - oneThirdCropHeight;
                canvas.drawLine(rect.left, y1, rect.right, y1, mGuidelinePaint);
                canvas.drawLine(rect.left, y2, rect.right, y2, mGuidelinePaint);
            }
        }
    }

    /**
     * Draw borders of the crop area.
     */
    private void drawBorders(Canvas canvas) {
        if (mBorderPaint != null) {
            float w = mBorderPaint.getStrokeWidth();
            RectF rect = mCropWindowHandler.getRect();
            rect.inset(w / 2, w / 2);

            if (mCropShape == CropImageView.CropShape.RECTANGLE) {
                // Draw rectangle crop window border.
                canvas.drawRect(rect, mBorderPaint);
            } else {
                // Draw circular crop window border
                canvas.drawOval(rect, mBorderPaint);
            }
        }
    }

    /**
     * Draw the corner of crop overlay.
     */
    private void drawCorners(Canvas canvas) {
        if (mBorderCornerPaint != null) {

            float lineWidth = mBorderPaint != null ? mBorderPaint.getStrokeWidth() : 0;
            float cornerWidth = mBorderCornerPaint.getStrokeWidth();
            float w = cornerWidth / 2 + mBorderCornerOffset;
            RectF rect = mCropWindowHandler.getRect();
            rect.inset(w, w);

            float cornerOffset = (cornerWidth - lineWidth) / 2;
            float cornerExtension = cornerWidth / 2 + cornerOffset;

            // Top left
            canvas.drawLine(rect.left - cornerOffset, rect.top - cornerExtension, rect.left - cornerOffset, rect.top + mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(rect.left - cornerExtension, rect.top - cornerOffset, rect.left + mBorderCornerLength, rect.top - cornerOffset, mBorderCornerPaint);

            // Top right
            canvas.drawLine(rect.right + cornerOffset, rect.top - cornerExtension, rect.right + cornerOffset, rect.top + mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(rect.right + cornerExtension, rect.top - cornerOffset, rect.right - mBorderCornerLength, rect.top - cornerOffset, mBorderCornerPaint);

            // Bottom left
            canvas.drawLine(rect.left - cornerOffset, rect.bottom + cornerExtension, rect.left - cornerOffset, rect.bottom - mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(rect.left - cornerExtension, rect.bottom + cornerOffset, rect.left + mBorderCornerLength, rect.bottom + cornerOffset, mBorderCornerPaint);

            // Bottom left
            canvas.drawLine(rect.right + cornerOffset, rect.bottom + cornerExtension, rect.right + cornerOffset, rect.bottom - mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(rect.right + cornerExtension, rect.bottom + cornerOffset, rect.right - mBorderCornerLength, rect.bottom + cornerOffset, mBorderCornerPaint);
        }
    }

    /**
     * Creates the Paint object for drawing.
     */
    private static Paint getNewPaint(int color) {
        Paint paint = new Paint();
        paint.setColor(color);
        return paint;
    }

    /**
     * Creates the Paint object for given thickness and color, if thickness < 0 return null.
     */
    private static Paint getNewPaintOrNull(float thickness, int color) {
        if (thickness > 0) {
            Paint borderPaint = new Paint();
            borderPaint.setColor(color);
            borderPaint.setStrokeWidth(thickness);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setAntiAlias(true);
            return borderPaint;
        } else {
            return null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // If this View is not enabled, don't allow for touch interactions.
        if (isEnabled()) {
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
        } else {
            return false;
        }
    }

    /**
     * On press down start crop window movment depending on the location of the press.<br>
     * if press is far from crop window then no move handler is returned (null).
     */
    private void onActionDown(float x, float y) {
        mMoveHandler = mCropWindowHandler.getMoveHandler(x, y, mTouchRadius, mCropShape);
        if (mMoveHandler != null) {
            invalidate();
        }
    }

    /**
     * Clear move handler starting in {@link #onActionDown(float, float)} if exists.
     */
    private void onActionUp() {
        if (mMoveHandler != null) {
            mMoveHandler = null;
            invalidate();
        }
    }

    /**
     * Handle move of crop window using the move handler created in {@link #onActionDown(float, float)}.<br>
     * The move handler will do the proper move/resize of the crop window.
     */
    private void onActionMove(float x, float y) {
        if (mMoveHandler != null) {
            mMoveHandler.move(x, y, mBitmapRect, mSnapRadius, mFixAspectRatio, mTargetAspectRatio);
            invalidate();
        }
    }
    //endregion
}