package com.reactnativecommunity.slider.drawables;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.uimanager.ReactStylesDiffMap;
import com.facebook.react.uimanager.UIManagerModule;

public abstract class DrawableHandler implements PropsUpdater {
  static final int MAX_LEVEL = 10000;
  private final ReactContext mContext;
  private final Drawable mOriginal;
  private View mView;
  boolean mSystemDrawable = true;

  DrawableHandler(ReactContext context, Drawable original) {
    mContext = context;
    mOriginal = original;
  }

  Resources getResources() {
    return mContext.getResources();
  }

  abstract Drawable createDrawable();
  abstract Drawable get();
  abstract void set(Drawable drawable);
  abstract void onPreDraw(Canvas canvas);

  public final View getView() {
    return mView;
  }

  public final void setView(final int tag) {
    if (tag == View.NO_ID) {
      setView(null);
    } else/* if (mView == null || mView.getId() != tag)*/{
      UiThreadUtil.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          UIManagerModule uiManagerModule = mContext.getNativeModule(UIManagerModule.class);
          View view = uiManagerModule.resolveView(tag);
          setView(view);
        }
      });
    }
  }

  private void setView(@Nullable View view) {
    mView = view;
    if (mView != null) {
      mSystemDrawable = false;
      draw();
    } else {
      mSystemDrawable = true;
      restore();
    }
  }

  public final void tearDown() {

  }

  /**
   * restore to previous drawable if exists
   */
  void restoreToLast() {
    setView(mView);
  }

  void setInverted(boolean inverted) {}

  Rect getBounds() {
    return get().copyBounds();
  }

  private synchronized void draw() {
    Rect bounds = getBounds();
    //Bitmap bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
    //Canvas canvas = new Canvas(bitmap);
    //draw(canvas, mView);
    Drawable outDrawable = createDrawable();
    outDrawable.setState(get().getState());
    outDrawable.setLevel(get().getLevel());
    set(outDrawable);
    invalidate();
  }

  private void restore() {
    mOriginal.setLevel(get().getLevel());
    set(mOriginal);
    invalidate();
  }

  private void invalidate() {
    get().invalidateSelf();
  }

  public void setTintColor(Integer color) {
    if (color == null) {
      get().clearColorFilter();
    } else {
      get().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }
  }

  @Override
  public void updateFromProps(int tag, ReactStylesDiffMap props) {
    if (props == null) return;
    Drawable drawable = get();
    if (drawable == null) return;
    if (drawable instanceof ReactDrawableGroup.ReactRootDrawableGroup) {
      ((ReactDrawableGroup.ReactRootDrawableGroup) drawable).updateFromProps(tag, props);
    } else if (tag == getView().getId() && drawable instanceof ReactDrawable) {
      ((ReactDrawable) drawable).updateFromProps(props);
    }
  }

  void onViewAdded(ViewGroup parent, View view) {
    Drawable drawable = get();
    if (drawable instanceof ReactDrawableGroup.ReactRootDrawableGroup) {
      ((ReactDrawableGroup.ReactRootDrawableGroup) drawable).addView(parent, view);
    }
  }

  void onViewRemoved(ViewGroup parent, View view) {
    Drawable drawable = get();
    if (drawable instanceof ReactDrawableGroup.ReactRootDrawableGroup) {
      ((ReactDrawableGroup.ReactRootDrawableGroup) drawable).removeView(parent, view);
    }
  }

  void onViewInvalidated(View view) {
    Drawable drawable = get();
    if (drawable instanceof ReactDrawableGroup.ReactRootDrawableGroup) {
      ((ReactDrawableGroup.ReactRootDrawableGroup) drawable).draw(view);
    }
  }

}
