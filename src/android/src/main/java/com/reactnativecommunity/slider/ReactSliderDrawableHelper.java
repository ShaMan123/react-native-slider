package com.reactnativecommunity.slider;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.uimanager.MatrixMathHelper;
import com.facebook.react.uimanager.ReactTransformHelper;
import com.facebook.react.uimanager.ReactStylesDiffMap;
import com.facebook.react.uimanager.UIManagerModule;
import com.reactnativecommunity.slider.ReactInformantViewManager.InformantRegistry.InformantTarget;

public class ReactSliderDrawableHelper {

  private static final int MAX_LEVEL = 10000;

  static class ReactDrawable extends BitmapDrawable implements ReactTransformHelper.Transformable {
    private float mScaleX = 1;
    private float mScaleY = 1;
    private float mTranslationX = 0;
    private float mTranslationY = 0;
    private float mRotationX = 0;
    private float mRotationY = 0;
    private float mRotation = 0;
    private float mSkewX = 0;
    private float mSkewY = 0;
    private float mOpacity = 0;
    private Camera mRotator = new Camera();
    
    ReactDrawable(Resources res, Bitmap bitmap) {
      super(res, bitmap);
    }

    @Override
    protected boolean onStateChange(int[] state) {
      return true;
    }

    @Override
    protected boolean onLevelChange(int level) {
      return true;
    }

    void setTransform(ReadableArray transforms) {
      ReactTransformHelper.setTransform(this, transforms);
      invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
      Rect bounds = getBounds();
      PointF center = new PointF(bounds.centerX(), bounds.centerY());
      // apply translation
      canvas.translate(mTranslationX, mTranslationY);
      // apply skew
      canvas.skew(mSkewX, mSkewY);
      // apply 3D rotation
      mRotator.save();
      mRotator.translate(center.x, -center.y, 0);
      mRotator.rotate(mRotationX, mRotationY, mRotation);
      mRotator.translate(-center.x, center.y, 0);
      mRotator.applyToCanvas(canvas);
      mRotator.restore();
      // apply scale
      canvas.scale(mScaleX, mScaleY, center.x, center.y);
      super.draw(canvas);
    }

    public float getScaleX() {
      return mScaleX;
    }

    @Override
    public void setScaleX(float scaleX) {
      mScaleX = scaleX;
    }

    public float getScaleY() {
      return mScaleY;
    }

    @Override
    public void setScaleY(float scaleY) {
      mScaleY = scaleY;
    }
    
    public float getRotation() {
      return mRotation;
    }

    @Override
    public void setRotation(float rotation) {
      mRotation = rotation;
    }

    public float getRotationX() {
      return mRotationX;
    }

    @Override
    public void setRotationX(float rotationX) {
      mRotationX = rotationX;
    }

    public float getRotationY() {
      return mRotationY;
    }

    @Override
    public void setRotationY(float rotationY) {
      mRotationY = rotationY;
    }

    public float getReactOpacity() {
      return mOpacity;
    }

    public void setReactOpacity(float opacity) {
      mOpacity = Math.max(Math.min(opacity, 1), 0);
      setAlpha((int) (mOpacity * 255));
    }

    public float getTranslationX() {
      return mTranslationX;
    }

    @Override
    public void setTranslationX(float translationX) {
      mTranslationX = translationX;
    }

    public float getTranslationY() {
      return mTranslationY;
    }

    @Override
    public void setTranslationY(float translationY) {
      mTranslationY = translationY;
    }

    @Override
    public void setCameraDistance(float distance) {
      
    }

    public float getSkewX() {
      return mSkewX;
    }

    @Override
    public void setSkewX(float skewX) {
      this.mSkewX = skewX;
    }

    public float getSkewY() {
      return mSkewY;
    }

    @Override
    public void setSkewY(float skewY) {
      this.mSkewY = skewY;
    }
  }
  /*
  static class ReactDrawableGroup extends LayerDrawable {
    ReactDrawableGroup(View view) {
      if (view instanceof ViewGroup) {

      }
      super();
    }
  }

   */

  abstract static class DrawableHandler implements ViewTreeObserver.OnDrawListener, InformantTarget<ReactStylesDiffMap> {
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

    final View getView() {
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
          setAlpha((float) context.getDouble("opacity", mOpacity));
        }
        if (context.hasKey("transform") && get() instanceof ReactDrawable) {
          ((ReactDrawable) get()).setTransform(context.getArray("transform"));
        }
      }
    }

    final void setView(final int tag) {
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

    final void tearDown() {
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

    void setTintColor(Integer color) {
      if (color == null) {
        get().clearColorFilter();
      } else {
        get().setColorFilter(color, PorterDuff.Mode.SRC_IN);
      }
    }

    void updateFromProps(ReactStylesDiffMap props) {
      if (props.hasKey("opacity")) {
        setAlpha((float) props.getDouble("opacity", mOpacity));
      }
      if (props.hasKey("transform") && get() instanceof ReactDrawable) {
        ((ReactDrawable) get()).setTransform(props.getArray("transform"));
      }
    }

    void setAlpha(@FloatRange(from = 0, to = 1) float opacity) {
      mOpacity = Math.max(Math.min(opacity, 1), 0);
      get().setAlpha((int) (mOpacity * 255));
    }
  }

  private static class DrawListenerRegistry {
    static void registerListener(final View view, final ViewTreeObserver.OnDrawListener listener) {
      ViewTreeObserver treeObserver = view.getViewTreeObserver();
      try {
        treeObserver.addOnDrawListener(listener);
      } catch (Throwable throwable) {
        view.post(new Runnable() {
          @Override
          public void run() {
            registerListener(view, listener);
          }
        });
      }
    }

    static void unregisterListener(final View view, final ViewTreeObserver.OnDrawListener listener) {
      ViewTreeObserver treeObserver = view.getViewTreeObserver();
      try {
        treeObserver.removeOnDrawListener(listener);
      } catch (Throwable throwable) {
        view.post(new Runnable() {
          @Override
          public void run() {
            unregisterListener(view, listener);
          }
        });
      }
    }
  }

  static class ThumbDrawableHandler extends DrawableHandler {
    private ReactSlider mSlider;
    private final AnimatorSet mScaleAnimator;
    private float mScale = 1;
    private Paint mPaint = new Paint();
    private static final long ANIM_DURATION = 500;
    private static final long ANIM_DELAY = 200;

    public ThumbDrawableHandler(ReactSlider slider) {
      super((ReactContext) slider.getContext(), slider.getThumb());
      mSlider = slider;
      mScaleAnimator = new AnimatorSet();
      mScaleAnimator.setInterpolator(new LinearInterpolator());
      mScaleAnimator.setDuration(ANIM_DURATION);
      mScaleAnimator.setStartDelay(ANIM_DELAY);
    }

    @Override
    public Drawable get() {
      return mSlider.getThumb();
    }

    @Override
    public void set(Drawable drawable) {
      mSlider.setThumb(drawable);
      get().jumpToCurrentState();
    }

    @Override
    void setTintColor(Integer color) {
      super.setTintColor(color);
      if (color == null) {
        mPaint.setColor(Color.TRANSPARENT);
      } else {
        mPaint.setColor(color);
      }
    }

    @Override
    Rect getBounds() {
      if (getView() != null) {
        return new Rect(0, 0, (int) (getView().getWidth() * mScale), (int) (getView().getHeight() * mScale));
      } else {
        return super.getBounds();
      }
    }

    @Override
    public void draw(Canvas canvas, View view) {
      RectF bounds = new RectF(getBounds());
      RectF src = new RectF(0, 0, view.getWidth(), view.getHeight());
      PointF scale = new PointF(bounds.width() / src.width(),bounds.height() / src.height());
      float scaleOut = Math.min(scale.x, scale.y);

      /*
        reverse scaleX due to {@link ReactSliderManager#setInverted(ReactSlider, boolean)}
       */
      PointF scaler = new PointF(scaleOut * (mSlider.isInverted() ? -1 : 1), scaleOut);
      // clip circle
      Path clipper = new Path();
      clipper.addCircle(
          canvas.getWidth() / 2,
          canvas.getHeight() / 2,
          Math.min(canvas.getWidth(), canvas.getHeight()) / 2,
          Path.Direction.CW);
      canvas.clipPath(clipper);
      // transform
      canvas.scale(mScale, mScale, canvas.getWidth() / 2, canvas.getHeight() / 2);
      canvas.translate(
          (bounds.width() - src.width() * scaler.x) / 2,
          (bounds.height() - src.height() * scaler.y) / 2);
      canvas.scale(scaler.x, scaler.y);
      // draw
      canvas.drawPaint(mPaint);
      view.draw(canvas);
    }

    /**
     * used by {@link ThumbDrawableHandler#mScaleAnimator}
     * @return
     */
    @SuppressWarnings("unused")
    public float getScale() {
      return mScale;
    }

    /**
     * used by {@link ThumbDrawableHandler#mScaleAnimator}
     * @param scale
     */
    @SuppressWarnings("unused")
    public void setScale(float scale) {
      mScale = scale;
      dispatchDraw();
    }

    void start() {
      if (isCustomDrawable()) {
        animate(1.2f);
      }
    }

    void end() {
      if (isCustomDrawable()) {
        animate(1);
      }
    }

    private void animate(float scaler) {
      if (mScaleAnimator.isRunning()) {
        mScaleAnimator.cancel();
      }
      if (get() instanceof ReactDrawable) {
        ReactDrawable drawable = (ReactDrawable) get();
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(
            drawable,
            Property.of(ReactDrawable.class, Float.class, "scaleX"),
            scaler * drawable.mScaleX);
        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(
            drawable,
            Property.of(ReactDrawable.class, Float.class, "scaleY"),
            scaler * drawable.mScaleY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
          scaleXAnim.setAutoCancel(true);
          scaleYAnim.setAutoCancel(true);
        }
        mScaleAnimator.playTogether(scaleXAnim, scaleYAnim);
        mScaleAnimator.start();
      }
    }

    void onTouchEvent(MotionEvent event) {
      int action = event.getActionMasked();
      if (isCustomDrawable()) {
        if (action == MotionEvent.ACTION_DOWN) {
          start();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
          end();
        }
      }
    }
  }

  private static class ProgressDrawableHandler extends DrawableHandler {
    final ReactSlider mSlider;
    private final int mLayerID;
    private Integer mColor;

    static Drawable getDrawable(ReactSlider slider, int layerID) {
      LayerDrawable drawable = (LayerDrawable) slider.getProgressDrawable().getCurrent();
      return drawable.findDrawableByLayerId(layerID);
    }

    public ProgressDrawableHandler(ReactSlider slider, int layerID) {
      super((ReactContext) slider.getContext(), getDrawable(slider, layerID));
      mSlider = slider;
      mLayerID = layerID;
    }

    @Override
    public Drawable get() {
      return getDrawable(mSlider, mLayerID);
    }

    @Override
    public void set(Drawable drawable) {
      LayerDrawable outDrawable = (LayerDrawable) mSlider.getProgressDrawable().getCurrent();
      outDrawable.setDrawableByLayerId(mLayerID, drawable);
      outDrawable.setState(new int[]{});
      outDrawable.jumpToCurrentState();
      outDrawable.setState(new int[]{android.R.attr.state_enabled});
      outDrawable.jumpToCurrentState();
    }

    int getBarHeight() {
      return mSlider.getIndeterminateDrawable().getIntrinsicHeight();
    }

    @Override
    Rect getBounds() {
      return new Rect(0, 0, mSlider.getWidth(), mSlider.getHeight());
    }

    @Override
    public void draw(Canvas canvas, View view) {
      RectF bounds = new RectF(getBounds());
      RectF src = new RectF(0, 0, view.getWidth(), view.getHeight());
      float barHeight = Math.max(Math.min(bounds.height(), src.height()), getBarHeight());
      PointF scale = new PointF(bounds.width() / src.width(),barHeight / src.height());
      canvas.translate(0, (bounds.height() - barHeight) / 2);
      canvas.scale(scale.x, scale.y);
      view.draw(canvas);
    }
  }

  static class ForegroundDrawableHandler extends ProgressDrawableHandler {
    ForegroundDrawableHandler(ReactSlider slider) {
      super(slider, android.R.id.progress);
    }

    @Override
    Drawable createDrawable(Resources res, Bitmap bitmap) {
      return new UpdatingBitmapDrawable(res, bitmap, get().getLevel());
    }

    private class UpdatingBitmapDrawable extends ReactDrawable {
      UpdatingBitmapDrawable(Resources res, Bitmap bitmap, int level) {
        super(res, bitmap);
        setLevel(level);
      }

      @Override
      protected boolean onLevelChange(int level) {
        return true;
      }

      @Override
      public void draw(Canvas canvas) {
        float levelScale = getLevel() * 1.f / MAX_LEVEL * 1.f;
        /*
          @deprecated now handled by {@link ReactSliderManager#setInverted(ReactSlider, boolean)}

        boolean inverted = mSlider.isInverted();
        Rect bounds = getBounds();
        if (inverted) {
          levelScale = 1 - levelScale;
          canvas.translate(bounds.width() * (1 - levelScale), 0);
        }
         */
        canvas.scale(levelScale, 1);
        super.draw(canvas);
      }
    }
  }

  static class BackgroundDrawableHandler extends ProgressDrawableHandler {
    BackgroundDrawableHandler(ReactSlider slider) {
      super(slider, android.R.id.background);
    }
  }

}
