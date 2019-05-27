package com.leombrosoft.demonhunter;

import android.content.Context;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by neku on 26/10/15.
 */
public class NonClickableToolbar extends Toolbar {

    public NonClickableToolbar(Context context) {
        super(context);
    }

    public NonClickableToolbar (Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NonClickableToolbar (Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }
}
