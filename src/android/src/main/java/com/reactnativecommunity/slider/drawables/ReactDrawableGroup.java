package com.reactnativecommunity.slider.drawables;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.uimanager.ReactStylesDiffMap;
import com.facebook.react.views.view.ReactViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReactDrawableGroup extends ReactDrawable {

  static class ReactRootDrawableGroup extends ReactDrawableGroup implements PropsUpdater {

    private final SparseArray<ReactDrawableGroup> mRegistry;

    ReactRootDrawableGroup(Builder builder) {
      super(builder);
      mRegistry = new SparseArray<>();
      traverseRegistration(this);
      setBounds(getBounds());
    }

    void traverseRegistration(ReactDrawableGroup reactDrawable) {
      mRegistry.put(reactDrawable.mID.getId(), reactDrawable);
      for (Map.Entry<View, ReactDrawableGroup> next : reactDrawable.mDrawables.entrySet()) {
        traverseRegistration(next.getValue());
      }
    }

    @Override
    public void updateFromProps(int tag, ReactStylesDiffMap props) {
      ReactDrawableGroup d = mRegistry.get(tag);
      if (d != null) d.updateFromProps(props);
    }

    public void transformBounds(Rect bounds) {}

    @Override
    protected void onBoundsChange(Rect bounds) {
      Rect v = new Rect();
      mID.getDrawingRect(v);
      transformBounds(bounds);
      PointF scale = new PointF(bounds.width() * 1f / v.width(), bounds.height() * 1f / v.height());
      traverseLayout(mID, scale);
    }

    void traverseLayout(View view, PointF scale) {
      boolean firstTraversal = view == mID;
      Drawable drawable = mRegistry.get(view.getId());
      if (drawable == null) return;

      Rect out = new Rect();
      view.getDrawingRect(out);
      RectF local = new RectF(0, 0, out.width() * scale.x, out.height() * scale.y);
      if (mID instanceof ViewGroup && !firstTraversal) {
        ((ViewGroup) mID).offsetDescendantRectToMyCoords(view, out);
      }
      local.offsetTo(out.left * scale.x, out.top * scale.y);
      local.round(out);

      if (firstTraversal) {
        super.onBoundsChange(out);
      } else {
        drawable.setBounds(out);
      }

      if (view instanceof ViewGroup) {
        ViewGroup viewGroup = ((ViewGroup) view);
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
          traverseLayout(viewGroup.getChildAt(i), scale);
        }
      }
    }

    void addView(ViewGroup parent, View view) {
      ReactDrawableGroup parentDrawable = mRegistry.get(parent.getId());
      if (parentDrawable != null) {
        mRegistry.put(view.getId(), parentDrawable.addView(view));
        layoutDrawableGroup(parentDrawable);
      }
    }

    void removeView(ViewGroup parent, View view) {
      ReactDrawableGroup parentDrawable = mRegistry.get(parent.getId());
      if (parentDrawable != null) {
        parentDrawable.removeView(view);
        layoutDrawableGroup(parentDrawable);
      }
      mRegistry.remove(view.getId());
    }

    void draw(View view) {
      traverseDrawing(view, true);
    }
/*
    void draw(View view) {
      draw(view, true);
    }

    void draw(View view, boolean layout) {
      traverseDrawing(view);
      if (layout) onBoundsChange(getBounds());
      invalidateSelf();
    }

 */

    private void traverseDrawing(View view, boolean layout) {
      if (view instanceof ViewGroup) {
        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
          traverseDrawing(viewGroup.getChildAt(i), false);
        }
      }
      ReactDrawableGroup d = mRegistry.get(view.getId());
      if (d != null) {
        d.setBase(view);
        if (layout) layoutDrawableGroup(d);
      }
    }

    private void layoutDrawableGroup(ReactDrawableGroup reactDrawableGroup) {
      onBoundsChange(getBounds());
      reactDrawableGroup.invalidateSelf();
    }

    private ReactDrawableGroup getParent(View view) {
      if (view.getParent() != null) {
        int parentID = ((View) view.getParent()).getId();
        return mRegistry.get(parentID);
      } else {
        return null;
      }
    }

  }

  private HashMap<View, ReactDrawableGroup> mDrawables;
  View mID;
  @Nullable
  private Drawable mBaseDrawable;

  ReactDrawableGroup(Builder builder) {
    super(builder.getLayers(), builder.view);
    mID = builder.view;
    mBaseDrawable = builder.base;
    mDrawables = builder.children;
    if (mBaseDrawable != null) {
      mBaseDrawable.setCallback(this);
    }
    for (Map.Entry<View, ReactDrawableGroup> dr : mDrawables.entrySet()) {
      dr.getValue().setCallback(this);
    }
  }
/*
  @Override
  protected boolean onLevelChange(int level) {
    boolean changed = false;
    if (mBaseDrawable != null) {
      changed = mBaseDrawable.setLevel(level);
    }
    for (Map.Entry<View, ReactDrawableGroup> dr : mDrawables.entrySet()) {
      if (dr.getValue().setLevel(level)) {
        changed = true;
      }
    }
    return changed;
  }

  @Override
  public void jumpToCurrentState() {
    if (mBaseDrawable != null) {
      mBaseDrawable.jumpToCurrentState();
    }
    for (Map.Entry<View, ReactDrawableGroup> dr : mDrawables.entrySet()) {
      dr.getValue().jumpToCurrentState();
    }
  }

  @Override
  protected boolean onStateChange(int[] state) {
    boolean changed = false;
    if (mBaseDrawable != null) {
      changed = mBaseDrawable.setState(state);
    }
    for (Map.Entry<View, ReactDrawableGroup> dr : mDrawables.entrySet()) {
      if (dr.getValue().setState(state)) {
        changed = true;
      }
    }
    return changed;
  }

 */

  @Override
  protected void onBoundsChange(Rect bounds) {
    if (mBaseDrawable != null) {
      mBaseDrawable.setBounds(bounds);
    }
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    canvas.save();
    onPreDraw(canvas);
    drawBackground(canvas);
    drawChildren(canvas);
    canvas.restore();
  }

  private void drawBackground(Canvas canvas) {
    canvas.save();
    if (mBaseDrawable != null) {
      mBaseDrawable.draw(canvas);
    }
    canvas.restore();
  }

  private void drawChildren(Canvas canvas) {
    if (mID instanceof ViewGroup) {
      ViewGroup viewGroup = ((ViewGroup) mID);
      for (int i = 0; i < viewGroup.getChildCount(); i++) {
        canvas.save();
        mDrawables.get(Builder.getChildInDrawingOrderAtIndex(viewGroup, i)).draw(canvas);
        canvas.restore();
      }
    }
  }

  private void setBase(View view) {
    mID = view;
    mBaseDrawable = Builder.createBaseDrawable(view.getResources(), view);
    /*
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      setDrawable(0, mBaseDrawable);
    } else {
      // needs layout
    }

     */
  }

  private ReactDrawableGroup addView(View view) {
    ReactDrawableGroup target = new Builder(view.getResources(), view).get(false);
    mDrawables.put(view, target);
    //int index = ((ViewGroup) mID).indexOfChild(view);
/*
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      setDrawable(index, target);
    } else {
      // needs layout
    }

 */

    return target;
  }

  private void removeView(View view) {
    ReactDrawableGroup target = mDrawables.remove(view);
    //int index = ((ViewGroup) mID).indexOfChild(view);
/*
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      setDrawable(index, target);
    } else {
      target.setVisible(false, false);
    }


 */

  }

  static class Builder {

    View view;
    @Nullable Drawable base;
    HashMap<View, ReactDrawableGroup> children;
    DrawableHandler handler;

    Builder(DrawableHandler handler) {
      this(handler.getResources(), handler.getView());
      this.handler = handler;
    }

    Builder(Resources res, View view) {
      this.view = view;
      base = createBaseDrawable(res, view);
      children = traverseChildren(res, view);
    }

    Drawable[] getLayers() {
      ArrayList<Drawable> layers = new ArrayList<>();
      if (base != null) {
        layers.add(base);
      }
      for (Map.Entry<View, ReactDrawableGroup> next : children.entrySet()) {
        layers.add(next.getValue());
      }
      return layers.toArray(new Drawable[0]);
    }

    private ReactDrawableGroup get(boolean isRoot) {
      if (isRoot) {
        return new ReactRootDrawableGroup(this);
      } else {
        return new ReactDrawableGroup(this);
      }
    }

    private static HashMap<View, ReactDrawableGroup> traverseChildren(Resources res, View view) {
      HashMap<View, ReactDrawableGroup> drawables = new HashMap<>();
      if (view instanceof ViewGroup) {
        ViewGroup viewGroup = (ViewGroup) view;
        View child;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
          child = viewGroup.getChildAt(i);
          drawables.put(child, new Builder(res, child).get(false));
        }
      }
      return drawables;
    }
    
    private static View getChildInDrawingOrderAtIndex(ViewGroup parent, int index) {
      if (parent instanceof ReactViewGroup) {
        return parent.getChildAt(((ReactViewGroup) parent).getZIndexMappedChildIndex(index));
      }
      return parent.getChildAt(index);
    }

    private static Drawable createBaseDrawable(Resources res, View view) {
      Rect src = new Rect();
      view.getDrawingRect(src);
      if (src.width() == 0 || src.height() == 0) {
        return null;
      } else {
        Bitmap bitmap = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawBareView(canvas, view);
        return new BitmapDrawable(res, bitmap);
      }
    }

    private static void drawBareView(Canvas canvas, View view) {
      if (view instanceof ViewGroup) {
        ArrayList<Integer> visibility = new ArrayList<>();
        View child;
        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
          child = viewGroup.getChildAt(i);
          visibility.add(child.getVisibility());
          child.setVisibility(View.INVISIBLE);
        }
        view.draw(canvas);
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
          child = viewGroup.getChildAt(i);
          child.setVisibility(visibility.get(i));
        }
      } else {
        view.draw(canvas);
      }
    }

  }

}
