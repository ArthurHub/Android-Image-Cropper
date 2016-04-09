Android Image Cropper
=======
[![build status](https://travis-ci.org/ArthurHub/Android-Image-Cropper.svg)](https://travis-ci.org/ArthurHub/Android-Image-Cropper) 
[![Codacy Badge](https://api.codacy.com/project/badge/grade/4d3781df0cce40959881a8d91365407a)](https://www.codacy.com/app/tep-arthur/Android-Image-Cropper)
[ ![Download](https://api.bintray.com/packages/arthurhub/maven/Android-Image-Cropper/images/download.svg) ](https://bintray.com/arthurhub/maven/Android-Image-Cropper/_latestVersion)

Image cropping tool, displays a resizable, rectengular/oval crop window on top of image.

[Optimized for cropping image picked from Camera or Gallery](http://theartofdev.com/2015/02/15/android-cropping-image-from-camera-or-gallery/)

![Crop](https://github.com/ArthurHub/Android-Image-Cropper/blob/master/crop.jpg?raw=true)

## Usage
*For a working implementation, please have a look at the Sample Project*

1. Include the library

 ```
 compile 'com.theartofdev.edmodo:android-image-cropper:1.2.+'
 ```

2. Add `CropImageView` into your activity

 ```xml
 <!-- Image Cropper fill the remaining available height -->
 <com.theartofdev.edmodo.cropper.CropImageView
    xmlns:custom="http://schemas.android.com/apk/res-auto"
    android:id="@+id/cropImageView"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1"
    custom:cropScaleType="fitCenter"/>
 ```

3. Set image to crop

 ```java
 cropImageView.setImageBitmap(bitmap);
 // or
 cropImageView.setImageUriAsync(uri);
 ```

4. Get cropped image

 ```java
 Bitmap cropped = cropImageView.getCroppedImage();
 // or (must subscribe to async event using cropImageView.setOnGetCroppedImageCompleteListener(listener))
 cropImageView.getCroppedImageAsync(CropImageView.CropShape.RECTANGLE, 400, 400);
 ```

## Features
- Set cropping image as Bitmap, Resource or Android URI (Gallery, Camera, Dropbox, etc.).
- Set cropping window shape to Rectengular or Oval (cube/circle by fixing aspect ration).
- Rotate image to allow the user to rotate the image during cropping.
- Auto rotate bitmap by provided Exif data or loading from Android URI.
- Set image Scale type in the cropping image view: center or fit.
- Set result image min/max limits in pixels.
- Get cropping rectangle or the cropped bitmap.
- Using sampling to reduce memory usage and prevent out-of-memory.
- Support required size and sampling on getting cropped image for memory optimization.
- Supported on API Level 10 and above.
 
## Customizations
- Cropping window aspect ratio: Free, 1:1, 4:3, 16:9 or Custom.
- Guidelines appearance: Off / Always On / Show on Toch.
- Cropping window Border line, border corner and guidelines thickness and color.
- Cropping background color.

For more information, see the [linked Github Wiki page](https://github.com/ArthurHub/Android-Image-Cropper/wiki). 

## Posts
 - [Android cropping image from camera or gallery](http://theartofdev.com/2015/02/15/android-cropping-image-from-camera-or-gallery/)
 - [Android Image Cropper async support and custom progress UI](http://theartofdev.com/2016/01/15/android-image-cropper-async-support-and-custom-progress-ui/)

## Change log
*1.2.6*
 * Fix off-by-1 error in cropping rectangle, double verify width == height for 1:1 fixed aspect ratio.

*1.2.4*
 * Added fallback in crop to use `BitmapFactory` when `BitmapRegionDecoder` fails

*1.2.3*
 * Fix `getActualCropRect` to adjust by sampling size for images loaded from URI.
 * Fix crop window size bounded with fixed aspect ratio and move of a single edge.
 * Added `CropImageHelper` class to simplify cropping image work.

*1.2.2 (beta)*
 * Fix `setShowCropOverlay(boolean)` not working properly.
 * Fix crop window bounds issue when cropping image is too small relative to min/max bounds with fixed aspect ratio.
 * Fix crop window reset on on-screen keyboard show/hide.

See [full change log](https://github.com/ArthurHub/Android-Image-Cropper/wiki/Change-Log).

## License
Forked from [edmodo/cropper](https://github.com/edmodo/cropper) fixing some bugs and adding some features.

Copyright 2013, Edmodo, Inc. 

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the   License.
You may obtain a copy of the License in the LICENSE file, or at:

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS   IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
