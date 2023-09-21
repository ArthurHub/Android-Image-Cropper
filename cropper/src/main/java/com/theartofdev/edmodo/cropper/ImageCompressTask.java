package com.theartofdev.edmodo.cropper;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.ArrayList;

public class ImageCompressTask implements Runnable
{
    private Context mContext;
    private ArrayList<Uri> originalPaths = new ArrayList<>();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private ArrayList<Uri> result = new ArrayList<>();
    private IImageCompressTaskListener mIImageCompressTaskListener;
    private CropImageOptions mOptions;

    public ImageCompressTask(Context context, Uri path, CropImageOptions options, IImageCompressTaskListener compressTaskListener)
    {
        originalPaths.add(path);
        mContext = context;
        mOptions = options;
        mIImageCompressTaskListener = compressTaskListener;
    }

    public ImageCompressTask(Context context, ArrayList<Uri> paths, CropImageOptions options, IImageCompressTaskListener compressTaskListener)
    {
        originalPaths.addAll(paths);
        mContext = context;
        mOptions = options;
        mIImageCompressTaskListener = compressTaskListener;
    }

    @Override
    public void run()
    {
        try
        {
            //Loop through all the given paths and collect the compressed file from Util.getCompressed(Context, String)
            for (Uri path : originalPaths)
            {
                Uri output = Util.getCompressed(mContext, path, mOptions);
                result.add(output);
            }
            //use Handler to post the result back to the main Thread
            mHandler.post(new Runnable() {
                @Override
                public void run() {

                    if(mIImageCompressTaskListener != null)
                        mIImageCompressTaskListener.onComplete(result);
                }
            });
        }
        catch (final IOException ex)
        {
            //There was an error, report the error back through the callback
            mHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    if(mIImageCompressTaskListener != null)
                        mIImageCompressTaskListener.onError(ex);
                }
            });
        }
    }
}
