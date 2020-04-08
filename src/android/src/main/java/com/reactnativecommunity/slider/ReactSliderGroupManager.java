package com.reactnativecommunity.slider;

import androidx.annotation.NonNull;

import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.views.view.ReactViewManager;

public class ReactSliderGroupManager extends ViewGroupManager<ReactSliderViewGroup> {
  private final static String REACT_CLASS = "RNCSliderContainer";

  @NonNull
  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @NonNull
  @Override
  protected ReactSliderViewGroup createViewInstance(@NonNull ThemedReactContext reactContext) {
    return new ReactSliderViewGroup(reactContext);
  }

  @ReactProp(name = "inverted", defaultBoolean = false)
  public void setInverted(ReactSliderViewGroup view, boolean inverted) {
    view.setInverted(inverted);
  }

}
