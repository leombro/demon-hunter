package com.leombrosoft.demonhunter;

import android.app.PendingIntent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.util.Random;

/**
 * Created by neku on 26/10/15.
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "DemonHunter";

    private ActionBarDrawerToggle actionBarDrawerToggle;
    private Toolbar toolbar;
    private GameValues gv;
    private DatabaseManager dbm;
    private NfcAdapter nfca;
    private PendingIntent pi;
    private IntentFilter[] mFilters;
    private View headerview;
    private Fragment toTransact;
    private boolean toShowFragment;

    private DemonScopeFragment d = new DemonScopeFragment();
    private ConnectivityFragment c = new ConnectivityFragment();
    private DemonDexFragment dd = new DemonDexFragment();
    private JunesFragment j = new JunesFragment();
    private StatusFragment s = new StatusFragment();

    private static final int BEGINNER_ACT = 1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BEGINNER_ACT:
                if (resultCode == RESULT_OK){
                    toShowFragment = true;
                } else {
                    finish();
                }
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        if (toShowFragment) {
            toShowFragment = false;
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, s)
                    .commit();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);

        Log.d(TAG, "Currently " + getResources().getDisplayMetrics().density);
        gv = GameValues.get(this);

        dbm = DatabaseManager.get(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        toolbar.setTitleTextColor(ContextCompat.getColor(this,R.color.primary_text));
        setSupportActionBar(toolbar);

        final DrawerLayout layout = (DrawerLayout) findViewById(R.id.Drawer);
        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, layout, toolbar, R.string.NULL_STRING, R.string.NULL_STRING) {
            private boolean invalidated = true;

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidated = true;
                if (toTransact != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left, R.anim.slide_in_right, R.anim.slide_out_right)
                            .replace(R.id.container, toTransact)
                            .addToBackStack(null)
                            .commit();
                }
                toTransact = null;
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                super.onDrawerStateChanged(newState);
                if (newState == DrawerLayout.STATE_IDLE && invalidated) {
                    updateHeader(headerview);
                    invalidated = false;
                }
            }
        };
        layout.setDrawerListener(actionBarDrawerToggle);

        nfca = NfcAdapter.getDefaultAdapter(this);
        if (nfca != null) {
            Log.d(TAG, "Made a push message");
            nfca.setNdefPushMessage(ConnectivityFragment.makeNFCMessage(gv.getUUID()), this);
            pi = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            try {
                ndef.addDataType("*/*");
            } catch (IntentFilter.MalformedMimeTypeException e) {
                throw new RuntimeException(e);
            }
            mFilters = new IntentFilter[] {ndef};
        }

        NavigationView nv = ((NavigationView) findViewById(R.id.LeftNavView));
        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.junes_menu:
                        toTransact = j;
                        break;
                    case R.id.demondex_menu:
                        toTransact = dd;
                        break;
                    case R.id.menu_demonscope:
                        toTransact = d;
                        break;
                    case R.id.debug:
                        gv.reset();
                        dbm.reset();
                        dbm.populateItemsDB();
                        dbm.populateDemonDB();
                        gv.setPlayerStats();
                        gv.setUUID();
                        break;
                    case R.id.cheat:
                        gv.changePlayerMoney(10000);
                        for (int i = 1; i <= 22; i++) {
                            dbm.addRemoveCaughtDemon(i, false);
                        }
                        for (int i = 0; i < 6; i++) {
                            dbm.changeItemAmount(i, 1);
                        }
                        break;
                    case R.id.connectivity_menu:
                        toTransact = c;
                        break;
                    default:
                        break;
                }
                layout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        headerview = LayoutInflater.from(this).inflate(R.layout.menu_header, null);
        headerview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toTransact = s;
                layout.closeDrawer(GravityCompat.START);
            }
        });
        nv.addHeaderView(headerview);
        updateHeader(headerview);

        if (!gv.getTutorialDone()) {
            Intent intent = new Intent(this, BeginnerActivity.class);
            startActivityForResult(intent, BEGINNER_ACT);
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, s)
                    .commit();
        }
    }

    private void updateHeader(View nv) {
        String name_surname = gv.getPlayerName() + " " + gv.getPlayerSurname();
        String moneystring = String.format(" \uD83D\uDCB4 %d ", gv.getPlayerMoney());
        Log.d(TAG, "Updating header, playermoney is " + gv.getPlayerMoney());
        ((TextView)nv.findViewById(R.id.navbar_name)).setText(name_surname);
        ((TextView)nv.findViewById(R.id.navbar_money)).setText(moneystring);
        int cd = dbm.getDemonsCaught(), td = dbm.getTotalDemons();
        String caughtDemons = String.format("%d", cd);
        String maxDemons = String.format("/ %d", td);
        if (cd == td) maxDemons = maxDemons + "\u2B50";
        ((TextView)nv.findViewById(R.id.navbar_demoncaught)).setText(caughtDemons);
        ((TextView)nv.findViewById(R.id.navbar_demoncaught_max)).setText(maxDemons);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfca != null)
            nfca.enableForegroundDispatch(this, pi, mFilters, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfca != null)
            nfca.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            Log.d(TAG, "Received intent! " + intent);
            if (intent.getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
                Log.d(TAG, "Received A GOOD intent!");
                Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                NdefRecord record = ((NdefMessage)messages[0]).getRecords()[0];
                String payload = new String(record.getPayload());
                if (dbm.checkAndPutGuest(payload)) {
                    int item = new Random().nextInt(Item.MAX_ITEMS);
                    Item i = dbm.getItem(item);
                    String s = String.format(getString(R.string.meet_ok), i.getName());
                    new AlertDialog.Builder(this)
                            .setMessage(s)
                            .show();
                    dbm.changeItemAmount(i.getId(), 1);
                } else {
                    Snackbar
                            .make(findViewById(R.id.Drawer), R.string.meet_no, Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return actionBarDrawerToggle.onOptionsItemSelected(menuItem) || super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        actionBarDrawerToggle.syncState();
    }
}
