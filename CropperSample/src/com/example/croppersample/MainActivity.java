
package com.example.croppersample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;

import com.edmodo.cropper.CropImageView;

public class MainActivity extends Activity {

    Bitmap croppedImage;
    
    private final static int DEFAULT_ASPECT_RATIO_VALUES = 10;
    
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        // Sets fonts for all
        Typeface mFont = Typeface.createFromAsset(getAssets(), "Roboto-Thin.ttf");
        ViewGroup root = (ViewGroup) findViewById(R.id.mylayout);
        setFont(root, mFont);

        final CropImageView cropImageView = (CropImageView) findViewById(R.id.CropImageView);

        // Sets fixedAspectRatio
        Switch fixedAspectRatioSwitch = (Switch) findViewById(R.id.fixedAspectRatioSwitch);
        fixedAspectRatioSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                cropImageView.setFixedAspectRatio(isChecked);
            }

        });
        
        //Sets initial aspect ratio to 10/10, for demonstration purposes
        cropImageView.setAspectRatioX(DEFAULT_ASPECT_RATIO_VALUES);
        cropImageView.setAspectRatioY(DEFAULT_ASPECT_RATIO_VALUES);
        
        // Sets aspectRatioX
        final TextView aspectRatioX = (TextView) findViewById(R.id.aspectRatioX);
        SeekBar aspectRatioXSeek = (SeekBar) findViewById(R.id.aspectRatioXSeek);
        aspectRatioXSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar aspectRatioXSeek, int progress, boolean fromUser) {
                try {
                    cropImageView.setAspectRatioX(progress);
                    aspectRatioX.setText("aspectRatioX = " + progress);
                } catch (IllegalArgumentException e) {
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Sets aspectRatioY
        final TextView aspectRatioY = (TextView) findViewById(R.id.aspectRatioY);
        SeekBar aspectRatioYSeek = (SeekBar) findViewById(R.id.aspectRatioYSeek);
        aspectRatioYSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar aspectRatioYSeek, int progress, boolean fromUser) {
                try {
                    cropImageView.setAspectRatioY(progress);
                    aspectRatioY.setText("aspectRatioY = " + progress);
                } catch (IllegalArgumentException e) {
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        
        // Sets showGuidelines
        final TextView showGuidelines = (TextView) findViewById(R.id.showGuidelines);
        SeekBar showGuidelinesSeek = (SeekBar) findViewById(R.id.showGuidelinesSeek);
        showGuidelinesSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar showGuidelinesSeek, int progress, boolean fromUser) {
                try {
                    cropImageView.setGuidelines(progress);
                    if (progress == 0)
                        showGuidelines.setText("showGuidelines = off");
                    else if (progress == 1)
                        showGuidelines.setText("showGuidelines = onTouch");
                    else if (progress == 2)
                        showGuidelines.setText("showGuidelines = on");
                    
                } catch (IllegalArgumentException e) {
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        
        final Button cropButton = (Button) findViewById(R.id.Button_crop);
        cropButton.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                croppedImage = cropImageView.getCroppedImage();
                ImageView croppedImageView = (ImageView) findViewById(R.id.croppedImageView);
                croppedImageView.setImageBitmap(croppedImage);
            }
        });

    }

    /*
     * Sets the font on all TextViews in the ViewGroup. Searches recursively for
     * all inner ViewGroups as well. Just add a check for any other views you
     * want to set as well (EditText, etc.)
     */
    public void setFont(ViewGroup group, Typeface font) {
        int count = group.getChildCount();
        View v;
        for (int i = 0; i < count; i++) {
            v = group.getChildAt(i);
            if (v instanceof TextView || v instanceof EditText || v instanceof Button) {
                ((TextView) v).setTypeface(font);
            } else if (v instanceof ViewGroup)
                setFont((ViewGroup) v, font);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
