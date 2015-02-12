Android Image Cropper
=======
Forked from [edmodo/cropper](https://github.com/edmodo/cropper) fixing some bugs and adding some features.
An image cropping tool, displays a resizable crop window on top of the image. 
Calling the method getCroppedImage() will then return the Bitmap marked by the crop window.

### Setting image:
 - Bitmap: setImageBitmap(Bitmap bitmap), setImageBitmap(Bitmap bitmap, ExifInterface exif)
 - Resource: setImageResource(int resId)
 - URI: setImageUri(Uri uri)  

### Customizable:
- Rectengular or Oval cropping shape.
- Scale type of the image in the control.
- appearance of guidelines in the crop window.
- whether the aspect ratio is fixed or not and the ratio if fixed.

### Image rotation:
- Ability to provide ExifInterface for bitmap to fix rotation on load.
- Will automatically load ExifInterface data if loaded from URI to fix rotation on load.
- API to rotate loaded image.

### Loading from URI:
- Easy to use to crop image picked from Gallery or Camera.
- Loaded using samplaning to lower memory usage.
- Cropped rectange and cropped image will be on the original image, with option to adjust required size.

Supported on API Level 10 and above.
For more information, see the [linked Github Wiki page](https://github.com/edmodo/cropper/wiki). 

![ScreenShot](https://github.com/ArthurHub/Android-Image-Cropper/blob/master/demo.jpg?raw=true)

## Installation

**build.gradle**

	repositories {
	  maven { url 'http://dl.bintray.com/arthurhub/maven' }
	}

	dependencies {
	  compile 'com.theartofdev.edmodo:Android-Image-Cropper:1.0@aar'
	}

## License

Copyright 2013, Edmodo, Inc. 

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the License.
You may obtain a copy of the License in the LICENSE file, or at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
