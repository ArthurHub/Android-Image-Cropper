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

import android.graphics.Rect;

/**
 * Enum representing a pressable, draggable Handle on the crop window.
 */
enum Handle {

    TOP_LEFT(new CropWindowMoveHandler(Edge.TOP, Edge.LEFT)),
    TOP_RIGHT(new CropWindowMoveHandler(Edge.TOP, Edge.RIGHT)),
    BOTTOM_LEFT(new CropWindowMoveHandler(Edge.BOTTOM, Edge.LEFT)),
    BOTTOM_RIGHT(new CropWindowMoveHandler(Edge.BOTTOM, Edge.RIGHT)),
    LEFT(new CropWindowMoveHandler(null, Edge.LEFT)),
    TOP(new CropWindowMoveHandler(Edge.TOP, null)),
    RIGHT(new CropWindowMoveHandler(null, Edge.RIGHT)),
    BOTTOM(new CropWindowMoveHandler(Edge.BOTTOM, null)),
    CENTER(new CropWindowMoveHandler(null, null));

    private CropWindowMoveHandler mHelper;

    Handle(CropWindowMoveHandler helper) {
        mHelper = helper;
    }

    public void updateCropWindow(float x, float y, Rect imageRect, float snapRadius, boolean fixAspectRatio, float targetAspectRatio) {
        mHelper.move(x, y, imageRect, snapRadius, fixAspectRatio, targetAspectRatio);
    }
}
