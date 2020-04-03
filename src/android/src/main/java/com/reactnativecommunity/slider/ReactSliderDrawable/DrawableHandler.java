package com.reactnativecommunity.slider.ReactSliderDrawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.FloatRange;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.uimanager.ReactStylesDiffMap;
import com.facebook.react.uimanager.UIManagerModule;
import com.reactnativecommunity.slider.ReactInformantViewManager.InformantRegistry.InformantTarget;

public abstract class DrawableHandler implements ViewTreeObserver.OnDrawListener, InformantTarget<ReactStylesDiffMap> {
  private final ReactContext mContext;
  private final Drawable mOriginal;
  private View mView;
  private boolean mIsDrawing = false;
  private float mOpacity = 1;

  DrawableHandler(ReactContext context, Drawable original) {
    mContext = context;
    mOriginal = original;
  }

  Drawable createDrawable(Resources res, Bitmap bitmap) {
    return new ReactDrawable(res, bitmap);
  }

  abstract Drawable get();
  abstract void set(Drawable drawable);
  abstract void draw(Canvas canvas, View view);

  boolean isCustomDrawable() {
    return mView != null;
  }

  public final View getView() {
    return mView;
  }

  @Override
  public void onDraw() {
    if (mView != null && !mIsDrawing && mView.isDirty()) {
      draw();
    }
  }

  @Override
  public void receiveFromInformant(int informantID, int recruiterID, ReactStylesDiffMap context) {
    if (getView() != null && recruiterID == getView().getId()) {
      if (context.hasKey("opacity")) {
        setOpacity((float) context.getDouble("opacity", mOpacity));
      }
      if (context.hasKey("transform") && get() instanceof ReactDrawable) {
        ((ReactDrawable) get()).setTransform(context.getArray("transform"));
      }
    }
  }

  public final void setView(final int tag) {
    if (tag == View.NO_ID) {
      setView(null);
    } else {
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

  final void setView(@Nullable View view) {
    if (mView != view) {
      if (mView != null) {
        DrawListenerRegistry.unregisterListener(mView, this);
      }
      if (view != null) {
        DrawListenerRegistry.registerListener(view, this);
      }
    }
    mView = view;
    if (mView != null) {
      mOpacity = mView.getAlpha();
      draw();
    } else {
      mOpacity = 1;
      restore();
    }
  }

  public final void tearDown() {
    if (mView != null) {
      DrawListenerRegistry.unregisterListener(mView, this);
    }
  }

  Rect getBounds() {
    return get().copyBounds();
  }

  final void dispatchDraw() {
    if (isCustomDrawable()) draw();
  }

  private synchronized void draw() {
    mIsDrawing = true;
    Rect bounds = getBounds();
    Bitmap bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    draw(canvas, mView);
    Drawable outDrawable = createDrawable(mContext.getResources(), bitmap);
    outDrawable.setAlpha((int) (mOpacity * 255));
    outDrawable.setState(get().getState());
    outDrawable.setLevel(get().getLevel());
    set(outDrawable);
    invalidate();
    mIsDrawing = false;
  }

  final void restore() {
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

  public void updateFromProps(ReactStylesDiffMap props) {
    if (get() instanceof ReactDrawable) {
      ((ReactDrawable) get()).updateFromProps(props);
    } else if (props.hasKey("opacity")) {
      setOpacity((float) props.getDouble("opacity", mOpacity));
      invalidate();
    }
  }

  void setOpacity(@FloatRange(from = 0, to = 1) float opacity) {
    mOpacity = Math.max(Math.min(opacity, 1), 0);
    get().setAlpha((int) (mOpacity * 255));
  }
}