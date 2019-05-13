package com.theartofdev.edmodo.cropper;

import android.net.Uri;

import java.util.ArrayList;

/**
 * Created by root on 9/14/17.
 */

public interface IImageCompressTaskListener {

    public void onComplete(ArrayList<Uri> compressed);
    public void onError(Throwable error);
}