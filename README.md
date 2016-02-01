Android Image Cropper
=======
[![build status](https://travis-ci.org/ArthurHub/Android-Image-Cropper.svg)](https://travis-ci.org/ArthurHub/Android-Image-Cropper) 
[![Codacy Badge](https://api.codacy.com/project/badge/grade/4d3781df0cce40959881a8d91365407a)](https://www.codacy.com/app/tep-arthur/Android-Image-Cropper)
[ ![Download](https://api.bintray.com/packages/arthurhub/maven/Android-Image-Cropper/images/download.svg) ](https://bintray.com/arthurhub/maven/Android-Image-Cropper/_latestVersion)

Image cropping tool, displays a resizable, rectengular/oval crop window on top of image.

[Optimized for cropping image picked from Camera or Gallery](http://theartofdev.com/2015/02/15/android-cropping-image-from-camera-or-gallery/)
- Support setting cropping image by Android URI loaded by ContentResolver.
- Auto image roatete by reading Exif data to handle rotation by camera.
- Using sampling to reduce memory usage and prevent out-of-memory.
- Support required size and sampling on getting cropped image for memory optimization.

![Crop](https://github.com/ArthurHub/Android-Image-Cropper/blob/master/crop.jpg?raw=true)

### Features:
- Set cropping image as Bitmap, Resource or Android URI.
- Set cropping window shape to Rectengular or Oval (circle by setting fixed aspect ration).
- Set image Scale type in the cropping image view: center or fit.
- Control the appearance of guidelines in the crop window.
- Control cropping window aspect ratio, ability to fix it (squared).
- Customization for border line, border corner, guidelines and background.
- Set result image min/max limits in pixels.
- Auto rotate bitmap by provided Exif data or loading from Android URI.
- Rotate image API to allow the user to rotate the image during cropping.
- Get cropping rectangle or the cropped bitmap.
- Supported on API Level 10 and above.

For more information, see the [linked Github Wiki page](https://github.com/ArthurHub/Android-Image-Cropper/wiki/Android-Image-Cropper). 

![ScreenShot](https://github.com/ArthurHub/Android-Image-Cropper/blob/master/demo.jpg?raw=true)

## Gradle
```
compile 'com.theartofdev.edmodo:android-image-cropper:1.2.+'
```

## Posts
 - [Android cropping image from camera or gallery](http://theartofdev.com/2015/02/15/android-cropping-image-from-camera-or-gallery/)
 - [Android Image Cropper async support and custom progress UI](http://theartofdev.com/2016/01/15/android-image-cropper-async-support-and-custom-progress-ui/)

## Change log
*1.2.0 (beta)*

Due too large changes in the internals please consider this a **beta** release, if you use it be sure to test is thoroughly and report any bugs you find (report no bugs will also be awesome) or use 1.1.0 until I feel 1.2.* is stable enough.
- Rewrite internal crop window handling.
-  Add `crop` prefix to all customization resources to prevent naming collision (**breaking change**).
- Add `CropImageView.Guidelines` enum of guidelines config instead of integer (**breaking change**).
- Change custom attributes types to `dimension` where appropriate (**breaking change**).
- Add `showCropOverlay` attribute and `setShowCropOverlay(boolean)` method allowing to hide/show crop overlay UI for animation or element transition.
- Add `cropInitialCropWindowPaddingRatio` customization [0 - 0.5) to control initial crop window padding from image borders relative to image size.
- Add min limit config on cropping window width/height in the UI (`cropMinCropWindowWidth`, `cropMinCropWindowHeight`)
Add min/max config on cropping image result width/height (`cropMinCropResultWidthPX`,`cropMinCropResultHeightPX`,`cropMaxCropResultWidthPX`,`cropMaxCropResultHeightPX`)

*1.1.1*
- Add customization support for border line, border corner, guidelines and background.
- Fix progress bar not showing on loading if previously bitmap was directly set.

See [full change log](https://github.com/ArthurHub/Android-Image-Cropper/wiki/Change-Log).

## License
Forked from [edmodo/cropper](https://github.com/edmodo/cropper) fixing some bugs and adding some features.

Copyright 2013, Edmodo, Inc. 

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the   License.
You may obtain a copy of the License in the LICENSE file, or at:

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS   IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
