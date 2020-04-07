package com.reactnativecommunity.slider;

import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.uimanager.ReactStylesDiffMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.views.slider.ReactSliderManager;
import com.facebook.react.views.view.ReactViewGroup;
import com.facebook.react.views.view.ReactViewManager;

public class ReactInformantViewManager extends ReactViewManager {

  public static class InformantRegistry {

    public interface InformantTarget {
      void onReceiveProps(int recruiterID, View informant, ReactStylesDiffMap context);
      void onViewAdded(int recruiterID, ViewGroup parent, View view);
      void onViewRemoved(int recruiterID, ViewGroup parent, View view);
      void onViewInvalidated(int recruiterID, View view);
    }

    private SparseArray<InformantTarget> mInformantToReceiverRegistry = new SparseArray<>();
    private SparseIntArray mInformantToRecruiterRegistry = new SparseIntArray();

    private final ReactContext mContext;
    private boolean mInformDeep;

    InformantRegistry(ReactContext context, boolean informDeep) {
      mContext = context;
      mInformDeep = informDeep;
    }

    void add(final InformantTarget target, final int informantID, final boolean informDeep) {
      if (informDeep) {
        Runnable action = new Runnable() {
          @Override
          public void run() {
            try {
              View informant = mContext.getNativeModule(UIManagerModule.class).resolveView(informantID);
              add(target, informant, true);
            } catch (Throwable throwable) {
              Log.w(ReactSliderManager.REACT_CLASS, "unable to add informant", throwable);
            }
          }
        };
        if (UiThreadUtil.isOnUiThread()) {
          action.run();
        } else {
          UiThreadUtil.runOnUiThread(action);
        }
      } else {
        mInformantToReceiverRegistry.put(informantID, target);
        mInformantToRecruiterRegistry.put(informantID, informantID);
      }
    }

    synchronized void add(InformantTarget target, View informant, boolean informDeep) {
      traverseRegistration(target, informant, informant.getId(), informDeep);
    }

    synchronized void addSubInformant(View informant, View child, boolean informDeep) {
      InformantTarget target = mInformantToReceiverRegistry.get(informant.getId());
      int recruiterID = mInformantToRecruiterRegistry.get(informant.getId());
      if (target != null) {
        target.onViewAdded(recruiterID, (ViewGroup) informant, child);
        traverseRegistration(
            target,
            child,
            recruiterID,
            informDeep);
      }
    }

    synchronized void removeSubInformant(View informant, View child, boolean informDeep) {
      InformantTarget target = mInformantToReceiverRegistry.get(informant.getId());
      int recruiterID = mInformantToRecruiterRegistry.get(informant.getId());
      mInformantToReceiverRegistry.delete(child.getId());
      mInformantToRecruiterRegistry.delete(child.getId());
      if (target != null) {
        target.onViewRemoved(recruiterID, (ViewGroup) informant, child);
      }
      if (child instanceof ViewGroup && informDeep) {
        ViewGroup viewGroup = (ViewGroup) child;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
          removeSubInformant(informant, child, true);
        }
      }
    }

    private void traverseRegistration(InformantTarget target, View informant, int recruiterID, boolean informDeep) {
      mInformantToReceiverRegistry.put(informant.getId(), target);
      mInformantToRecruiterRegistry.put(informant.getId(), recruiterID);
      if (informant instanceof ViewGroup && informDeep) {
        ViewGroup viewGroup = (ViewGroup) informant;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
          traverseRegistration(target, viewGroup.getChildAt(i), recruiterID, true);
        }
      }
    }

    void remove(View informant) {
      mInformantToReceiverRegistry.delete(informant.getId());
      //removeSubInformant(mInformantToRecruiterRegistry.get(informant.getId()), informant, true);
    }

    void informReceivedProps(View informant, ReactStylesDiffMap context) {
      int informantID = informant.getId();
      InformantTarget target = mInformantToReceiverRegistry.get(informantID, null);
      int recruiterID = mInformantToRecruiterRegistry.get(informantID, -1);
      if (target != null) {
        target.onReceiveProps(recruiterID, informant, context);
      }
    }

    void informInvalidation(View informant) {
      int informantID = informant.getId();
      InformantTarget target = mInformantToReceiverRegistry.get(informantID, null);
      int recruiterID = mInformantToRecruiterRegistry.get(informantID, -1);
      if (target != null) {
        target.onViewInvalidated(recruiterID, informant);
      }
    }
  }

  private final InformantRegistry mInformantRegistry;

  ReactInformantViewManager(InformantRegistry registry) {
    super();
    mInformantRegistry = registry;
  }

  @Override
  public ReactViewGroup createViewInstance(ThemedReactContext context) {
    return new ReactViewGroup(context) {
      @Override
      public void invalidate() {
        super.invalidate();
        mInformantRegistry.informInvalidation(this);
      }
    };
  }

  @Override
  public void addView(ReactViewGroup parent, View child, int index) {
    super.addView(parent, child, index);
    mInformantRegistry.addSubInformant(parent, child, true);
  }

  @Override
  public void removeViewAt(ReactViewGroup parent, int index) {
    super.removeViewAt(parent, index);
    View child = getChildAt(parent, index);
    mInformantRegistry.removeSubInformant(parent, child, true);
  }

  @Override
  public void onDropViewInstance(@NonNull ReactViewGroup view) {
    super.onDropViewInstance(view);
    mInformantRegistry.remove(view);
    // // TODO: 07/04/2020 remove deep
  }

  @Override
  public void updateProperties(@NonNull ReactViewGroup viewToUpdate, ReactStylesDiffMap props) {
    super.updateProperties(viewToUpdate, props);
    mInformantRegistry.informReceivedProps(viewToUpdate, props);
  }

}
