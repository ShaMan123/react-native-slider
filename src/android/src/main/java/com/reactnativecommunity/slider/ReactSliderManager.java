/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reactnativecommunity.slider;

import android.os.Build;
import android.view.View;
import android.widget.SeekBar;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.LayoutShadowNode;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaMeasureMode;
import com.facebook.yoga.YogaMeasureOutput;
import com.facebook.yoga.YogaNode;
import com.reactnativecommunity.slider.ReactInformantViewManager.InformantRegistry;
import com.reactnativecommunity.slider.drawables.ReactSliderDrawableHelper.SliderDrawable;
import com.reactnativecommunity.slider.drawables.DrawableHandler;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Manages instances of {@code ReactSlider}.
 *
 * Note that the slider is _not_ a controlled component.
 */
@ReactModule(name = ReactSliderManager.REACT_CLASS)
public class ReactSliderManager extends SimpleViewManager<ReactSlider> {

  private static final int STYLE = android.R.attr.seekBarStyle;
  private static final String DEFAULT_COLOR = "#009688";

  public static final String REACT_CLASS = "RNCSlider";

  static class ReactSliderShadowNode extends LayoutShadowNode implements
      YogaMeasureFunction {

    private int mWidth;
    private int mHeight;
    private boolean mMeasured;

    private ReactSliderShadowNode() {
      initMeasureFunction();
    }

    private void initMeasureFunction() {
      setMeasureFunction(this);
    }

    @Override
    public long measure(
        YogaNode node,
        float width,
        YogaMeasureMode widthMode,
        float height,
        YogaMeasureMode heightMode) {
      if (!mMeasured) {
        SeekBar reactSlider = new ReactSlider(getThemedContext(), null, STYLE);
        final int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        reactSlider.measure(spec, spec);
        mWidth = reactSlider.getMeasuredWidth();
        mHeight = reactSlider.getMeasuredHeight();
        mMeasured = true;
      }

      return YogaMeasureOutput.make(mWidth, mHeight);
    }
  }

  private static final SeekBar.OnSeekBarChangeListener ON_CHANGE_LISTENER =
      new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
          ReactContext reactContext = (ReactContext) seekbar.getContext();
          reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher().dispatchEvent(
              new ReactSliderEvent(
                  seekbar.getId(),
                  ((ReactSlider) seekbar).toRealProgress(progress),
                  fromUser));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekbar) {
          ReactContext reactContext = (ReactContext) seekbar.getContext();
          reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher().dispatchEvent(
              new ReactSlidingStartEvent(
                  seekbar.getId(),
                  ((ReactSlider) seekbar).toRealProgress(seekbar.getProgress())));
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekbar) {
          ReactContext reactContext = (ReactContext) seekbar.getContext();
          reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher().dispatchEvent(
              new ReactSlidingCompleteEvent(
                  seekbar.getId(),
                  ((ReactSlider) seekbar).toRealProgress(seekbar.getProgress())));
        }
      };

  private final InformantRegistry mInformantRegistry;

  ReactSliderManager(InformantRegistry registry) {
    super();
    mInformantRegistry = registry;
  }

  private DrawableHandler getDrawableHandler(ReactSlider view, @SliderDrawable int type) {
    return view.drawableHelper.getDrawableHandler(type);
  }

  private void handleRegistration(ReactSlider receiver, Integer informantTag, @SliderDrawable int type) {
    if (informantTag != null) {
      mInformantRegistry.add(receiver, informantTag, true);
    } else {
      DrawableHandler handler = getDrawableHandler(receiver, type);
      View informant = handler.getView();
      if (informant != null) {
        mInformantRegistry.remove(informant.getId());
      }
    }
  }

  private void registerView(ReactSlider view, Integer tag, @SliderDrawable int type) {
    getDrawableHandler(view, type).setView(tag == null ? -1 : tag);
    handleRegistration(view, tag, type);
  }

  private void setTintColor(ReactSlider view, Integer color, @SliderDrawable int type) {
    getDrawableHandler(view, type).setTintColor(color);
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  public LayoutShadowNode createShadowNodeInstance() {
    return new ReactSliderShadowNode();
  }

  @Override
  public Class getShadowNodeClass() {
    return ReactSliderShadowNode.class;
  }

  @Override
  protected ReactSlider createViewInstance(ThemedReactContext context) {
    ReactSlider slider = new ReactSlider(context, null, STYLE);

    if (Build.VERSION.SDK_INT >= 21) {
      /**
       * The "splitTrack" parameter should have "false" value,
       * otherwise the SeekBar progress line doesn't appear when it is rotated.
       */
      slider.setSplitTrack(false);
    }

    return slider;
  }

  @Override
  public void onDropViewInstance(@Nonnull ReactSlider view) {
    view.drawableHelper.tearDown();
  }

  @ReactProp(name = ViewProps.ENABLED, defaultBoolean = true)
  public void setEnabled(ReactSlider view, boolean enabled) {
    view.setEnabled(enabled);
  }

  @ReactProp(name = "value", defaultDouble = 0d)
  public void setValue(ReactSlider view, double value) {
    view.setOnSeekBarChangeListener(null);
    view.setValue(value);
    view.setOnSeekBarChangeListener(ON_CHANGE_LISTENER);
  }

  @ReactProp(name = "minimumValue", defaultDouble = 0d)
  public void setMinimumValue(ReactSlider view, double value) {
    view.setMinValue(value);
  }

  @ReactProp(name = "maximumValue", defaultDouble = 1d)
  public void setMaximumValue(ReactSlider view, double value) {
    view.setMaxValue(value);
  }

  @ReactProp(name = "step", defaultDouble = 0d)
  public void setStep(ReactSlider view, double value) {
    view.setStep(value);
  }

  @ReactProp(name = "thumbTintColor", customType = "Color")
  public void setThumbTintColor(ReactSlider view, Integer color) {
    setTintColor(view, color, SliderDrawable.THUMB);
  }

  @ReactProp(name = "thumbViewTag")
  public void setThumbView(ReactSlider view, Integer tag) {
    registerView(view, tag, SliderDrawable.THUMB);
  }

  @ReactProp(name = "thumbImage")
  public void setThumbImage(ReactSlider view, @Nullable ReadableMap source) {
    String uri = null;
    if (source != null) {
      uri = source.getString("uri");
    }
    view.drawableHelper.setThumbImage(uri);
  }

  @ReactProp(name = "minimumTrackTintColor", customType = "Color")
  public void setMinimumTrackTintColor(ReactSlider view, Integer color) {
    setTintColor(view, color, SliderDrawable.MINIMUM_TRACK);
  }

  @ReactProp(name = "minimumTrackViewTag")
  public void setMinimumTrackView(ReactSlider view, Integer tag) {
    registerView(view, tag, SliderDrawable.MINIMUM_TRACK);
  }

  @ReactProp(name = "maximumTrackTintColor", customType = "Color")
  public void setMaximumTrackTintColor(ReactSlider view, Integer color) {
    setTintColor(view, color, SliderDrawable.MAXIMUM_TRACK);
  }

  @ReactProp(name = "maximumTrackViewTag")
  public void setMaximumTrackView(ReactSlider view, Integer tag) {
    registerView(view, tag, SliderDrawable.MAXIMUM_TRACK);
  }

  @ReactProp(name = "backgroundTrackTintColor", customType = "Color")
  public void setBackgroundTrackTintColor(ReactSlider view, Integer color) {
    setTintColor(view, color, SliderDrawable.BACKGROUND);
  }

  @ReactProp(name = "backgroundTrackViewTag")
  public void setBackgroundTrackView(ReactSlider view, Integer tag) {
    registerView(view, tag, SliderDrawable.BACKGROUND);
  }

  @ReactProp(name = "inverted", defaultBoolean = false)
  public void setInverted(ReactSlider view, boolean inverted) {
    view.setInverted(inverted);
  }

  @Override
  protected void addEventEmitters(final ThemedReactContext reactContext, final ReactSlider view) {
    view.setOnSeekBarChangeListener(ON_CHANGE_LISTENER);
  }

  @Override
  public Map getExportedCustomDirectEventTypeConstants() {
    return MapBuilder.of(ReactSlidingCompleteEvent.EVENT_NAME, MapBuilder.of("registrationName", "onRNCSliderSlidingComplete"),
        ReactSlidingStartEvent.EVENT_NAME, MapBuilder.of("registrationName", "onRNCSliderSlidingStart"));
  }

  @Nullable
  @Override
  public Map getConstants() {
    return MapBuilder.of("style", MapBuilder.of("defaultColor", DEFAULT_COLOR));
  }

}
