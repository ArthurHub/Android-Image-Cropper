Android Image Cropper
=======
Forked from [edmodo/cropper](https://github.com/edmodo/cropper) fixing some bugs and adding some features.

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
- Auto rotate bitmap by provided Exif data or loading from Android URI.
- Rotate image API to allow the user to rotate the image during cropping.
- Get cropping rectangle or the cropped bitmap.
- Supported on API Level 10 and above.

For more information, see the [linked Github Wiki page](https://github.com/ArthurHub/Android-Image-Cropper/wiki/Android-Image-Cropper). 

![ScreenShot](https://github.com/ArthurHub/Android-Image-Cropper/blob/master/demo.jpg?raw=true)

## Gradle
```
compile 'com.theartofdev.edmodo:android-image-cropper:1.0.+'
```
or with using [AARLinkSources Plugin](https://github.com/xujiaao/AARLinkSources) to get the [sources in the IDE](https://groups.google.com/forum/#!msg/adt-dev/yVPo71O_ZKM/BcYP-hLai2wJ).
```
buildscript {
    repositories {
        maven { url 'https://raw.github.com/xujiaao/mvn-repository/master/releases' }
    }

    dependencies {
        classpath 'com.github.xujiaao:aarLinkSources:1.0.0'
    }
}

apply plugin: 'aar-link-sources'

dependencies {
  compile 'com.theartofdev.edmodo:android-image-cropper:1.0.+'
  aarLinkSources 'com.theartofdev.edmodo:android-image-cropper:1.0.+:sources@jar'
}
```

## License
Copyright 2013, Edmodo, Inc. 

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the   License.
You may obtain a copy of the License in the LICENSE file, or at:

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS   IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
