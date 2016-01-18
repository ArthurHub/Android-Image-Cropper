*1.1.1*
- Add customization support for border line, border corner, guidelines and background.
- Fix progress bar not showing on loading if previously bitmap was directly set.

*1.1.0*
- Deprecated `setImageUri(Uri)`.
- Added `setImageUriAsync(Uri)` and `getCroppedImageAsync()` for better handling of slow image loading/decode/cropping.
- Fixed Save/Restore state handling, proper orientation change expirience.
- Bug fixes on rotation and bitmap recycled error.

*1.0.7*
 - Added `setSnapRadius(float)` allowing to disable snap by setting 0.
 - Nicer rectengular crop border.
 - Fix oval shape rendering on old devices by disabling hardware rendering when required.
