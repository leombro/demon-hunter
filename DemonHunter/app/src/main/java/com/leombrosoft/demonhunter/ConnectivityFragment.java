package com.leombrosoft.demonhunter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.github.jorgecastilloprz.FABProgressCircle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by neku on 01/11/15.
 */
public class ConnectivityFragment extends Fragment {

    private static final String APP_UUID = "d61e9b84-4f11-4405-b587-a41a97609725";
    private static final String TAG = MainActivity.TAG + "Connectivity";

    private DatabaseManager dbm;
    private boolean continueMusic = false;
    private GameValues gv;
    private FragmentActivity activity;
    private LinearLayout layout;
    private FloatingActionButton fab_bluetooth;
    private FloatingActionButton fab_nfc;
    private TextView bluetooth_main;
    private TextView bluetooth_desc;
    private TextView nfc_main;
    private TextView nfc_desc;
    private TextView start_trade;
    private TextView accept_trade;
    private FABProgressCircle progress_circle;
    private BluetoothAdapter bta;
    private NfcAdapter nfa;
    private boolean isBluetoothPresent;
    private boolean isNfcPresent;
    private BroadcastReceiver bluetooth_status_receiver;
    private BroadcastReceiver nfc_status_receiver;
    private int toTrade;
    private BroadcastReceiver bt_device_receiver;
    private Handler handler;
    private boolean searching = false;
    private boolean listening = false;
    private BluetoothServerSocket bss;
    private BluetoothSocket bs;
    private boolean exitforPause = false;

    private int color_red;
    private int ripple_red;
    private int color_green;
    private int ripple_green;
    private int color_yellow;
    private int ripple_yellow;
    private String bluetooth_not_present;
    private String bluetooth_off;
    private String bluetooth_on;
    private String bluetooth_not_present_desc;
    private String bluetooth_off_desc;
    private String bluetooth_on_desc;
    private String bluetooth_searching;
    private String bluetooth_searching_desc;
    private String nfc_not_present;
    private String nfc_off;
    private String ab_off;
    private String nfc_on;
    private String nfc_not_present_desc;
    private String nfc_off_desc;
    private String nfc_on_desc;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int DISCOVERY_REQUEST = 2;

    public static NdefMessage makeNFCMessage(String payload) {
        byte[] mimetype = "application/com.leombrosoft.demonhunter"
                .getBytes(Charset.forName("US-ASCII"));

        return new NdefMessage(new NdefRecord[] {
                new NdefRecord(
                        NdefRecord.TNF_MIME_MEDIA,
                        mimetype,
                        new byte[0],
                        payload.getBytes()
                ),
                NdefRecord.createApplicationRecord("com.leombrosoft.demonhunter"),
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            switch (resultCode) {
                case MainActivity.RESULT_OK:
                    makeBluetoothOn();
                    break;
                case MainActivity.RESULT_CANCELED:
                    makeBluetoothOff();
                    break;
                default:
                    break;
            }
        }
        if (requestCode == DISCOVERY_REQUEST) {
            switch (resultCode) {
                case MainActivity.RESULT_CANCELED:
                    Snackbar
                            .make(activity.findViewById(R.id.conn_root), R.string.must_discover, Snackbar.LENGTH_SHORT)
                            .show();
                    break;
                default:
                    makeServer(resultCode * 1000);
                    break;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!continueMusic)
            BackgroundMusicManager.pause();
        if (bluetooth_status_receiver != null)
            activity.unregisterReceiver(bluetooth_status_receiver);
        if (nfc_status_receiver != null)
            activity.unregisterReceiver(nfc_status_receiver);
        searching = false;
        listening = false;
        exitforPause = true;
        if (bss != null) {
            try {
                Log.d(TAG, "closed bss in onpause");
                bss.close();
                bss = null;
            } catch (IOException e) {
                Log.d(TAG, "Could not close serversocket: " + e);
            }
        }
        if (bs != null) {
            try {
                Log.d(TAG, "closed bs in onpause");
                bs.close();
                bs = null;
            } catch (IOException e) {
                Log.d(TAG, "Could not close clientsocket: " + e);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        continueMusic = false;
        exitforPause = false;
        BackgroundMusicManager.start(getContext(), BackgroundMusicManager.MUSIC_SCHOOL_DAYS);
        if (bluetooth_status_receiver != null)
            activity.registerReceiver(bluetooth_status_receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        if (nfc_status_receiver != null)
            activity.registerReceiver(nfc_status_receiver, new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED));
        if (isNfcPresent)
            checkNFC();
        if (isBluetoothPresent)
            checkBluetooth();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        gv = GameValues.get(getContext());
        handler = new Handler();
        dbm = DatabaseManager.get(getContext());
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layout = (LinearLayout) inflater.inflate(R.layout.connectivity_fragment, container, false);

        setColorsAndText();

        fab_bluetooth = (FloatingActionButton)layout.findViewById(R.id.view);
        fab_nfc = (FloatingActionButton)layout.findViewById(R.id.view2);
        bluetooth_main = (TextView)layout.findViewById(R.id.bluetooth_main);
        bluetooth_desc = (TextView)layout.findViewById(R.id.bluetooth_desc);
        nfc_main = (TextView)layout.findViewById(R.id.nfc_main);
        nfc_desc = (TextView)layout.findViewById(R.id.nfc_desc);
        start_trade = (TextView)layout.findViewById(R.id.textView4);
        accept_trade = (TextView)layout.findViewById(R.id.textView5);
        progress_circle = (FABProgressCircle)layout.findViewById(R.id.progress_bt);

        fab_bluetooth.setOnClickListener(null);
        fab_nfc.setOnClickListener(null);

        bta = BluetoothAdapter.getDefaultAdapter();
        if (bta == null) {
            isBluetoothPresent = false;
            fab_bluetooth.setBackgroundTintList(ColorStateList.valueOf(color_red));
            fab_bluetooth.setRippleColor(ripple_red);
            bluetooth_main.setText(bluetooth_not_present);
            bluetooth_desc.setText(bluetooth_not_present_desc);
            start_trade.setTextColor(ContextCompat.getColor(getContext(), R.color.hint_text));
            accept_trade.setTextColor(ContextCompat.getColor(getContext(), R.color.hint_text));
        } else {
            isBluetoothPresent = true;
            checkBluetooth();
            bluetooth_status_receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    switch (state) {
                        case BluetoothAdapter.STATE_TURNING_ON:
                        case BluetoothAdapter.STATE_ON:
                            makeBluetoothOn();
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                        case BluetoothAdapter.STATE_OFF:
                            makeBluetoothOff();
                            break;
                        default:
                            break;
                    }
                }
            };
        }

        nfa = NfcAdapter.getDefaultAdapter(getContext());
        if (nfa == null) {
            isNfcPresent = false;
            fab_nfc.setBackgroundTintList(ColorStateList.valueOf(color_red));
            fab_nfc.setRippleColor(ripple_red);
            nfc_main.setText(nfc_not_present);
            nfc_desc.setText(nfc_not_present_desc);
        } else {
            isNfcPresent = true;
            checkNFC();
            nfc_status_receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "NFC Broad Receive");
                    int state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, -1);

                    switch (state) {
                        case NfcAdapter.STATE_TURNING_ON:
                        case NfcAdapter.STATE_ON:
                            if (nfa.isEnabled()) makeNfcOn();
                            else makeNfcOff(true);
                            break;
                        case NfcAdapter.STATE_TURNING_OFF:
                        case NfcAdapter.STATE_OFF:
                            makeNfcOff(false);
                            break;
                        default:
                            break;
                    }
                }
            };
        }

        ((Toolbar)activity.findViewById(R.id.toolbar)).setTitle(R.string.connectivity);
        return layout;
    }

    private void checkBluetooth() {
        if (bta.isEnabled()) {
            makeBluetoothOn();
        } else {
            makeBluetoothOff();
        }
    }

    private void checkNFC() {
        if (nfa.isEnabled()) {
            if (nfa.isNdefPushEnabled()) {
                makeNfcOn();
            } else {
                makeNfcOff(true);
            }
        } else {
            makeNfcOff(false);
        }
    }

    private void makeNfcOn() {
        fab_nfc.setBackgroundTintList(ColorStateList.valueOf(color_green));
        fab_nfc.setOnClickListener(null);
        fab_nfc.setRippleColor(ripple_green);
        nfc_main.setText(nfc_on);
        nfc_desc.setText(nfc_on_desc);
    }

    private void makeNfcOff(boolean isNFCon) {
        fab_nfc.setBackgroundTintList(ColorStateList.valueOf(color_yellow));
        fab_nfc.setRippleColor(ripple_yellow);
        nfc_desc.setText(nfc_off_desc);
        if (isNFCon) {
            nfc_main.setText(ab_off);
            fab_nfc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Settings.ACTION_NFCSHARING_SETTINGS);
                    startActivity(intent);
                }
            });
        } else {
            nfc_main.setText(nfc_off);
            fab_nfc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                    startActivity(intent);
                }
            });
        }
    }

    private void makeBluetoothOn() {
        if (!searching) {
            Log.d(TAG, "called MakeBluetoothOn");
            View.OnClickListener ocl = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.textView4:
                            pickDemon(true);
                            break;
                        case R.id.textView5:
                            pickDemon(false);
                            break;
                        default:
                            break;
                    }
                }
            };
            fab_bluetooth.setBackgroundTintList(ColorStateList.valueOf(color_green));
            fab_bluetooth.setOnClickListener(null);
            fab_bluetooth.setRippleColor(ripple_green);
            bluetooth_main.setText(bluetooth_on);
            bluetooth_desc.setText(bluetooth_on_desc);
            start_trade.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
            accept_trade.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_text));
            start_trade.setOnClickListener(ocl);
            accept_trade.setOnClickListener(ocl);
            progress_circle.measure(15, 15);
            progress_circle.hide();
        }
    }

    private void makeBluetoothSearching() {
        searching = true;
        Log.d(TAG, "called MakeBluetoothSearching");
        start_trade.setTextColor(ContextCompat.getColor(getContext(), R.color.hint_text));
        accept_trade.setTextColor(ContextCompat.getColor(getContext(), R.color.hint_text));
        start_trade.setOnClickListener(null);
        accept_trade.setOnClickListener(null);
        progress_circle.show();
        bluetooth_main.setText(bluetooth_searching);
        bluetooth_desc.setText(bluetooth_searching_desc);

    }

    private void makeBluetoothOff() {
        fab_bluetooth.setBackgroundTintList(ColorStateList.valueOf(color_yellow));
        fab_bluetooth.setRippleColor(ripple_yellow);
        fab_bluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
        });
        bluetooth_main.setText(bluetooth_off);
        bluetooth_desc.setText(bluetooth_off_desc);
        start_trade.setTextColor(ContextCompat.getColor(getContext(), R.color.hint_text));
        accept_trade.setTextColor(ContextCompat.getColor(getContext(), R.color.hint_text));
        start_trade.setOnClickListener(null);
        accept_trade.setOnClickListener(null);
    }

    private void setColorsAndText() {
        color_red = ContextCompat.getColor(getContext(),R.color.red_500);
        ripple_red = ContextCompat.getColor(getContext(),R.color.red_700);
        color_green = ContextCompat.getColor(getContext(),R.color.green_700);
        ripple_green = ContextCompat.getColor(getContext(),R.color.green_900);
        color_yellow = ContextCompat.getColor(getContext(),R.color.yellow_600);
        ripple_yellow = ContextCompat.getColor(getContext(),R.color.yellow_800);
        bluetooth_not_present = getString(R.string.bluetooth_not_present);
        bluetooth_off = getString(R.string.bluetooth_off);
        bluetooth_on = getString(R.string.bluetooth_on);
        bluetooth_not_present_desc = getString(R.string.bluetooth_not_present_desc);
        bluetooth_off_desc = getString(R.string.bluetooth_off_desc);
        bluetooth_on_desc = getString(R.string.bluetooth_on_desc);
        bluetooth_searching = getString(R.string.bluetooth_accepting);
        bluetooth_searching_desc = getString(R.string.bluetooth_accepting_desc);
        nfc_not_present = getString(R.string.nfc_not_present);
        nfc_off = getString(R.string.nfc_off);
        ab_off = getString(R.string.nfc_ab_off);
        nfc_on = getString(R.string.nfc_on);
        nfc_not_present_desc = getString(R.string.nfc_not_present_desc);
        nfc_off_desc = getString(R.string.nfc_off_desc);
        nfc_on_desc = getString(R.string.nfc_on_desc);
    }

    private void pickDemon(final boolean isServer) {
        final ArrayList<Demon> demons = dbm.getDemons(true);
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        if (demons.isEmpty()) {
            alert.setMessage(R.string.no_caught_demons)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        } else {
            DemonAdapter adapter = new DemonAdapter(getContext(), R.layout.demon_list_adapter, demons);
            alert.setTitle(R.string.choose_demon)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        toTrade = demons.get(which).getDatabaseKey();
                        trade(isServer);
                    }
                })
                .show();
        }
    }

    private void trade(boolean isServer) {
        if (isServer) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), DISCOVERY_REQUEST);
        } else {
            bt_device_receiver = new BroadcastReceiver() {
                private AlertDialog ad;
                private boolean first_time = true;
                String begin_discovery = BluetoothAdapter.ACTION_DISCOVERY_STARTED,
                        end_discovery = BluetoothAdapter.ACTION_DISCOVERY_FINISHED,
                        device_found = BluetoothDevice.ACTION_FOUND;
                private ArrayList<BluetoothDevice> array = new ArrayList<>();

                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "received BT event!");
                    if (first_time) {
                        AlertDialog.Builder ab = new AlertDialog.Builder(getContext());
                        SimpleDeviceAdapter adapter = new SimpleDeviceAdapter(getContext(), R.layout.bt_device_adapter, array);
                        ab.setTitle(R.string.bt_device_string)
                                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        makeClient(array.get(which));
                                    }
                                })
                                .setCancelable(false)
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        bta.cancelDiscovery();
                                    }
                                });
                        ad = ab.create();
                        first_time = false;
                    }

                    if (begin_discovery.equals(intent.getAction())) {
                        Log.d(TAG, "BT event is begin discovery!");
                        ad.show();
                    } else if (end_discovery.equals(intent.getAction())) {
                        Log.d(TAG, "BT event is end discovery!");
                        ad.dismiss();
                        activity.unregisterReceiver(bt_device_receiver);
                    } else if (device_found.equals(intent.getAction())) {
                        Log.d(TAG, "BT event is FOUND DEVICE!");
                        BluetoothDevice ndev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        array.add(ndev);
                        ((ArrayAdapter)ad.getListView().getAdapter()).notifyDataSetChanged();
                    }
                }
            };
            bta.startDiscovery();
            activity.registerReceiver(bt_device_receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
            activity.registerReceiver(bt_device_receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
            activity.registerReceiver(bt_device_receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }

    }

    private void makeServer(final int timeout) {
        Log.d(TAG, "timeout is " + timeout);
        try {
            bss =
                    bta.listenUsingRfcommWithServiceRecord("com.leombrosoft.demonhunter/Server", UUID.fromString(APP_UUID));
            Runnable accept = new Runnable() {
                @Override
                public void run() {
                    try {
                        final BluetoothSocket socket = bss.accept(timeout);
                        if (socket.isConnected()) Log.d(TAG, "Socket is connected");
                        send(socket);
                        final String result = receive(socket);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                tradeResults(result, socket);
                            }
                        });
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!exitforPause)
                                    noHunterFound();
                            }
                        });
                        Log.d(TAG, "bss accept exception is " + e);
                    } finally {
                        try {
                            if (bss != null) {
                                Log.d(TAG, "closed bss in makeserver");
                                bss.close();
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Cannot close BT Server Socket", e);
                        }
                    }
                }
            };
            makeBluetoothSearching();
            new Thread(accept).start();
        } catch (IOException e) {
            throw new RuntimeException("Exception in creating BT Server Socket", e);
        }
    }

    private void tradeResults(String json, BluetoothSocket socket) {
        AlertDialog.Builder ab = new AlertDialog.Builder(getContext());
        if (socket != null) {
            try {
                Log.d(TAG, "closed socket in traderesults");
                socket.close();
            } catch (IOException e) {
                Log.d(TAG, "error in closing socket", e);
            }
        }
        try {
            JSONObject obj = new JSONObject(json);
            String surname = obj.getString("SURNAME"),
                    name = obj.getString("NAME");
            int demonID = obj.getInt("DEMON");
            String success = String.format(getString(R.string.trade_success),
                    dbm.getDemon(toTrade).getName(),
                    name,
                    surname,
                    dbm.getDemon(demonID).getName());
            ab.setMessage(success);
            dbm.addRemoveCaughtDemon(toTrade, true);
            dbm.addRemoveCaughtDemon(demonID, false);
        } catch (JSONException e) {
            Log.d(TAG, "JSONException", e);
            ab.setMessage(R.string.trade_fail);
        }
        ab.setCancelable(false)
                .setNeutralButton(android.R.string.ok, null)
                .show();
        Log.d(TAG, "tradeResults are " + json);
        searching = false;
        toTrade = -1;
        makeBluetoothOn();
    }

    private void noHunterFound() {
        Log.d(TAG, "called noHunterFound");
        searching = false;
        if (bss != null) {
            try {
                Log.d(TAG, "closed bss in nohunterfound");
                bss.close();
                bss = null;
            } catch (IOException e) {
                Log.d(TAG, "Could not close serversocket: " + e);
            }
        }
        ((FABProgressCircle)activity.findViewById(R.id.progress_bt)).hide();
        AlertDialog.Builder ab = new AlertDialog.Builder(getContext());
        ab.setMessage(R.string.no_hunter_nearby)
                .setNeutralButton(android.R.string.ok, null)
                .show();
        makeBluetoothOn();
    }

    private void makeClient(BluetoothDevice dv) {
        try {
            bta.cancelDiscovery();
            bs = dv.createRfcommSocketToServiceRecord(UUID.fromString(APP_UUID));
            Runnable conn = new Runnable() {
                @Override
                public void run() {
                    try {
                        try {
                            bs.connect();
                            final String result = receive(bs);
                            send(bs);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    tradeResults(result, bs);
                                }
                            });
                        } catch (IOException e) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (!exitforPause)
                                        noHunterFound();//makeConnInterrupted();
                                }
                            });
                        } finally {
                            if (bs != null) {
                                Log.d(TAG, "closed bs in makeclient");
                                bs.close();
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Could not close", e);
                    }
                }
            };
            makeBluetoothSearching();//makebluetoothconnect();
            new Thread(conn).start();
        } catch (IOException e) {
            throw new RuntimeException("Exception in creating BT Client Socket", e);
        }
    }

    private void send(BluetoothSocket socket) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("SURNAME", gv.getPlayerSurname())
                    .put("NAME", gv.getPlayerName())
                    .put("DEMON", toTrade);
            String json = obj.toString() + " ";
            Log.d(TAG, "jsonobj is " + json);
            byte[] buf = json.getBytes();
            buf[buf.length - 1] = 0;
            OutputStream os = null;
            try {
                os = socket.getOutputStream();
                os.write(buf);
            } catch (IOException e) {
                Log.e(TAG, "Message send failed", e);
                throw new RuntimeException(e);
            }
        }  catch (JSONException e) {
            Log.d(TAG, "JSON ERROR", e);
        }
    }

    private String receive(BluetoothSocket socket) {
        String ret = null;
        InputStream is = null;
        listening = true;
        int bufsz = 1024;
        byte[] torec = new byte[1024];
        final StringBuilder toProcess = new StringBuilder();
        try {
            is = socket.getInputStream();
            int bytes_read = -1;
            while (listening) {
                bytes_read = is.read(torec);
                if (bytes_read != -1) {
                    String result = "";
                    while (bytes_read == bufsz && torec[bufsz - 1] != 0) {
                        result = result + new String(torec, 0, bytes_read - 1);
                        bytes_read = is.read(torec);
                    }
                    result = result + new String(torec, 0, bytes_read - 1);
                    toProcess.append(result);
                }
                listening = false;
            }
            ret = toProcess.toString();
        } catch (IOException e) {
            Log.e(TAG, "Message receive failed", e);
            throw new RuntimeException(e);
        }
        return ret;
    }
}
