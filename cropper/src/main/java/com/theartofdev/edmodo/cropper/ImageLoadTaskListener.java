package com.theartofdev.edmodo.cropper;

import android.graphics.Bitmap;

/**
 * Created by root on 9/14/17.
 */

public interface ImageLoadTaskListener {

    public void onComplete(Bitmap bitmap);
    public void onError(Throwable error);
}