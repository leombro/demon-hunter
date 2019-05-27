package com.leombrosoft.demonhunter;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.SharedElementCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Image;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import io.codetail.animation.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.codetail.animation.SupportAnimator;

/**
 * Created by neku on 23/10/15.
 */
public class FindDemonActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.TAG + "/FindDemon";

    private static final int NEGOTIATION = 1;

    private AlertDialog.Builder alertDialogBuilder;
    private DatabaseManager dbm;
    private Demon demon;
    private LatLng position;
    private ArrayList<Drawable> images;
    private ArrayList<Drawable> reversed;
    private int currentPos;
    private int subHeight = 0;
    private boolean continueMusic = false;
    private boolean sound_loaded_focus = false;
    private boolean sound_loaded_scan = false;
    private boolean sound_loaded_nacs = false;
    private int soundFocus = -1;
    private int soundScan = -1;
    private int soundNacs = -1;
    private int state;
    private SoundPool sp;
    private ImageView streetview;
    private ImageView negview;
    private int demonScreen;
    private Point demonPos;
    private ImageView demonImg;
    private int searchTimes = 0;
    private NfcAdapter nfca;

    private static final int NORMAL = 0;
    private static final int FOCUSED = 1;
    private static final int TRANSITION = 2;

    private static final int NORTH = 0;
    private static final int EAST = 1;
    private static final int SOUTH = 2;
    private static final int WEST = 3;

    private GestureDetectorCompat mDetector;

    @Override
    protected void onPause() {
        super.onPause();

        if (nfca != null)
            nfca.disableForegroundDispatch(this);

        if (!continueMusic)
            BackgroundMusicManager.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (nfca != null) {
            Intent intent = new Intent(this, getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pint = PendingIntent.getActivity(this, 0, intent, 0);
            nfca.enableForegroundDispatch(this, pint, null, null);
        }

        continueMusic = false;
        BackgroundMusicManager.start(this, BackgroundMusicManager.MUSIC_BACKSIDE_OF_THE_TV);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())){
            return;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                continueMusic = false;
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_demon);

        nfca = NfcAdapter.getDefaultAdapter(this);
        if (nfca != null) {
            nfca.setNdefPushMessage(null, this);
        }

        dbm = DatabaseManager.get(this);
        reversed = new ArrayList<>();

        mDetector = new GestureDetectorCompat(this, new SwipeDetector());
        alertDialogBuilder = new AlertDialog.Builder(this).setCancelable(false);

        Intent starter = getIntent();
        final int demonID = starter.getIntExtra("DEMON", -1);
        double latitude = starter.getDoubleExtra("LAT", -1);
        double longitude = starter.getDoubleExtra("LNG", -1);
        final double distance = starter.getDoubleExtra("DISTANCE", -1);
        position = new LatLng(latitude, longitude);

        final DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        final int DISPLAY_WIDTH = metrics.widthPixels;
        final int DISPLAY_HEIGHT = metrics.heightPixels;
        final int sbheight = getStatusBarHeight(getResources());

        int imgW = DISPLAY_WIDTH, imgH = DISPLAY_HEIGHT - sbheight;
        int divisor = 2;

        while (imgW > 640 || imgH > 640) {
            imgW = DISPLAY_WIDTH / divisor;
            imgH = (DISPLAY_HEIGHT - sbheight) / divisor;
            divisor++;
        }

        if (Build.VERSION.SDK_INT >= 21) {
            SoundPool.Builder spb = new SoundPool.Builder();
            spb.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            spb.setMaxStreams(3);
            sp = spb.build();
        } else {
            sp = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        }

        sp.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                if (sampleId == soundScan) {
                    sound_loaded_scan = true;
                    Log.d(TAG, "loaded scan");
                } else if (sampleId == soundFocus) {
                    sound_loaded_focus = true;
                    Log.d(TAG, "loaded focus");
                } else if (sampleId == soundNacs) {
                    sound_loaded_nacs = true;
                } else Log.d(TAG, "invalid id");
            }
        });

        soundScan = sp.load(FindDemonActivity.this, R.raw.scan, 1);
        soundFocus = sp.load(FindDemonActivity.this, R.raw.focus, 2);
        soundNacs = sp.load(FindDemonActivity.this, R.raw.nacs, 1);


        if (checkIfInternetConnection()) {
            Random r = new Random();
            Log.d(TAG, "Dimensions are w=" + imgW + " h= " + imgH);

            demon = dbm.getDemon(demonID);
            demonImg = (ImageView)findViewById(R.id.monster);
            Bitmap b = BitmapFactory.decodeResource(getResources(), demon.getImage());
            demonImg.setImageDrawable(new BitmapDrawable(getResources(), b));
            demonImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(FindDemonActivity.this, DemonNegotiation.class);
                    intent.putExtra("DEMON", demonID);
                    intent.putExtra("DIST", distance);
                    ActivityOptionsCompat options = ActivityOptionsCompat
                            .makeSceneTransitionAnimation(FindDemonActivity.this, v, "demon");
                    ActivityCompat.startActivityForResult(FindDemonActivity.this, intent, NEGOTIATION, options.toBundle());
                }
            });

            demonScreen = r.nextInt(4);
            Log.d(TAG, "demonImg is " + demonImg.getMeasuredState());
            Log.d(TAG, "Imgw=" + imgW + ", ImgH=" + imgH + ", demonSize=(" + demonImg.getWidth() + "," + demonImg.getHeight() + ")");
            int demonX = r.nextInt(imgW - demonImg.getWidth()), demonY = r.nextInt(imgH - demonImg.getHeight());
            demonPos = new Point(demonX, demonY);
            String n =
                    String.format("https://maps.googleapis.com/maps/api/streetview" +
                                    "?size=%dx%d&location=%f,%f&fov=90&heading=%d&pitch=0&key=%s",
                            imgW, imgH, latitude, longitude, 0, getString(R.string.google_maps_key)),
                    e = String.format("https://maps.googleapis.com/maps/api/streetview" +
                                    "?size=%dx%d&location=%f,%f&fov=90&heading=%d&pitch=0&key=%s",
                            imgW, imgH, latitude, longitude, 90, getString(R.string.google_maps_key)),
                    s = String.format("https://maps.googleapis.com/maps/api/streetview" +
                                    "?size=%dx%d&location=%f,%f&fov=90&heading=%d&pitch=0&key=%s",
                            imgW, imgH, latitude, longitude, 180, getString(R.string.google_maps_key)),
                    w = String.format("https://maps.googleapis.com/maps/api/streetview" +
                                    "?size=%dx%d&location=%f,%f&fov=90&heading=%d&pitch=0&key=%s",
                            imgW, imgH, latitude, longitude, 270, getString(R.string.google_maps_key));
            URL u1, u2, u3, u4;
            try {
                u1 = new URL(n);
                u2 = new URL(e);
                u3 = new URL(s);
                u4 = new URL(w);
            } catch (MalformedURLException e1) {
                Log.d(TAG, "MalformedUrl");
                throw new RuntimeException(e1);
            }
            new GetImages().execute(u1, u2, u3, u4);
        } else {
            DialogInterface.OnClickListener ocl = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        Intent intent = new Intent(Settings.ACTION_SETTINGS);
                        startActivity(intent);
                    }
                    setResult(RESULT_CANCELED);
                    finish();
                }
            };
            alertDialogBuilder.setMessage(R.string.internet_not_found)
                    .setPositiveButton(android.R.string.ok, ocl)
                    .setNegativeButton(android.R.string.cancel, ocl)
                    .show();

        }

        streetview = (ImageView)findViewById(R.id.streetview);
        negview = (ImageView)findViewById(R.id.negativeview);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case NEGOTIATION:
                setResult(RESULT_FIRST_USER);
                finish();
                break;
            default:
                break;
        }
    }

    public static int getStatusBarHeight(Resources res) {
        int result = 0;
        int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = res.getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private class GetImages extends AsyncTask<URL, Integer, ArrayList<Drawable>> {

        @Override
        protected ArrayList<Drawable> doInBackground(URL... params) {
            ArrayList<Drawable> results = new ArrayList<>();
            int count = params.length;
            float[] neg = { -1.0f, 0, 0, 0, 255, //red
                    0, -1.0f, 0, 0, 255, //green
                    0, 0, -1.0f, 0, 255, //blue
                    0, 0, 0, 1.0f, 0 //alpha
            };
            ColorFilter negative = new ColorMatrixColorFilter(neg);
            for (int i = 0; i < count; i++) {
                try {
                    Bitmap b = BitmapFactory.decodeStream((InputStream) params[i].getContent());
                    Drawable d = new BitmapDrawable(getResources(), b), d1 = new BitmapDrawable(getResources(), b);
                    results.add(d);
                    d1.setColorFilter(negative);
                    reversed.add(d1);
                    Log.d(TAG, "Downloaded from " + params[i]);
                    Log.d(TAG, "Bitmap " + i + " of size " + b.getWidth() + "," + b.getHeight());
                    Log.d(TAG, "Drawable " + i + " of size " + d.getIntrinsicWidth() + "," + d.getIntrinsicHeight());
                } catch (IOException e) {
                    Log.d(TAG, "Could not download from " + params[i]);
                }
                if (isCancelled()) break;
            }
            return results;
        }

        @Override
        protected void onPostExecute(ArrayList<Drawable> drawables) {
            super.onPostExecute(drawables);
            (findViewById(R.id.progress_find)).setVisibility(View.GONE);
            (findViewById(R.id.lay_streetview)).setVisibility(View.VISIBLE);
            images = drawables;
            Log.d(TAG, "Generated array with size " + images.size() + " and elements " + images);
            onCompletedSetup();
        }
    }

    @Override
    protected void onDestroy() {
        sp.release();
        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private void onCompletedSetup() {
        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.FABStreet);
        streetview.setImageDrawable(images.get(NORTH));
        negview.setImageDrawable(reversed.get(NORTH));
        currentPos = NORTH;
        state = NORMAL;

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state == NORMAL) {
                    searchTimes++;
                    int centerX = (v.getLeft() + v.getRight()) / 2;
                    int centerY = (v.getTop() + v.getBottom()) / 2;

                    int startRadius = 0;
                    // get the final radius for the clipping circle
                    int endRadius = (int) Math.hypot(streetview.getWidth(), streetview.getHeight());
                    SupportAnimator anim =
                            ViewAnimationUtils.createCircularReveal(findViewById(R.id.toShow), centerX, centerY, startRadius, endRadius);
                    anim.setInterpolator(new AccelerateDecelerateInterpolator());
                    anim.setDuration(2000);
                    if (demonScreen == currentPos) {
                        Animation anim2 = AnimationUtils.loadAnimation(FindDemonActivity.this, R.anim.wobbling);
                        demonImg.setX(demonPos.x);
                        demonImg.setY(demonPos.y);
                        demonImg.setAnimation(anim2);
                        demonImg.setVisibility(View.VISIBLE);
                    }
                    negview.setVisibility(View.VISIBLE);
                    if (sound_loaded_focus) sp.play(soundFocus, 0.8f, 0.8f, 1, 0, 1);
                    if (sound_loaded_scan) sp.play(soundScan, 1, 1, 1, 0, 1);
                    state = TRANSITION;
                    anim.addListener(new SupportAnimator.AnimatorListener() {
                        @Override
                        public void onAnimationStart() {
                        }

                        @Override
                        public void onAnimationEnd() {
                            if (searchTimes == 1) {
                                Snackbar
                                        .make(findViewById(R.id.lay_streetview), R.string.alert, Snackbar.LENGTH_LONG)
                                        .show();
                            }
                            state = FOCUSED;
                        }

                        @Override
                        public void onAnimationCancel() {
                            if (searchTimes == 1) {
                                Snackbar
                                        .make(findViewById(R.id.lay_streetview), R.string.alert, Snackbar.LENGTH_LONG)
                                        .show();
                            }
                            state = FOCUSED;
                        }

                        @Override
                        public void onAnimationRepeat() {
                        }
                    });
                    anim.start();
                } else if (state == FOCUSED) {
                    int centerX = (v.getLeft() + v.getRight()) / 2;
                    int centerY = (v.getTop() + v.getBottom()) / 2;

                    int startRadius = 0;
                    // get the final radius for the clipping circle
                    int endRadius = (int) Math.hypot(streetview.getWidth(), streetview.getHeight());
                    SupportAnimator anim =
                            ViewAnimationUtils.createCircularReveal(findViewById(R.id.toShow), centerX, centerY, startRadius, endRadius);
                    anim = anim.reverse();
                    anim.setInterpolator(new AccelerateDecelerateInterpolator());
                    anim.setDuration(1100);
                    anim.addListener(new SupportAnimator.AnimatorListener() {
                        @Override
                        public void onAnimationStart() {
                        }

                        @Override
                        public void onAnimationEnd() {
                            negview.setVisibility(View.INVISIBLE);
                            demonImg.setVisibility(View.INVISIBLE);
                            demonImg.clearAnimation();
                            state = NORMAL;
                            if (searchTimes == 2) {
                                String text = String.format(getString(R.string.escape), demon.getName());
                                alertDialogBuilder.setMessage(text)
                                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                setResult(RESULT_FIRST_USER);
                                                finish();
                                            }
                                        })
                                        .show();
                            }
                        }

                        @Override
                        public void onAnimationCancel() {
                            negview.setVisibility(View.INVISIBLE);
                            demonImg.setVisibility(View.INVISIBLE);
                            demonImg.clearAnimation();
                            state = NORMAL;
                            if (searchTimes == 2) {
                                String text = String.format(getString(R.string.escape), demon.getName());
                                alertDialogBuilder.setMessage(text)
                                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                setResult(RESULT_FIRST_USER);
                                                finish();
                                            }
                                        })
                                        .show();
                            }
                        }

                        @Override
                        public void onAnimationRepeat() {
                        }
                    });
                    state = TRANSITION;
                    if (sound_loaded_nacs) sp.play(soundNacs, 1, 1, 1, 0, 1);
                    anim.start();
                }
            }
        });

        fab.show();
    }

    private boolean checkIfInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        Log.d(TAG, "Internet status is " + ni);
        return (ni != null && ni.isConnectedOrConnecting());
    }

    public class SwipeDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
            Log.d(TAG, "onFling");
            final int swipe_min_distance = 100;
            final int swipe_max_distance = 200;
            final int swipe_threshold = 200;
            try {
                if (Math.abs(me1.getY() - me2.getY()) > swipe_max_distance) {
                    return false;
                }
                if (me1.getX() - me2.getX() > swipe_min_distance && Math.abs(velocityX) > swipe_threshold) {
                    if (state == NORMAL) {
                        switch (currentPos) {
                            case NORTH:
                                currentPos = EAST;
                                break;
                            case EAST:
                                currentPos = SOUTH;
                                break;
                            case SOUTH:
                                currentPos = WEST;
                                break;
                            case WEST:
                                currentPos = NORTH;
                                break;
                            default: break;
                        }
                        streetview.setImageDrawable(images.get(currentPos));
                        negview.setImageDrawable(reversed.get(currentPos));
                    }
                } else if (me2.getX() - me1.getX() > swipe_min_distance && Math.abs(velocityX) > swipe_threshold) {
                    if (state == NORMAL) {
                        switch (currentPos) {
                            case NORTH:
                                currentPos = WEST;
                                break;
                            case EAST:
                                currentPos = NORTH;
                                break;
                            case SOUTH:
                                currentPos = EAST;
                                break;
                            case WEST:
                                currentPos = SOUTH;
                                break;
                            default: break;
                        }
                        streetview.setImageDrawable(images.get(currentPos));
                        negview.setImageDrawable(reversed.get(currentPos));
                    }
                }
            } catch (Exception e) {}
            return super.onFling(me1, me2, velocityX, velocityY);
        }
    }
}
