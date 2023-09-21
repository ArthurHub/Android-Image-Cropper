package com.theartofdev.edmodo.cropper;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

public class ImageLoadTask implements Runnable
{
    private Context mContext;
    private Uri originalPath = null;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private int mReqWidth, mReqHeight;
    private ImageLoadTaskListener mImageLoadTaskListener;

    public ImageLoadTask(Context context, Uri path, int reqWidth, int reqHeight, ImageLoadTaskListener loadTaskListener)
    {
        mContext = context;
        originalPath = path;
        mReqWidth = reqWidth;
        mReqHeight = reqHeight;
        mImageLoadTaskListener = loadTaskListener;
    }

    @Override
    public void run()
    {
        final Bitmap output = Util.decodeSampledBitmapFromResource(mContext, originalPath, mReqWidth, mReqHeight);

        //use Handler to post the result back to the main Thread
        mHandler.post(new Runnable() {
            @Override
            public void run()
            {
                if(mImageLoadTaskListener != null)
                {
                    if(output != null)
                        mImageLoadTaskListener.onComplete(output);
                    else
                        mImageLoadTaskListener.onError(new Exception("Bitmap is null"));
                }
            }
        });
    }
}
