Android Image Cropper
=======
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Android--Image--Cropper-green.svg?style=true)](https://android-arsenal.com/details/1/3487)
[ ![Download](https://api.bintray.com/packages/arthurhub/maven/Android-Image-Cropper/images/download.svg) ](https://bintray.com/arthurhub/maven/Android-Image-Cropper/_latestVersion)


**Powerful** (Zoom, Rotation, Multi-Source), **customizable** (Shape, Limits, Style), **optimized** (Async, Sampling, Matrix) and **simple** image cropping library for Android.

![Crop](https://github.com/ArthurHub/Android-Image-Cropper/blob/master/art/demo.gif?raw=true)

## Usage
*For a working implementation, please have a look at the Sample Project*

[See GitHub Wiki for more info.](https://github.com/ArthurHub/Android-Image-Cropper/wiki)

Include the library

 ```
 compile 'com.theartofdev.edmodo:android-image-cropper:2.4.+'
 ```

Add permissions to manifest

 ```
 <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
 <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
 ```

### Using Activity

2. Add `CropImageActivity` into your AndroidManifest.xml
 ```xml
 <activity android:name="com.theartofdev.edmodo.cropper.CropImageActivity"
   android:theme="@style/Base.Theme.AppCompat"/> <!-- optional (needed if default theme has no action bar) -->
 ```

3. Start `CropImageActivity` using builder pattern from your activity
 ```java
 // start picker to get image for cropping and then use the image in cropping activity
 CropImage.activity()
   .setGuidelines(CropImageView.Guidelines.ON)
   .start(this);

 // start cropping activity for pre-acquired image saved on the device
 CropImage.activity(imageUri)
  .start(this);

 // for fragment (DO NOT use `getActivity()`)
 CropImage.activity()
   .start(getContext(), this);
 ```

4. Override `onActivityResult` method in your activity to get crop result
 ```java
 @Override
 public void onActivityResult(int requestCode, int resultCode, Intent data) {
   if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
     CropImage.ActivityResult result = CropImage.getActivityResult(data);
     if (resultCode == RESULT_OK) {
       Uri resultUri = result.getUri();
     } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
       Exception error = result.getError();
     }
   }
 }
 ```

### Using View
2. Add `CropImageView` into your activity
 ```xml
 <!-- Image Cropper fill the remaining available height -->
 <com.theartofdev.edmodo.cropper.CropImageView
   xmlns:custom="http://schemas.android.com/apk/res-auto"
   android:id="@+id/cropImageView"
   android:layout_width="match_parent"
   android:layout_height="0dp"
   android:layout_weight="1"/>
 ```

3. Set image to crop
 ```java
 cropImageView.setImageUriAsync(uri);
 // or (prefer using uri for performance and better user experience)
 cropImageView.setImageBitmap(bitmap);
 ```

4. Get cropped image
 ```java
 // subscribe to async event using cropImageView.setOnCropImageCompleteListener(listener)
 cropImageView.getCroppedImageAsync();
 // or
 Bitmap cropped = cropImageView.getCroppedImage();
 ```

## Features
- Built-in `CropImageActivity`.
- Set cropping image as Bitmap, Resource or Android URI (Gallery, Camera, Dropbox, etc.).
- Image rotation/flipping during cropping.
- Auto zoom-in/out to relevant cropping area.
- Auto rotate bitmap by image Exif data.
- Set result image min/max limits in pixels.
- Set initial crop window size/location.
- Request cropped image resize to specific size.
- Bitmap memory optimization, OOM handling (should never occur)!
- API Level 10.
- More..
 
## Customizations
- Cropping window shape: Rectangular or Oval (cube/circle by fixing aspect ratio).
- Cropping window aspect ratio: Free, 1:1, 4:3, 16:9 or Custom.
- Guidelines appearance: Off / Always On / Show on Toch.
- Cropping window Border line, border corner and guidelines thickness and color.
- Cropping background color.

For more information, see the [GitHub Wiki](https://github.com/ArthurHub/Android-Image-Cropper/wiki). 

## Posts
 - [Android cropping image from camera or gallery](http://theartofdev.com/2015/02/15/android-cropping-image-from-camera-or-gallery/)
 - [Android Image Cropper async support and custom progress UI](http://theartofdev.com/2016/01/15/android-image-cropper-async-support-and-custom-progress-ui/)
 - [Adding auto-zoom feature to Android-Image-Cropper](https://theartofdev.com/2016/04/25/adding-auto-zoom-feature-to-android-image-cropper/)

## Change log
*2.4.2*

- Fix instance state saved before any image loaded resulted in corrupted initial crop rectangle.
- Fix corners of crop overlay offset for oval shape resulting in confusing UI (#256).
- Fix support for negative rotation values for CropImageActivity.
- Add `start` method that accepts `android.app.Fragment`, annotated with `@RequiresApi` 11

*2.4.0*

- Super simple crop activity quick start flow that handles URI and permissions (huge thanks to @adwiv)
- Add image horizontal/vertical flipping during cropping (huge thanks to @tibbi).
- Handle OOM error for cropping set bitmap object, previously only image set by URI handled OOM.
- Allows for rotation degrees to be negative, fixes operator-precedence-related bug in flipAxes computation (thx Tim Cooke)
- Added crop overlay released listener (thx Richard Yee)
- Added originalUri and originalBitmap to CropResult object.
- Fix `resetCropRect()` resetting image rotation to 0 instead of the original exif data.
- Fix ignoring image rotation data in `setImageBitmap(Bitmap, ExifInterface )` method.
- Removed deprecated listeners.

*2.3.1*

- Fix image picker for xiaomi and huawei phones (thx @nicolabeghin)
- Fix crop window get corrupted on `CropImageView` resize.

See [full change log](https://github.com/ArthurHub/Android-Image-Cropper/wiki/Change-Log).

## License
Originally forked from [edmodo/cropper](https://github.com/edmodo/cropper).

Copyright 2016, Arthur Teplitzki, 2013, Edmodo, Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the   License.
You may obtain a copy of the License in the LICENSE file, or at:

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS   IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
