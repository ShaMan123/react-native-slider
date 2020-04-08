package com.reactnativecommunity.slider;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.react.modules.i18nmanager.I18nUtil;
import com.facebook.react.views.view.ReactViewGroup;
import com.reactnativecommunity.slider.drawables.ReactSliderDrawableHelper.SliderDrawable;

public class ReactSliderViewGroup extends ReactViewGroup {

  private boolean mIsInverted = false;
  private Rect mBounds;

  public ReactSliderViewGroup(Context context) {
    super(context);
  }

  public void setInverted(boolean inverted) {
    mIsInverted = inverted;
    setScaleX(getScaleX());
    invalidate();
  }

  @Override
  public void setScaleX(float scaleX) {
    super.setScaleX(scaleX * (mIsInverted ? -1 : 1));
  }

  ReactSlider getSlider() {
    return (ReactSlider) getChildAt(0);
  }

  public int getIndex(@SliderDrawable int type) {
    switch (type) {
      case SliderDrawable.BACKGROUND:
        return 1;
      case SliderDrawable.MAXIMUM_TRACK:
        return 2;
      case SliderDrawable.MINIMUM_TRACK:
        return 3;
      case SliderDrawable.THUMB:
        return 4;
      default:
        return -1;
    }
  }

  public View getViewByType(@SliderDrawable int type) {
    return getChildAt(getIndex(type));
  }

  private PointF getThumbSize() {
    View thumb = getViewByType(SliderDrawable.THUMB);
    if (thumb instanceof ViewGroup) {
      View inner = ((ViewGroup) thumb).getChildAt(0);
      return new PointF(inner.getWidth(), inner.getHeight());
    } else {
      return new PointF(0, 0);
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    refresh();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    refresh();
  }

  public void setLevel(int level) {
    setMaximumTrackLevel(level);
    setMinimumTrackLevel(level);
    setThumbLevel(level);
  }

  private void setMinimumTrackLevel(int level) {
    View track = getViewByType(SliderDrawable.MINIMUM_TRACK);
    PointF thumb = getThumbSize();
    float scale = level * 1f / 10000;

    //scale *= (mBounds.width() - thumb.x / 2) / track.getWidth();
    float delta = track.getWidth() * (1 - scale) / 2;
    track.setTranslationX(-delta);
    track.setScaleX(scale);
  }

  private void setMaximumTrackLevel(int level) {
    View track = getViewByType(SliderDrawable.MAXIMUM_TRACK);
    PointF thumb = getThumbSize();
    float scale = 1 - level * 1f / 10000;
    //scale *= (mBounds.width() - thumb.x / 2) / track.getWidth();
    Log.d("Sliderrrrrrrrrrrrr", "setMaximumTrackLevel: " + scale);
    float delta = track.getWidth() * (1 - scale) / 2;
    track.setTranslationX(delta);
    track.setScaleX(scale);
  }

  private void setThumbLevel(int level) {
    float scale = level * 1f / 10000;
    if (mIsInverted) scale = 1 - scale;
    float translateX = mBounds.width() * scale; //* (I18nUtil.getInstance().isRTL(getContext()) ? -1: 1);
    //if ()
    View thumb = getViewByType(SliderDrawable.THUMB);
    thumb.setTranslationX(translateX);
    thumb.setTranslationY(thumb.getHeight() / 2);
  }

  public void setBounds(Rect bounds) {
    mBounds = bounds;
    refresh();
  }

  private void refresh() {
    ReactSlider slider = getSlider();
    slider.setProgress(slider.getProgress());
  }
}
