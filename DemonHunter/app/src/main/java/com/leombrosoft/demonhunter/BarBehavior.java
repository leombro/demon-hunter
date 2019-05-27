package com.leombrosoft.demonhunter;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.AttributeSet;
import android.view.View;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * Created by neku on 25/10/15.
 */
public class BarBehavior extends CoordinatorLayout.Behavior<MaterialProgressBar> {

    public BarBehavior(Context context, AttributeSet attrs) {}

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, MaterialProgressBar child, View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, MaterialProgressBar child, View dependency) {
        float transY = Math.min(0, dependency.getTranslationY() - dependency.getHeight());
        child.setTranslationY(transY);
        return true;
    }
}
