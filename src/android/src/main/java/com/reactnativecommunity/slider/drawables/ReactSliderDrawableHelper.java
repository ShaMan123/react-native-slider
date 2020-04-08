package com.reactnativecommunity.slider.drawables;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntDef;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.ReactStylesDiffMap;
import com.reactnativecommunity.slider.R;
import com.reactnativecommunity.slider.ReactInformantViewManager;
import com.reactnativecommunity.slider.ReactSlider;
import com.reactnativecommunity.slider.ReactSliderViewGroup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ReactSliderDrawableHelper implements ReactInformantViewManager.InformantRegistry.InformantTarget {

  @IntDef({
      SliderDrawable.BACKGROUND,
      SliderDrawable.MAXIMUM_TRACK,
      SliderDrawable.MINIMUM_TRACK,
      SliderDrawable.THUMB
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface SliderDrawable {
    int BACKGROUND = -1;
    int MAXIMUM_TRACK = 0;
    int MINIMUM_TRACK = 1;
    int THUMB = 2;
  }

  private final ReactSlider mSlider;

  public ReactSliderDrawableHelper(ReactSlider slider) {
    mSlider = slider;
    setViewBackgroundDrawable();
    LayerDrawable outDrawable = (LayerDrawable) slider.getProgressDrawable().getCurrent().mutate();
    LayerDrawable progressDrawable = ((LayerDrawable) slider.getResources().getDrawable(R.drawable.progress_layer).mutate());

    outDrawable.setDrawableByLayerId(ProgressDrawableHandler.ForegroundDrawableHandler.DRAWABLE_ID, new ColorDrawable(Color.TRANSPARENT) {
      @Override
      protected boolean onLevelChange(int level) {
        Log.d("Sliderr", "onLevelChange: " + level);
        if (mSlider.getParent() instanceof ReactSliderViewGroup) ((ReactSliderViewGroup) mSlider.getParent()).setLevel(level);
        return super.onLevelChange(level);
      }

      @Override
      protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        if (mSlider.getParent() instanceof ReactSliderViewGroup) ((ReactSliderViewGroup) mSlider.getParent()).setBounds(bounds);
      }
    });
    outDrawable.setDrawableByLayerId(ProgressDrawableHandler.BackgroundDrawableHandler.DRAWABLE_ID, new ColorDrawable(Color.TRANSPARENT));
    slider.setProgressDrawable(outDrawable);
  }

  /**
   * this fixes the thumb's ripple drawable and preserves it even when a background color is applied
   * when used with {@link #handleSetBackgroundColor(int)}
   */
  public void setViewBackgroundDrawable() {
    int color = Color.TRANSPARENT;
    if (mSlider.getBackground() instanceof ColorDrawable) {
      color = ((ColorDrawable) mSlider.getBackground()).getColor();
    }
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(Color.LTGRAY), null, null);
      LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{new ColorDrawable(color), rippleDrawable});
      mSlider.setBackground(layerDrawable);
    }
  }

  /**
   * {@link #setViewBackgroundDrawable()}
   * @param color
   */
  public void handleSetBackgroundColor(int color) {
    ((ColorDrawable) ((LayerDrawable) mSlider.getBackground()).getDrawable(0)).setColor(color);
  }

  public void setInverted(boolean inverted) {
    /*
    DrawableHandler[] handlers = new DrawableHandler[]{
        mBackgroundDrawableHandler,
        mMinimumTrackDrawableHandler,
        mMaximumTrackDrawableHandler,
        mThumbDrawableHandler};
    for (DrawableHandler handler: handlers) {
      handler.setInverted(inverted);
    }

     */
  }

  public void setThumbImage(final String uri) {
    //mThumbDrawableHandler.setThumbImage(uri);
  }

  public DrawableHandler getDrawableHandler(@SliderDrawable int type) {
    /*
    switch (type) {
      case SliderDrawable.BACKGROUND:
        return mBackgroundDrawableHandler;
      case SliderDrawable.MAXIMUM_TRACK:
        return mMaximumTrackDrawableHandler;
      case SliderDrawable.MINIMUM_TRACK:
        return mMinimumTrackDrawableHandler;
      case SliderDrawable.THUMB:
        return mThumbDrawableHandler;
      default:
        throw new Error("bad drawable type");
    }

     */
    return new DrawableHandler(((ReactContext) mSlider.getContext()),mSlider.getBackground()) {
      @Override
      Drawable createDrawable() {
        return get();
      }

      @Override
      Drawable get() {
        return new ColorDrawable(Color.MAGENTA);
      }

      @Override
      void set(Drawable drawable) {

      }

      @Override
      void onPreDraw(Canvas canvas) {

      }
    };
  }

  public void onTouchEvent(MotionEvent event) {
   // mThumbDrawableHandler.onTouchEvent(event);
  }

  @Override
  public void onReceiveProps(int recruiterID, View informant, ReactStylesDiffMap context) {
    /*
    DrawableHandler[] handlers = new DrawableHandler[]{
        mBackgroundDrawableHandler,
        mMinimumTrackDrawableHandler,
        mMaximumTrackDrawableHandler,
        mThumbDrawableHandler};
    for (DrawableHandler handler: handlers) {
      int id = handler.getView() != null ? handler.getView().getId() : View.NO_ID;
      if (id != View.NO_ID) {
        if (id == informant.getId()) {
          handler.updateFromProps(0, context);
          //break;
        }
        if (id == recruiterID) {
          handler.updateFromProps(informant.getId(), context);
          //handler.dispatchDraw();
          break;
        }
      }
    }

     */
  }

  @Override
  public void onViewAdded(int recruiterID, ViewGroup parent, View view) {
    DrawableHandler drawableHandler = findHandler(recruiterID);
    if (drawableHandler != null) {
      drawableHandler.onViewAdded(parent, view);
    }
  }

  @Override
  public void onViewRemoved(int recruiterID, ViewGroup parent, View view) {
    DrawableHandler drawableHandler = findHandler(recruiterID);
    if (drawableHandler != null) {
      drawableHandler.onViewRemoved(parent, view);
    }
  }

  @Override
  public void onViewInvalidated(int recruiterID, View view) {
    DrawableHandler drawableHandler = findHandler(recruiterID);
    if (drawableHandler != null) {
      drawableHandler.onViewInvalidated(view);
    }
  }

  private DrawableHandler findHandler(int informantID) {
    /*
    DrawableHandler[] handlers = new DrawableHandler[]{
        mBackgroundDrawableHandler,
        mMinimumTrackDrawableHandler,
        mMaximumTrackDrawableHandler,
        mThumbDrawableHandler};
    for (DrawableHandler handler: handlers) {
      int id = handler.getView() != null ? handler.getView().getId() : View.NO_ID;
      if (id == informantID) {
        return handler;
      }
    }

     */
    return null;
  }

  public void tearDown() {
    /*
    mMinimumTrackDrawableHandler.tearDown();
    mMaximumTrackDrawableHandler.tearDown();
    mBackgroundDrawableHandler.tearDown();
    mThumbDrawableHandler.tearDown();

     */
  }

  static Bitmap getBitmap(final View view, final String uri) {
    Bitmap bitmap = null;
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<Bitmap> future = executorService.submit(new Callable<Bitmap>() {
      @Override
      public Bitmap call() {
        Bitmap bitmap = null;
        try {
          if (uri.startsWith("http://") || uri.startsWith("https://") ||
              uri.startsWith("file://") || uri.startsWith("asset://") || uri.startsWith("data:")) {
            bitmap = BitmapFactory.decodeStream(new URL(uri).openStream());
          } else {
            int drawableId = view.getResources()
                .getIdentifier(uri, "drawable", view.getContext()
                    .getPackageName());
            bitmap = BitmapFactory.decodeResource(view.getResources(), drawableId);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        return bitmap;
      }
    });
    try {
      bitmap = future.get();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return bitmap;
  }

}
