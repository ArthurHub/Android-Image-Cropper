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

package com.theartofdev.edmodo.cropper.sample;

import android.util.Pair;

import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;

/** The crop image view options that can be changed live. */
final class CropImageViewOptions {

  public CropImageView.ScaleType scaleType = CropImageView.ScaleType.CENTER_INSIDE;

  public CropImageView.CropShape cropShape = CropImageView.CropShape.RECTANGLE;

  public CropImageView.Guidelines guidelines = CropImageView.Guidelines.ON_TOUCH;

  public Pair<Integer, Integer> aspectRatio = new Pair<>(1, 1);

  public Pair<Integer, Integer> imageRatio = new Pair<>(1, 1);

  public boolean autoZoomEnabled;

  public int maxZoomLevel;

  public boolean fixAspectRatio;

  public boolean multitouch;

  public boolean showCropOverlay;

  public boolean showProgressBar;

  public boolean flipHorizontally;

  public boolean flipVertically;

  public enum RatioType {
    FREE(0),
    R_1_1(1),
    R_3_4(2),
    R_16_9(3),
    R_9_16(4),
    IMAGE(5);

    private int value;
    private static Map map = new HashMap<>();

    private RatioType(int value) {
      this.value = value;
    }

    static {
      for (RatioType ratioType : RatioType.values()) {
        map.put(ratioType.value, ratioType);
      }
    }

    public static RatioType valueOf(int ratioType) {
      return (RatioType)map.get(ratioType);
    }

    public int getValue() {
      return value;
    }
  }

  private RatioType ratioType = RatioType.FREE;

  public void cycleRatioType () {
    this.ratioType = RatioType.valueOf((this.ratioType.getValue() + 1) % 6);
    setRatioType(this.ratioType);
  }

  public void setRatioType(RatioType ratioType) {
    this.ratioType = ratioType;
    this.fixAspectRatio = this.ratioType != RatioType.FREE;
    switch (this.ratioType) {
      case R_1_1:
        this.aspectRatio = new Pair<>(1, 1);
        break;
      case R_3_4:
        this.aspectRatio = new Pair<>(3, 4);
        break;
      case R_16_9:
        this.aspectRatio = new Pair<>(16, 9);
        break;
      case R_9_16:
        this.aspectRatio = new Pair<>(9, 16);
        break;
      case IMAGE:
        this.aspectRatio = this.imageRatio;
        break;
      default:
        break;
    }
  }

  public String getRatioTypeString() {
    if (this.ratioType == RatioType.FREE) {
      return "FREE";
    } else if (this.ratioType == RatioType.IMAGE) {
      return "IMAGE";
    }

    return this.aspectRatio.first + ":" + this.aspectRatio.second;
  }
}
