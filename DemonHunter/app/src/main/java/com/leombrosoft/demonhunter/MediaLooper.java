package com.leombrosoft.demonhunter;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

/**
 * Created by neku on 23/10/15.
 */
public class MediaLooper {

    private static final String TAG = MainActivity.TAG + "MediaLooper";
    private MediaPlayer mp1;
    private MediaPlayer mp2;
    private Context context;
    private int intro;
    private int loop;
    private int position = 0;

    private MediaLooper(Context context, int intro, int loop) {
        this.context = context;
        this.intro = intro;
        this.loop = loop;
        setup();
    }

    public static MediaLooper create(Context context, int intro, int loop) {
        Log.d(TAG,  "Create");
        return new MediaLooper(context, intro, loop);
    }

    public void setVolume(float left, float right) {
        Log.d(TAG,  "SetVolume");
        if (mp1 != null) mp1.setVolume(left, right);
        mp2.setVolume(left, right);
    }

    public boolean isPlaying() {
        Log.d(TAG,  "isPlaying");
        if (mp1 != null)
            return (mp1.isPlaying() || mp2.isPlaying());
        else
            return mp2.isPlaying();
    }

    public void pause() {
        Log.d(TAG,  "pause");
        if (mp1 != null) {
            mp1.pause();
            position = (0 - mp1.getCurrentPosition());
        } else {
            mp2.pause();
            position = mp2.getCurrentPosition();
        }
    }

    public void reset() {
        Log.d(TAG,  "reset and mp1 is " + mp1);
        if (mp1 != null) {
            if (mp1.isPlaying()) mp1.stop();
            mp1.reset();
            mp1.release();
            mp1 = null;
        }
        if (mp2 != null) {
            if (mp2.isPlaying()) mp2.stop();
            mp2.reset();
            mp2.release();
            mp2 = null;
        }
        setup();
        start();
    }

    public void start() {
        Log.d(TAG,  "start");
        if (position == 0) {
            if (mp1 != null) mp1.start();
            else mp2.start();
        } else if (position < 0 && mp1 != null) mp1.start();
        else mp2.start();
    }

    public void stop() {
        Log.d(TAG,  "stop");
        if (mp1 != null && mp1.isPlaying()) mp1.stop();
        else if (mp2.isPlaying()) mp2.stop();
    }

    public void release() {
        Log.d(TAG,  "release");
        if (mp1 != null)
            mp1.release();
        mp2.release();
    }

    private void setup() {
        position = 0;
        if (intro != -1) {
            mp1 = MediaPlayer.create(this.context, this.intro);
            mp1.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp1.release();
                    mp1 = null;
                }
            });
        } else mp1 = null;
        mp2 = MediaPlayer.create(context, loop);
        mp2.setLooping(true);
        if (mp1 != null) {
            mp1.setNextMediaPlayer(mp2);
        }
    }
}
