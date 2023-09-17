// "Therefore those skilled at the unorthodox
// are infinite as heaven and earth,
// inexhaustible as the great rivers.
// When they come to an end,
// they begin again,
// like the days and months;
// they die and are reborn,
// like the four seasons."
//
// - Sun Tsu,
// "The Art of War"

package com.theartofdev.edmodo.cropper.quick.start;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  /** Start pick image activity with chooser. */
  public void onSelectImageClick(View view) {
    CropImage.activity()
        .setGuidelines(CropImageView.Guidelines.ON)
        .setActivityTitle("My Crop")
        .setCropShape(CropImageView.CropShape.OVAL)
        .setCropMenuCropButtonTitle("Done")
        .setRequestedSize(400, 400)
        .setCropMenuCropButtonIcon(R.drawable.ic_launcher)
        .start(this);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    // handle result of CropImageActivity
    if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
      CropImage.ActivityResult result = CropImage.getActivityResult(data);
      if (resultCode == RESULT_OK) {
        ((ImageView) findViewById(R.id.quick_start_cropped_image)).setImageURI(result.getUri());
        Toast.makeText(
                this, "Cropping successful, Sample: " + result.getSampleSize(), Toast.LENGTH_LONG)
            .show();
      } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
        Toast.makeText(this, "Cropping failed: " + result.getError(), Toast.LENGTH_LONG).show();
      }
    }
  }
}
