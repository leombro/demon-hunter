package com.leombrosoft.demonhunter;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;

import java.util.ArrayList;
import java.util.Random;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

/**
 * Created by neku on 27/10/15.
 */
public class DemonNegotiation extends AppCompatActivity {

    public static final String TAG = MainActivity.TAG + "/Negotiation";

    private boolean continueMusic = false;
    private DatabaseManager dbm;
    private Button button1;
    private Button button2;
    private Button button3;
    private Button button4;
    private ImageView card;
    private Button eliminate_all;
    private Demon demon;
    private int currentyen;
    private int curritem;
    private View.OnClickListener onClickListener;
    private View.OnClickListener ok_give_money;
    private int give_yen;
    private View.OnClickListener ok_give_item;
    private int give_item;
    private int toDismiss;
    private View.OnClickListener reject;
    private boolean swipetoreject = false;
    private CardViewAdapter adapter;
    private Random r;
    private int demon_state;
    private int demon_disp;
    private int maxturns;
    private int turns;
    private double base_chance;
    private ArrayList<NegMessage> msg_queue;
    private ImageView demon_img;
    private ImageView demon_overlay;
    private Animation slideout;
    private Animation slidein;
    private Animation slidein_button;
    private Animation slideout_button;
    private boolean escaped = false;
    private GameValues gv;
    private NfcAdapter nfca;

    private float player_charisma;
    private float player_charm;
    private float player_luck;

    private static final int STATE_DEMON_FRIENDLY = -2;
    private static final int STATE_DEMON_ALMOST_FRIENDLY = -1;
    private static final int STATE_DEMON_NEUTRAL = 0;
    private static final int STATE_DEMON_ALMOST_PISSED = 1;
    private static final int STATE_DEMON_PISSED = 2;

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
        BackgroundMusicManager.start(this, BackgroundMusicManager.MUSIC_ILL_FACE_MYSELF_BATTLE);
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
                DialogInterface.OnClickListener ocl = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            finish();
                        }
                    }
                };
                new AlertDialog.Builder(this)
                        .setMessage(R.string.run)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, ocl)
                        .setNegativeButton(android.R.string.no, ocl)
                        .show();
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_negotiation);

        nfca = NfcAdapter.getDefaultAdapter(this);
        if (nfca != null) {
            nfca.setNdefPushMessage(null, this);
        }

        Intent starter = getIntent();
        int demonID = starter.getIntExtra("DEMON", -1);
        double distance = starter.getDoubleExtra("DIST", -1);

        gv = GameValues.get(this);
        if (gv.setPlayerStats()) Log.d("Negotiation", "added player values");
        else Log.d("Negotiation", "NOT added player values");
        r = new Random(System.currentTimeMillis());
        demon_state = STATE_DEMON_NEUTRAL;
        maxturns = 5 + r.nextInt(5);
        player_charisma = gv.getPlayerCharisma();
        player_charm = gv.getPlayerCharm();
        player_luck = gv.getPlayerLuck();
        base_chance = distance;
        Log.d("Negotiation", "generated basechance is " + base_chance);
        base_chance = 400 - base_chance;
        base_chance /= 400;
        base_chance *= 30;
        demon_disp = 100;
        Log.d("Negotiation", "basechance is " + base_chance);

        msg_queue = new ArrayList<>();

        adapter = new CardViewAdapter(msg_queue);

        dbm = DatabaseManager.get(this);
        demon = dbm.getDemon(demonID);
        demon_img = (ImageView) findViewById(R.id.demon_neg);
        demon_overlay = (ImageView) findViewById(R.id.demon_neg2);
        if (demonID > -1) {
            demon_img.setImageDrawable(ContextCompat.getDrawable(this, demon.getImage()));
            demon_overlay.setImageDrawable(ContextCompat.getDrawable(this, demon.getImage()));
        } else finish();
        int id = 1;
        switch (1 + new Random().nextInt(6)) {
            case 1:
                id = R.drawable.back1;
                break;
            case 2:
                id = R.drawable.back2;
                break;
            case 3:
                id = R.drawable.back3;
                break;
            case 4:
                id = R.drawable.back4;
                break;
            case 5:
                id = R.drawable.back5;
                break;
            case 6:
                id = R.drawable.back6;
                break;
        }
        ((ImageView) findViewById(R.id.backdrop)).setImageDrawable(ContextCompat.getDrawable(this, id));
        final View cardview = findViewById(R.id.negcard);
        slideout = AnimationUtils.loadAnimation(DemonNegotiation.this, R.anim.fade_to_left);
        slidein = AnimationUtils.loadAnimation(DemonNegotiation.this, R.anim.fade_in_from_right);
        slidein_button = AnimationUtils.loadAnimation(DemonNegotiation.this, R.anim.fade_in_from_right);
        slidein_button.setDuration(300);
        slideout_button = AnimationUtils.loadAnimation(DemonNegotiation.this, R.anim.fade_out_to_right);
        slidein.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                button1.setOnClickListener(onClickListener);
                button2.setOnClickListener(onClickListener);
                button3.setOnClickListener(onClickListener);
                button4.setOnClickListener(onClickListener);
                card.setOnClickListener(onClickListener);
                cardview.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        String name = demon.getName().concat(" ");
        ((TextView) findViewById(R.id.textView2)).setText(name);
        final TextView tvoverlay = (TextView) findViewById(R.id.textViewOverlay);
        tvoverlay.setText(name);
        final Animation white = AnimationUtils.loadAnimation(this, R.anim.white_animation),
                animation1 = AnimationUtils.loadAnimation(this, R.anim.wobbling),
                ani2 = AnimationUtils.loadAnimation(this, R.anim.fade_wobbling),
                aniText = AnimationUtils.loadAnimation(this, R.anim.fade_text);
        Animation.AnimationListener ali = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                Log.d(TAG, "Beginning proper animations");
                demon_img.clearAnimation();
                demon_img.startAnimation(animation1);
                demon_overlay.startAnimation(ani2);
                tvoverlay.startAnimation(aniText);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        };
        white.setAnimationListener(ali);
        demon_img.startAnimation(white);

        button1 = (Button) findViewById(R.id.button);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        button4 = (Button) findViewById(R.id.button4);
        card = (ImageView) findViewById(R.id.arcanacard);
        eliminate_all = (Button) findViewById(R.id.eliminate_all);

        ok_give_money = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                giveMoney();
            }
        };
        ok_give_item = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                giveItem();
            }
        };
        reject = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _reject(false);
            }
        };

        onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button1.setOnClickListener(null);
                button2.setOnClickListener(null);
                button3.setOnClickListener(null);
                button4.setOnClickListener(null);
                card.setOnClickListener(null);
                slideout.reset();
                slidein_button.reset();
                boolean arcanaUsed = false;
                if (turns == maxturns) {
                    escape();
                } else {
                    turns++;
                    int warning = 1 + r.nextInt(2);
                    if (turns == maxturns - warning) {
                        msg_queue.add(new NegMessage(String.format(getString(R.string.restless), demon.getName())));
                        adapter.notifyItemInserted(msg_queue.size()-1);
                    }
                    switch (v.getId()) {
                        case R.id.button:
                            Log.d("Negotiation", "Case 1: There are " + adapter.getItemCount() + " items");
                            negotiate_provoke();
                            break;
                        case R.id.button2:
                            Log.d("Negotiation", "Case 2: There are " + adapter.getItemCount() + " items");
                            negotiate_persuade();
                            break;
                        case R.id.button3:
                            Log.d("Negotiation", "Case 3: There are " + adapter.getItemCount() + " items");
                            negotiate_joke();
                            break;
                        case R.id.button4:
                            Log.d("Negotiation", "Case 4: There are " + adapter.getItemCount() + " items");
                            negotiate_yell();
                            break;
                        case R.id.arcanacard:
                            beginArcanaUse();
                            arcanaUsed = true;
                            break;
                        default:
                            break;
                    }
                }
                if (!arcanaUsed) {
                    cardview.startAnimation(slideout);
                    cardview.setVisibility(View.GONE);
                    eliminate_all.startAnimation(slidein_button);
                    eliminate_all.setVisibility(View.VISIBLE);
                }
            }
        };

        button1.setOnClickListener(onClickListener);
        button2.setOnClickListener(onClickListener);
        button3.setOnClickListener(onClickListener);
        button4.setOnClickListener(onClickListener);
        card.setOnClickListener(onClickListener);
        card.startAnimation(AnimationUtils.loadAnimation(this, R.anim.card_wobbling));

        RecyclerView recyclerView = (RecyclerView) this.findViewById(R.id.recyview);
        RecyclerView.ItemAnimator itemAnimator = new MyItemAnimation(this);
        recyclerView.setItemAnimator(itemAnimator);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        SwipeableRecyclerViewTouchListener swipeTouch =
                new SwipeableRecyclerViewTouchListener(recyclerView,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipe(int position) {
                                return true;
                            }

                            @Override
                            public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    msg_queue.remove(position);
                                    adapter.notifyItemRemoved(position);
                                }
                                adapter.notifyDataSetChanged();
                                remakeSelectionBox(cardview);
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    msg_queue.remove(position);
                                    adapter.notifyItemRemoved(position);
                                }
                                adapter.notifyDataSetChanged();
                                remakeSelectionBox(cardview);
                            }
                        });

        recyclerView.addOnItemTouchListener(swipeTouch);

        eliminate_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!swipetoreject) {
                    int sz = msg_queue.size();
                    Log.d("Negotiation", "size of message queue is " + sz);
                    if (sz > 0) {
                        for (int i = sz - 1; i >= 0; i--) {
                            msg_queue.remove(i);
                            adapter.notifyItemRemoved(i);
                        }
                        adapter.notifyDataSetChanged();
                        remakeSelectionBox(cardview);
                    }
                }
            }
        });
    }

    private void beginArcanaUse() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setMessage(R.string.wildcard_use);
        adb.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                button1.setOnClickListener(onClickListener);
                button2.setOnClickListener(onClickListener);
                button3.setOnClickListener(onClickListener);
                button4.setOnClickListener(onClickListener);
                card.setOnClickListener(onClickListener);
            }
        });
        adb.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ok_arcana();
            }
        });
        adb.setCancelable(false);
        adb.show();
    }

    private void ok_arcana() {
        final ImageView iv = (ImageView) findViewById(R.id.monsterwhite);
        View v = findViewById(R.id.shower);
        eliminate_all.setVisibility(View.INVISIBLE);
        iv.setImageDrawable(ContextCompat.getDrawable(this, demon.getImage()));
        DisplayMetrics dm = this.getResources().getDisplayMetrics();
        final boolean caught = checkIfCaught();
        //int dispX = dm.widthPixels, dispY = dm.heightPixels - FindDemonActivity.getStatusBarHeight(getResources());
        int dispX = v.getWidth(), dispY = v.getHeight();
        int centerX = dispX / 2;
        int centerY = dispY / 2;
        int startRadius = 0;
        // get the final radius for the clipping circle
        int endRadius = (int) Math.hypot(centerX, centerY);
        final ImageView arcana_card = (ImageView) findViewById(R.id.arcanacard);
        arcana_card.clearAnimation();
        int[] arr = new int[2];
        arcana_card.getLocationOnScreen(arr);
        int centerCardX = arr[0] + (arcana_card.getWidth() / 2), centerCardY = (arr[1] - FindDemonActivity.getStatusBarHeight(getResources()))  + (arcana_card.getHeight()/2);
        int dx = centerX - centerCardX, dy = centerY - centerCardY;
        TranslateAnimation transl_center = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.ABSOLUTE, dx,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.ABSOLUTE, dy
        );
        transl_center.setDuration(1000);
        transl_center.setInterpolator(new LinearInterpolator());
        transl_center.setFillAfter(true);
        final SupportAnimator anim =
                ViewAnimationUtils.createCircularReveal(v, centerX, centerY, startRadius, endRadius);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(3000);
        final SupportAnimator anim2 = anim.reverse();
        final Animation anim3;
        if (caught) anim3 = AnimationUtils.loadAnimation(this, R.anim.demon_caught);
        else anim3 = AnimationUtils.loadAnimation(this, R.anim.demon_uncaught);
        anim3.setFillAfter(true);
        anim.addListener(new SupportAnimator.AnimatorListener() {
            @Override
            public void onAnimationStart() {
            }

            @Override
            public void onAnimationEnd() {
                iv.startAnimation(anim3);
            }

            @Override
            public void onAnimationCancel() {
            }

            @Override
            public void onAnimationRepeat() {
            }
        });
        anim3.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {anim2.start();}

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        anim2.setInterpolator(new AccelerateDecelerateInterpolator());
        anim2.setDuration(500);
        anim2.addListener(new SupportAnimator.AnimatorListener() {
            @Override
            public void onAnimationStart() {
            }

            @Override
            public void onAnimationEnd() {
                (findViewById(R.id.shower)).setVisibility(View.INVISIBLE);
                if (!caught) {
                    AlertDialog.Builder ab = new AlertDialog.Builder(DemonNegotiation.this);
                    ab.setMessage(String.format(getString(R.string.escape), demon.getName()));
                    ab.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    ab.setCancelable(false);
                    ab.show();
                } else {
                    AlertDialog.Builder ab = new AlertDialog.Builder(DemonNegotiation.this);
                    ab.setMessage(String.format(getString(R.string.caught), demon.getName()));
                    dbm.addRemoveCaughtDemon(demon.getDatabaseKey(), false);
                    ab.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    ab.setCancelable(false);
                    ab.show();
                }
            }

            @Override
            public void onAnimationCancel() {
            }

            @Override
            public void onAnimationRepeat() {
            }
        });
        transl_center.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                (findViewById(R.id.shower)).setVisibility(View.VISIBLE);
                iv.setVisibility(View.VISIBLE);
                anim.start();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        arcana_card.startAnimation(transl_center);
    }

    private boolean checkIfCaught() {
        double prob = base_chance + 10 * demon_state + (player_luck/GameValues.MAX_LUCK) * 20;
        if (prob > 100) prob = 100;
        int test = r.nextInt(10000);
        Log.d("Negotiation", "chance is " + prob * 100 + " generated is " + test);
        if (test > prob * 100) {
            return false;
        } else {
            return true;
        }
    }

    private void giveItem() {
        if (curritem < 1) {
            msg_queue.add(new NegMessage(getString(R.string.not_enough)));
            adapter.notifyItemInserted(msg_queue.size()-1);
            _reject(false);
        } else {
            dbm.changeItemAmount(give_item, -1);
            demon_disp += 10 + r.nextInt(10);
            updateDemonDisposition();
            msg_queue.add(new NegMessage(demon.getName(), String.format(getString(R.string.gave), demon.getQuirk(false))));
            adapter.notifyItemInserted(msg_queue.size() - 1);
            swipetoreject = false;
            msg_queue.remove(toDismiss);
            adapter.notifyDataSetChanged();
            toDismiss = -1;
        }
    }

    private void giveMoney() {
        if (currentyen < give_yen) {
            msg_queue.add(new NegMessage(getString(R.string.not_enough)));
            adapter.notifyItemInserted(msg_queue.size()-1);
            _reject(false);
        } else {
            gv.changePlayerMoney(-give_yen);
            demon_disp += 10 + r.nextInt(10);
            updateDemonDisposition();
            msg_queue.add(new NegMessage(demon.getName(), String.format(getString(R.string.gave), demon.getQuirk(true))));
            adapter.notifyItemInserted(msg_queue.size() - 1);
            swipetoreject = false;
            msg_queue.remove(toDismiss);
            adapter.notifyDataSetChanged();
            toDismiss = -1;
        }
    }

    private void _reject(boolean swiped) {
        Log.d("Negotiation", "In _reject");
        int res = r.nextInt(2);
        NegMessage nm = null;
        if (res == 0) {
            Log.d("Negotiation", "_reject 0");
            demon_disp -= 10 + r.nextInt(5);
            nm = new NegMessage(demon.getName(), getString(R.string.no_1));
        } else {
            Log.d("Negotiation", "_reject 1");
            demon_disp -= 20 + r.nextInt(5);
            nm = new NegMessage(demon.getName(), getString(R.string.no_2));
        }
        updateDemonDisposition();
        swipetoreject = false;
        msg_queue.add(nm);
        Log.d("Negotiation", "Added nm");
        adapter.notifyItemInserted(msg_queue.size() - 1);
        if (!swiped) {
            msg_queue.remove(toDismiss);
            adapter.notifyDataSetChanged();
        }
        toDismiss = -1;
    }

    private void remakeSelectionBox(View v) {
        if (escaped) {
            Log.d("Negotiation", "escaping");
            finish();
        } else if (swipetoreject) {
            Log.d("Negotiation", "rejecting");
            _reject(true);
        } else if (msg_queue.size() == 0) {
            Log.d("Negotiation", "remaking card");
            slidein.reset();
            slideout_button.reset();
            v.startAnimation(slidein);
            eliminate_all.startAnimation(slideout_button);
            eliminate_all.setVisibility(View.INVISIBLE);
        }
    }

    private void changeDemonState(int newState) {
        if (demon_state != newState) {
            demon_state = newState;
            ImageView layer = (ImageView) findViewById(R.id.demon_neg2);
            TextView text = (TextView) findViewById(R.id.textViewOverlay);
            if (layer.getVisibility() == View.INVISIBLE) layer.setVisibility(View.VISIBLE);
            int color;
            switch (demon_state) {
                case STATE_DEMON_NEUTRAL:
                    color = -1;
                    break;
                case STATE_DEMON_ALMOST_FRIENDLY:
                    color = Color.argb(64, 0, 180, 0);
                    break;
                case STATE_DEMON_FRIENDLY:
                    color = Color.argb(128, 0, 180, 0);
                    break;
                case STATE_DEMON_ALMOST_PISSED:
                    color = Color.argb(64, 255, 0, 0);
                    break;
                case STATE_DEMON_PISSED:
                    color = Color.argb(128, 255, 0, 0);
                    break;
                default:
                    color = -1;
                    break;
            }
            if (color == -1) {
                layer.clearColorFilter();
                text.setTextColor(Color.WHITE);
            } else {
                layer.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
                text.setTextColor(color);
            }
        }
    }

    private static final int TYPE_SMALL = 0; // small friendship increment, chance of money/items
    private static final int TYPE_BIG = 1; // big friendship increment, no money/items

    private boolean negotiation(String message,
                             String response1,
                             String response2,
                             String response3,
                             float curr_value,
                             float max_value,
                             int neg_type) {
        boolean result = false;
        NegMessage ms1 = new NegMessage(message), ms2;
        msg_queue.add(ms1);
        adapter.notifyItemInserted(msg_queue.size()-1);
        double chance_good = (base_chance + (curr_value/max_value) * 30 + (-10 + r.nextInt(30)));
        Log.d("Negotiation", "factor is " + ((curr_value/max_value) * 30));
        if (r.nextInt(GameValues.MAX_LUCK) < r.nextInt((int)player_luck)) chance_good += 25 + r.nextInt(25);
        double chance_neutral = chance_good + 10 + r.nextInt(20);
        if (chance_good > 100) chance_good = 100;
        if (chance_neutral > 100) chance_neutral = 100;
        int rolldice = r.nextInt(10000);
        Log.d("Negotiation", "chance for good provoke is " + (chance_good * 100) + ", neutral is " + (100 * chance_neutral) + ", roll is " + rolldice);
        if (rolldice > chance_good * 100) {
            if (rolldice > chance_neutral * 100) {
                ms2 = new NegMessage(demon.getName(), response3);
                Log.d("Negotiation", "Case 3: disp went from " + demon_disp);
                switch (neg_type) {
                    case TYPE_BIG:
                        demon_disp -= 20 + r.nextInt(20);
                        break;
                    case TYPE_SMALL:
                        demon_disp -= 10 + r.nextInt(5);
                        break;
                    default:
                        break;
                }
                Log.d("Negotiation", "Case 3: to " + demon_disp);
            } else {
                ms2 = new NegMessage(demon.getName(), response2);
                Log.d("Negotiation", "Case 2: disp went from " + demon_disp);
                switch (neg_type) {
                    case TYPE_BIG:
                        demon_disp += -10 + r.nextInt(10);
                        break;
                    case TYPE_SMALL:
                        demon_disp += -5 + r.nextInt(5);
                        break;
                    default:
                        break;
                }
                Log.d("Negotiation", "Case 2: to " + demon_disp);
            }
        } else {
            result = true;
            ms2 = new NegMessage(demon.getName(), response1);
            Log.d("Negotiation", "Case 1: disp went from " + demon_disp);
            switch (neg_type) {
                case TYPE_BIG:
                    demon_disp += 20 + r.nextInt(20);
                    break;
                case TYPE_SMALL:
                    demon_disp += 10 + r.nextInt(5);
                    break;
                default:
                    break;
            }
            Log.d("Negotiation", "Case 1: to " + demon_disp);
        }
        updateDemonDisposition();
        msg_queue.add(ms2);
        adapter.notifyItemInserted(msg_queue.size()-1);
        return result;
    }

    private void negotiate_provoke() {
        String msg = String.format(getString(R.string.provoked_msg), demon.getName());
        String res1 = String.format(getString(R.string.provoked_res_1), demon.getQuirk(true));
        String res2 = getString(R.string.provoked_res_2);
        String res3 = String.format(getString(R.string.provoked_res_3), demon.getQuirk(true));
        boolean result = negotiation(msg, res1, res2, res3, player_charisma, GameValues.MAX_CHARISMA, TYPE_BIG);
        if (!result) {
            double chance_for_ask_money = base_chance + demon_state * 20 + r.nextInt (10) - ((player_luck/GameValues.MAX_LUCK) * 20);
            if (chance_for_ask_money < 0) chance_for_ask_money = 0;
            int test = r.nextInt(10000);
            Log.d("Negotiation","result not good, chance " + chance_for_ask_money + " test " + test);
            if (test <= chance_for_ask_money * 100) {
                int yen = 100 + r.nextInt(400);
                currentyen = gv.getPlayerMoney();
                String wants = String.format(getString(R.string.wants_money), yen, demon.getQuirk(false));
                String[] temp1 = wants.split("delim");
                String wants_final = temp1[0] + " \uD83D\uDCB4 " + temp1[1];
                msg_queue.add(new NegMessage(demon.getName(), wants_final));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                String yens = String.format(getString(R.string.give_money_msg), yen, demon.getName(), currentyen);
                String[] temp = yens.split("delim");
                String yen_final = temp[0] + " \uD83D\uDCB4 " + temp[1];
                give_yen = yen;
                swipetoreject = true;
                msg_queue.add(new NegMessage(null, yen_final, getString(android.R.string.ok), getString(android.R.string.no), ok_give_money, reject));
                toDismiss = msg_queue.size()-1;
                adapter.notifyItemInserted(toDismiss);
            }
        }
    }

    private void negotiate_persuade() {
        String msg = String.format(getString(R.string.persuade_msg), demon.getName());
        String res1 = String.format(getString(R.string.persuade_res_1), demon.getQuirk(false));
        String res2 = String.format(getString(R.string.persuade_res_2), demon.getQuirk(false));
        String res3 = String.format(getString(R.string.persuade_res_3), demon.getQuirk(false));
        boolean result = negotiation(msg, res1, res2, res3, player_charm, GameValues.MAX_CHARM, TYPE_SMALL);
        if (result) {
            double chance_for_money = base_chance - demon_state * 20 + (player_luck/GameValues.MAX_LUCK) * 50 + r.nextInt(10);
            if (chance_for_money > 100) chance_for_money = 100;
            int test = r.nextInt(10000);
            if (test <= chance_for_money * 100) {
                int yen = 100 + r.nextInt(400);
                msg_queue.add(new NegMessage(demon.getName(), String.format(getString(R.string.give_money_1), demon.getQuirk(false))));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                String yens = String.format(getString(R.string.give_money_2), demon.getName(), yen);
                String[] temp = yens.split("delim");
                String yen_final = temp[0] + " \uD83D\uDCB4 " + temp[1];
                msg_queue.add(new NegMessage(yen_final));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                gv.changePlayerMoney(yen);
            }
        }
    }

    private void negotiate_joke() {
        String msg = String.format(getString(R.string.joke_msg), demon.getName());
        String res1 = getString(R.string.joke_res_1);
        String res2 = getString(R.string.joke_res_2);
        String res3 = getString(R.string.joke_res_3);
        boolean result = negotiation(msg, res1, res2, res3, player_charisma, GameValues.MAX_CHARISMA, TYPE_SMALL);
        if (result) {
            double chance_for_items = base_chance - demon_state * 20 + (player_luck/GameValues.MAX_LUCK) * 50 + r.nextInt(10);
            if (chance_for_items > 100) chance_for_items = 100;
            int test = r.nextInt(10000);
            if (test <= chance_for_items * 100) {
                int item = 1 + r.nextInt(3);
                Item it = dbm.getItem(item);
                Log.d("Negotiation", "item has id " + item + " and is " + it);
                msg_queue.add(new NegMessage(demon.getName(), String.format(getString(R.string.give_item_1), demon.getQuirk(false))));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                msg_queue.add(new NegMessage(String.format(getString(R.string.give_item_2), demon.getName(), it.getName())));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                dbm.changeItemAmount(item, 1);
            }
        }
    }

    private void negotiate_yell() {
        String msg = String.format(getString(R.string.yell_msg), demon.getName());
        String res1 = String.format(getString(R.string.yell_res_1), demon.getQuirk(false));
        String res2 = getString(R.string.yell_res_2);
        String res3 = getString(R.string.yell_res_3);
        boolean result = negotiation(msg, res1, res2, res3, player_charm, GameValues.MAX_CHARM, TYPE_BIG);
        if (!result) {
            double chance_for_ask_item = base_chance + demon_state * 20 + r.nextInt (10) - ((player_luck/GameValues.MAX_LUCK) * 20);
            if (chance_for_ask_item < 0) chance_for_ask_item = 0;
            int test = r.nextInt(10000);
            if (test <= chance_for_ask_item * 100) {
                int prob = r.nextInt(100);
                if (prob <= 49) give_item = 0;
                else give_item = 1 + r.nextInt(3);
                Item item = dbm.getItem(give_item);
                curritem = item.getQuantity();
                String wants = String.format(getString(R.string.wants_item), demon.getQuirk(true), item.getName());
                msg_queue.add(new NegMessage(demon.getName(), wants));
                adapter.notifyItemInserted(msg_queue.size()-1);
                String items = String.format(getString(R.string.give_item_msg), item.getName(), demon.getName(), curritem);
                swipetoreject = true;
                msg_queue.add(new NegMessage(null, items, getString(android.R.string.ok), getString(android.R.string.no), ok_give_item, reject));
                toDismiss = msg_queue.size()-1;
                adapter.notifyItemInserted(toDismiss);
            }
        }
    }

    private void escape() {
        NegMessage nm = new NegMessage(String.format(getString(R.string.escape), demon.getName()));
        msg_queue.add(nm);
        adapter.notifyItemInserted(msg_queue.size() - 1);
        escaped = true;
    }

    private void updateDemonDisposition() {
        int changeState = 0;
        int text;
        if (demon_disp > 170) {
            changeState = STATE_DEMON_FRIENDLY;
            text = R.string.respect;
        } else if (demon_disp > 120) {
            changeState = STATE_DEMON_ALMOST_FRIENDLY;
            text = R.string.friendly;
        } else if (demon_disp > 80) {
            changeState = STATE_DEMON_NEUTRAL;
            text = R.string.neutral;
        } else if (demon_disp > 30) {
            changeState = STATE_DEMON_ALMOST_PISSED;
            text = R.string.angry;
        } else if (demon_disp > 0){
            changeState = STATE_DEMON_PISSED;
            text = R.string.pissed;
        } else {
            changeState = STATE_DEMON_PISSED;
            text = -1;
            escape();
        }
        if (changeState != demon_state && !escaped) {
            msg_queue.add(new NegMessage(String.format(getString(text), demon.getName())));
            adapter.notifyItemInserted(msg_queue.size()-1);
            changeDemonState(changeState);
        }
    }

}
