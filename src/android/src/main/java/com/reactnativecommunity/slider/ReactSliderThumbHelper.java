package com.reactnativecommunity.slider;

import android.view.View;

import com.reactnativecommunity.slider.ReactSliderDrawableHelper.SliderDrawable;

public class ReactSliderThumbHelper extends DrawableHelper {
  ReactSliderThumbHelper(SliderContainer drawableContainer) {
    super(drawableContainer);
  }

  @Override
  void tryAttach(int index, View view) {
    if (index == SliderDrawable.THUMB) {
      attach(view);
    }
  }

}
