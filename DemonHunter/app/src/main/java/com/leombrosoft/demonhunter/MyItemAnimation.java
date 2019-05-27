package com.leombrosoft.demonhunter;

import android.content.Context;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class MyItemAnimation extends DefaultItemAnimator {
    Animation anim1;

    MyItemAnimation(Context context) {
        super();
        anim1 = AnimationUtils.loadAnimation(context, R.anim.fade_in_from_below);
    }

    @Override
    public boolean animateAdd(final RecyclerView.ViewHolder holder) {
        anim1.reset();
        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                dispatchAddFinished(holder);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
        anim1.setAnimationListener(listener);
        holder.itemView.startAnimation(anim1);
        return true;
    }
}