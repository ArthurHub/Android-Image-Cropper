package com.theartofdev.edmodo.cropper;

import android.os.Parcel;
import android.os.Parcelable;

public class ParcelableHelper
{
    /**
     * There is not always a guarantee that Parcelable values will be immediately written out and
     * read back in.  For data data that mutable (its own issue), this can be a problem.  This is
     * for the times when it would be great to have confidence that you will be working with a copy
     * of that data.
     */
    public static Parcelable immediateDeepCopy(Parcelable input) {
        return immediateDeepCopy(input, input.getClass().getClassLoader());
    }

    /**
     * Same as {@link #immediateDeepCopy(android.os.Parcelable)}, but for when you need a little
     * more control over which ClassLoader will be used.
     */

    public static Parcelable immediateDeepCopy(Parcelable input, ClassLoader classLoader)
    {
        Parcel parcel = null;
        try
        {
            parcel = Parcel.obtain();
            parcel.writeParcelable(input, 0);
            parcel.setDataPosition(0);
            return parcel.readParcelable(classLoader);
        }
        finally {
            if (parcel != null) {
                parcel.recycle();
            }
        }
    }
}