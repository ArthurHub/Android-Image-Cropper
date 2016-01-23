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
import android.util.DisplayMetrics;
import android.util.TypedValue;
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
    private float mHandleRadius;

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
     * Whether the Crop View has been initialized for the first time
     */
    private boolean initializedCropWindow = false;

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
     * Sets the guidelines for the CropOverlayView to be either on, off, or to
     * show when resizing the application.
     *
     * @param guidelines Integer that signals whether the guidelines should be
     * on, off, or only showing when resizing.
     */
    public void setGuidelines(CropImageView.Guidelines guidelines) {
        mGuidelines = guidelines;
        if (initializedCropWindow) {
            initCropWindow(mBitmapRect);
            invalidate();
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
     * An edge of the crop window will snap to the corresponding edge of a
     * specified bounding box when the crop window edge is less than or equal to
     * this distance (in pixels) away from the bounding box edge. (default: 3)
     */
    public void setSnapRadius(float snapRadius) {
        mSnapRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, snapRadius, getResources().getDisplayMetrics());
    }

    /**
     * Sets all initial values, but does not call initCropWindow to reset the
     * views. Used once at the very start to initialize the attributes.
     *
     * @param cropShape
     * @param snapRadius
     * @param guidelines Integer that signals whether the guidelines should be
     * on, off, or only showing when resizing.
     * @param fixAspectRatio Boolean that signals whether the aspect ratio
     * should be maintained.
     * @param aspectRatioX float that specifies the new X value of the aspect
     * ratio
     * @param aspectRatioY float that specifies the new Y value of the aspect
     * @param guidelinesThickness
     * @param guidelinesColor
     */
    public void setInitialAttributeValues(CropImageView.CropShape cropShape,
                                          float snapRadius,
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
                                          int backgroundColor) {

        DisplayMetrics dm = getResources().getDisplayMetrics();

        setCropShape(cropShape);

        setSnapRadius(snapRadius);

        setGuidelines(guidelines);

        setFixedAspectRatio(fixAspectRatio);

        setAspectRatioX(aspectRatioX);

        setAspectRatioY(aspectRatioY);

        if (initialCropWindowPaddingRatio < 0 || initialCropWindowPaddingRatio >= 0.5) {
            throw new IllegalArgumentException("Cannot set initial crop window padding value to a number less < 0 or >= 0.5");
        }
        mInitialCropWindowPaddingRatio = initialCropWindowPaddingRatio;

        if (borderLineThickness < 0) {
            throw new IllegalArgumentException("Cannot set line thickness value to a number less than 0.");
        }
        mBorderPaint = getNewPaintOrNull(dm, borderLineThickness, borderLineColor);

        if (borderCornerThickness < 0) {
            throw new IllegalArgumentException("Cannot set corner thickness value to a number less than 0.");
        }
        mBorderCornerOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, borderCornerOffset, dm);
        mBorderCornerLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, borderCornerLength, dm);
        mBorderCornerPaint = getNewPaintOrNull(dm, borderCornerThickness, borderCornerColor);

        if (guidelinesThickness < 0) {
            throw new IllegalArgumentException("Cannot set guidelines thickness value to a number less than 0.");
        }
        mGuidelinePaint = getNewPaintOrNull(dm, guidelinesThickness, guidelinesColor);

        mBackgroundPaint = getNewPaint(backgroundColor);

        mHandleRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CropDefaults.TARGET_RADIUS, dm);
    }

    //region: Private methods

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

        RectF rect = new RectF();

        // Tells the attribute functions the crop window has already been initialized
        initializedCropWindow = true;

        float horizontalPadding = mInitialCropWindowPaddingRatio * bitmapRect.width();
        float verticalPadding = mInitialCropWindowPaddingRatio * bitmapRect.height();

        if (mFixAspectRatio && (bitmapRect.left != 0 || bitmapRect.right != 0 || bitmapRect.top != 0 || bitmapRect.bottom != 0)) {

            // If the image aspect ratio is wider than the crop aspect ratio,
            // then the image height is the determining initial length. Else, vice-versa.
            float bitmapAspectRatio = (float) bitmapRect.width() / (float) bitmapRect.height();
            if (bitmapAspectRatio > mTargetAspectRatio) {

                rect.top = bitmapRect.top + verticalPadding;
                rect.bottom = bitmapRect.bottom - verticalPadding;

                float centerX = getWidth() / 2f;

                //dirty fix for wrong crop overlay aspect ratio when using fixed aspect ratio
                mTargetAspectRatio = (float) mAspectRatioX / mAspectRatioY;

                // Limits the aspect ratio to no less than 40 wide or 40 tall
                float cropWidth = Math.max(40, rect.height() * mTargetAspectRatio);

                // Create new TargetAspectRatio if the original one does not fit the screen
                if (cropWidth == 40) {
                    mTargetAspectRatio = 40 / rect.height();
                }

                float halfCropWidth = cropWidth / 2f;
                rect.left = centerX - halfCropWidth;
                rect.right = centerX + halfCropWidth;

            } else {

                rect.left = bitmapRect.left + horizontalPadding;
                rect.right = bitmapRect.right - horizontalPadding;

                float centerY = getHeight() / 2f;

                // Limits the aspect ratio to no less than 40 wide or 40 tall
                float cropHeight = Math.max(40, rect.width() / mTargetAspectRatio);

                // Create new TargetAspectRatio if the original one does not fit the screen
                if (cropHeight == 40) {
                    mTargetAspectRatio = rect.width() / 40;
                }

                float halfCropHeight = cropHeight / 2f;
                rect.top = centerY - halfCropHeight;
                rect.bottom = centerY + halfCropHeight;
            }
        } else {
            // ... do not fix aspect ratio...

            // Initialize crop window to have 10% padding w/ respect to image.
            rect.left = bitmapRect.left + horizontalPadding;
            rect.top = bitmapRect.top + verticalPadding;
            rect.right = bitmapRect.right - horizontalPadding;
            rect.bottom = bitmapRect.bottom - verticalPadding;
        }

        mCropWindowHandler.setRect(rect);
    }

    /**
     * Initialize the crop window here because we need the size of the view to have been determined.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        initCropWindow(mBitmapRect);
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
            } else if (mGuidelines == CropImageView.Guidelines.ON_TOUCH) {
                // Draw only when resizing
                if (mMoveHandler != null) {
                    drawGuidelines(canvas);
                }
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
    private static Paint getNewPaintOrNull(DisplayMetrics displayMetrics, float thickness, int color) {
        if (thickness > 0) {
            Paint borderPaint = new Paint();
            borderPaint.setColor(color);
            borderPaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, thickness, displayMetrics));
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
        mMoveHandler = mCropWindowHandler.getMoveHandler(x, y, mHandleRadius, mCropShape);
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