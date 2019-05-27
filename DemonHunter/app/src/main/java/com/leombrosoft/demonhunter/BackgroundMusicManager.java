package com.leombrosoft.demonhunter;

import android.content.Context;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;

/**
 * Created by neku on 23/10/15.
 */
public class BackgroundMusicManager {
    private static final String TAG = MainActivity.TAG + "/MusicMngr";
    public static final int MUSIC_PREVIOUS = -1;
    public static final int MUSIC_WHEN_MOONS_REACHING_OUT_STARS = 0;
    public static final int MUSIC_BACKSIDE_OF_THE_TV = 1;
    public static final int MUSIC_THEME_OF_JUNES = 2;
    public static final int MUSIC_ILL_FACE_MYSELF_BATTLE = 3;
    public static final int MUSIC_SCHOOL_DAYS = 4;
    public static final int MUSIC_VELVET_ROOM = 5;

    private static HashMap<Integer, MediaLooper> players = new HashMap<>();
    private static int currentMusic = -1;
    private static int previousMusic = -1;


    public static void start(Context context, int music) {
        start(context, music, false);
    }

    public static void start(Context context, int music, boolean retainMusic) {
        if (retainMusic) {
            MediaLooper ml = players.get(music);
            if (ml != null) {
                Log.d(TAG, "Retaining music");
                if (!ml.isPlaying()) ml.start();
                return;
            } else {
                Log.d(TAG, "Could not retain music: normal start");
            }
        }
        if (music == MUSIC_PREVIOUS) {
            MediaLooper ml = players.get(music);
            if (ml != null) {
                Log.d(TAG, "Requested previous music");
                if (!ml.isPlaying()) ml.start();
                return;
            }
        }
        if (currentMusic != -1) {
            pause();
        }
        currentMusic = music;
        Log.d(TAG, "Playing music " + music + ", previous is " + previousMusic);
        MediaLooper ml = players.get(music);
        if (ml != null) {
            if (currentMusic != previousMusic)
                ml.reset();
            else
                if (!ml.isPlaying()) ml.start();
            previousMusic = currentMusic;
        }
        else {
            switch (music) {
                case MUSIC_WHEN_MOONS_REACHING_OUT_STARS:
                    ml = MediaLooper.create(context, R.raw.moon_intro, R.raw.moon_loop);
                    break;
                case MUSIC_BACKSIDE_OF_THE_TV:
                    ml = MediaLooper.create(context, R.raw.backside_intro, R.raw.backside_loop);
                    break;
                case MUSIC_THEME_OF_JUNES:
                    ml = MediaLooper.create(context, -1, R.raw.junes);
                    break;
                case MUSIC_ILL_FACE_MYSELF_BATTLE:
                    ml = MediaLooper.create(context, -1, R.raw.myself);
                    break;
                case MUSIC_SCHOOL_DAYS:
                    ml = MediaLooper.create(context, R.raw.school_intro, R.raw.school_loop);
                    break;
                case MUSIC_VELVET_ROOM:
                    ml = MediaLooper.create(context, -1, R.raw.poem);
                    break;
                default:
                    Log.d(TAG, "Unknown song with id " + music);
                    return;
            }
            players.put(music, ml);
            ml.setVolume(100, 100);
            ml.start();
        }
    }

    /*public static void start(Context context, int music, boolean forceRecreation) {
        if (music == MUSIC_PREVIOUS) {
            Log.d(TAG, "Using previous music [" + previousMusic + "]");
            music = previousMusic;
        }
        if (!forceRecreation && currentMusic == music) {
            // already playing this music
            return;
        }
        if (currentMusic != -1) {
            previousMusic = currentMusic;
            Log.d(TAG, "Previous music was [" + previousMusic + "]");
            // playing some other music, pause it and change
            pause();
        }
        currentMusic = music;
        Log.d(TAG, "Current music is now [" + currentMusic + "]");
        MediaLooper ml = players.get(music);
        if (ml != null) {
            if (forceRecreation) ml.reset();
            if (!ml.isPlaying()) {
                ml.start();
            }
        } else {
            switch (music) {
                case MUSIC_WHEN_MOONS_REACHING_OUT_STARS:
                    ml = MediaLooper.create(context, R.raw.moon_intro, R.raw.moon_loop);
                    break;
                case MUSIC_BACKSIDE_OF_THE_TV:
                    ml = MediaLooper.create(context, R.raw.backside_intro, R.raw.backside_loop);
                    break;
                case MUSIC_THEME_OF_JUNES:
                    ml = MediaLooper.create(context, -1, R.raw.junes);
                    break;
                default:
                    Log.e(TAG, "unsupported music number - " + music);
                    return;
            }
            players.put(music, ml);
            float volume = 100f;
            Log.d(TAG, "Setting music volume to " + volume);
            ml.setVolume(volume, volume);
            try {
                ml.start();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }*/

    public static void pause() {
        Collection<MediaLooper> mls = players.values();
        for (MediaLooper l : mls) {
            if (l.isPlaying()) {
                l.pause();
            }
        }
// previousMusic should always be something valid
        if (currentMusic != -1) {
            previousMusic = currentMusic;
            Log.d(TAG, "Previous music was [" + previousMusic + "]");
        }
        currentMusic = -1;
        Log.d(TAG, "Current music is now [" + currentMusic + "]");
    }

    public static void release() {
        Log.d(TAG, "Releasing media players");
        Collection<MediaLooper> mls = players.values();
        for (MediaLooper ml : mls) {
            try {
                if (ml != null) {
                    if (ml.isPlaying()) {
                        ml.stop();
                    }
                    ml.release();
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        mls.clear();
        if (currentMusic != -1) {
            previousMusic = currentMusic;
            Log.d(TAG, "Previous music was [" + previousMusic + "]");
        }
        currentMusic = -1;
        Log.d(TAG, "Current music is now [" + currentMusic + "]");
    }
}
