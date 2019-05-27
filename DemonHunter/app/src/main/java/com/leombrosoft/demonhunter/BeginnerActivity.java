package com.leombrosoft.demonhunter;

import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;

import java.util.ArrayList;

public class BeginnerActivity extends AppCompatActivity {

    private boolean continueMusic = false;
    private ArrayList<NegMessage> msg_queue;
    private CardViewAdapter adapter;
    private Button eliminate_all;
    private NfcAdapter nfca;
    private GameValues gv;
    private int state;

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
        BackgroundMusicManager.start(this, BackgroundMusicManager.MUSIC_VELVET_ROOM);
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
                            setResult(RESULT_CANCELED);
                            finish();
                        } else {
                            gv.setPlayerStats();
                            gv.setTutorialDone();
                            setResult(RESULT_OK);
                            finish();
                        }
                    }
                };
                new AlertDialog.Builder(this)
                        .setMessage(R.string.exit_app)
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
        setContentView(R.layout.beginner_activity);

        gv = GameValues.get(this);

        msg_queue = new ArrayList<>();
        adapter = new CardViewAdapter(msg_queue);

        nfca = NfcAdapter.getDefaultAdapter(this);
        if (nfca != null) {
            nfca.setNdefPushMessage(null, this);
        }

        RecyclerView recyclerView = (RecyclerView) this.findViewById(R.id.begin_recyview);
        RecyclerView.ItemAnimator itemAnimator = new MyItemAnimation(this);
        recyclerView.setItemAnimator(itemAnimator);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        eliminate_all = (Button) findViewById(R.id.begin_eliminate_all);

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
                                if (msg_queue.isEmpty()) {
                                    state++;
                                    continueBeginningSequence();
                                }
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    msg_queue.remove(position);
                                    adapter.notifyItemRemoved(position);
                                }
                                adapter.notifyDataSetChanged();
                                if (msg_queue.isEmpty()) {
                                    state++;
                                    continueBeginningSequence();
                                }
                            }
                        });

        recyclerView.addOnItemTouchListener(swipeTouch);

        eliminate_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int sz = msg_queue.size();
                Log.d("Negotiation", "size of message queue is " + sz);
                if (sz > 0) {
                    for (int i = sz - 1; i >= 0; i--) {
                        msg_queue.remove(i);
                        adapter.notifyItemRemoved(i);
                    }
                    adapter.notifyDataSetChanged();
                    state++;
                    continueBeginningSequence();
                }
            }
        });

        state = 1;

        final View view = findViewById(R.id.fadeLL);

        Animation anim = AnimationUtils.loadAnimation(this, R.anim.slowfadeout);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
                msg_queue.add(new NegMessage(getString(R.string.longnose), getString(R.string.begin1)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                msg_queue.add(new NegMessage(getString(R.string.longnose), getString(R.string.begin2)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        view.startAnimation(anim);

    }

    private void continueBeginningSequence() {
        String igor = "Igor";
        switch (state) {
            case 2:
                msg_queue.add(new NegMessage(igor, getString(R.string.begin3)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                msg_queue.add(new NegMessage(igor, getString(R.string.begin4)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                break;
            case 3:
                msg_queue.add(new NegMessage(igor, getString(R.string.begin5)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                break;
            case 4:
                msg_queue.add(new NegMessage(igor, getString(R.string.begin6)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                break;
            case 5:
                msg_queue.add(new NegMessage(igor, getString(R.string.begin7)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                msg_queue.add(new NegMessage(igor, getString(R.string.begin8)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                break;
            case 6:
                msg_queue.add(new NegMessage(igor, getString(R.string.begin9)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                break;
            case 7:
                msg_queue.add(new NegMessage(igor, getString(R.string.begin10)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                break;
            case 8:
                askName();
                break;
            case 9:
                msg_queue.add(new NegMessage(igor, getString(R.string.begin12)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                msg_queue.add(new NegMessage(igor, getString(R.string.begin13)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                break;
            case 10:
                msg_queue.add(new NegMessage(igor, getString(R.string.begin14)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                msg_queue.add(new NegMessage(igor, getString(R.string.begin15)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                break;
            case 11:
                msg_queue.add(new NegMessage(igor, getString(R.string.begin16)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                msg_queue.add(new NegMessage(igor, getString(R.string.begin17)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                break;
            case 12:
                msg_queue.add(new NegMessage(igor, getString(R.string.begin18)));
                adapter.notifyItemInserted(msg_queue.size() - 1);
                break;
            default:
                DatabaseManager dbm = DatabaseManager.get(this);
                dbm.populateItemsDB();
                dbm.populateDemonDB();
                gv.setPlayerStats();
                gv.setUUID();
                gv.setTutorialDone();
                setResult(RESULT_OK);
                finish();
                break;
        }
    }

    private void askName() {
        final View v = getLayoutInflater().inflate(R.layout.insert_name, null);
        ((TextInputLayout)v.findViewById(R.id.namewrapper)).setHint(getString(R.string.name));
        ((TextInputLayout)v.findViewById(R.id.surnamewrapper)).setHint(getString(R.string.surname));
        new AlertDialog.Builder(this)
            .setTitle(R.string.insert_name)
            .setView(v)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = ((EditText) v.findViewById(R.id.editName)).getText().toString();
                        String surname = ((EditText) v.findViewById(R.id.editSurname)).getText().toString();
                        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(surname)) {
                            askName();
                        } else {
                            gv.setPlayerName(surname, name);
                            String s = String.format(getString(R.string.begin11), gv.getPlayerName(), gv.getPlayerSurname());
                            msg_queue.add(new NegMessage("Igor", s));
                            adapter.notifyItemInserted(msg_queue.size() - 1);
                        }
                    }
                })
                .show();
    }
}
