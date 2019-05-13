package com.theartofdev.edmodo.cropper.sample;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.croppersample.R;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.theartofdev.edmodo.cropper.CropImages;

import java.io.File;
import java.util.ArrayList;

public class AddMultiplePhotos extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_multiple);
        setupViews();
    }

    public void setupViews()
    {
        AppCompatButton clickHere = findViewById(R.id.click_here);
        clickHere.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createIntentChooser();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImages.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImages.ActivityResult result = CropImages.getActivityResult(data);
            if (resultCode == RESULT_OK)
            {
                ArrayList<Uri> resultUri = result.getResultUri();
                Log.d("fatal", "New photo size ==> " + resultUri.toString()); //log new file size.
            }
            else if (resultCode == CropImages.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE)
            {
                Exception error = result.getError();
                error.printStackTrace();
                Log.d("fatal", "Some error occurred "); //log new file size.
            }
            else
            {
                Log.d("fatal", "Back button pressed"); //log new file size.
            }
        }
    }

    private void createIntentChooser() {
        // start picker to get image for cropping and then use the image in cropping activity
        CropImages.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAllowRotation(true)
                .setAllowCounterRotation(true)
                .setRequestedSize(1920, 1920)
                .setAllowFlipping(false)
                .setAutoZoomEnabled(true)
                .setMinCropResultSize(480, 480)
                .setMinCropWindowSize(480, 480)
                .start(this);
    }

    public static void deleteCache(Context context)
    {
        try
        {
            File dir = context.getCacheDir();
            deleteFiles(dir, "cropped");

            File extDir = context.getExternalCacheDir();
            deleteFiles(extDir, "pick");

        }
        catch (Exception e) {}
    }

    private static void deleteFiles(File dir, String name)
    {
        if (dir != null && dir.isDirectory())
        {
            File[] children = dir.listFiles();
            for (File ff: children)
            {
                if(ff.isFile() && ff.getName().startsWith(name))
                    ff.delete();
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        deleteCache(AddMultiplePhotos.this);
        super.onDestroy();
    }
}