package com.leombrosoft.demonhunter;

import android.app.ActionBar;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class StatusFragment extends Fragment {

    public static final String TAG = MainActivity.TAG + "Status";
    private FragmentActivity activity;
    private LinearLayout layout;
    private GameValues gv;
    private ArrayList<Item> items;
    private ItemAdapter adapter;
    private DatabaseManager dbm;
    private boolean continueMusic;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        gv = GameValues.get(getContext());
        dbm = DatabaseManager.get(getContext());
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layout = (LinearLayout) inflater.inflate(R.layout.status_fragment, container, false);

        ((TextView)layout.findViewById(R.id.status_player_name)).setText(gv.getPlayerName());
        ((TextView)layout.findViewById(R.id.status_player_surname)).setText(gv.getPlayerSurname());

        String moneystring = String.format(" \uD83D\uDCB4 %d ", gv.getPlayerMoney());
        ((TextView)layout.findViewById(R.id.status_money)).setText(moneystring);

        int cd = dbm.getDemonsCaught(), td = dbm.getTotalDemons();
        String caughtDemons = String.format("%d", cd);
        String maxDemons = String.format("/ %d", td);
        if (cd == td) maxDemons = maxDemons + "\u2B50";
        ((TextView)layout.findViewById(R.id.status_demoncaught)).setText(caughtDemons);
        ((TextView)layout.findViewById(R.id.status_demoncaught_max)).setText(maxDemons);

        updateCharisma();
        updateCharm();
        updateLuck();

        items = dbm.getItems(true);

        ListView lw = (ListView) layout.findViewById(R.id.status_item_list);
        adapter = new ItemAdapter(getContext(), R.layout.item_list_adapter, items, false);
        lw.setAdapter(adapter);

        lw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                Item i = items.get(position);
                new AlertDialog.Builder(getContext())
                        .setTitle(i.getName())
                        .setMessage(i.getDescription())
                        .setPositiveButton(R.string.use, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                useItem(position);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });

        ((Toolbar) activity.findViewById(R.id.toolbar)).setTitle(R.string.status);
        return layout;
    }

    private void useItem(int position) {
        Item i = items.get(position);
        AlertDialog.Builder build = new AlertDialog.Builder(getContext())
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateLuck();
                        updateCharisma();
                        updateCharm();
                    }
                });
        switch (i.getUse()) {
            case Item.NOT_USABLE:
                build.setMessage(R.string.not_usable)
                        .show();
                break;
            case Item.INCR_CHARISMA:
                int charisma = gv.getPlayerCharisma();
                if (charisma == GameValues.MAX_CHARISMA) {
                    build.setMessage(R.string.max_charisma)
                            .show();
                } else {
                    dbm.changeItemAmount(i.getId(), -1);
                    i.setQuantity(i.getQuantity() - 1);
                    if (i.getQuantity() == 0) {
                        items.remove(position);
                    }
                    adapter.notifyDataSetChanged();
                    gv.increaseCharisma();
                    build.setMessage(R.string.incr_charisma)
                            .show();
                }
                break;
            case Item.INCR_CHARM:
                int charm = gv.getPlayerCharm();
                if (charm == GameValues.MAX_CHARM) {
                    build.setMessage(R.string.max_charm)
                            .show();
                } else {
                    dbm.changeItemAmount(i.getId(), -1);
                    i.setQuantity(i.getQuantity() - 1);
                    if (i.getQuantity() == 0) {
                        items.remove(position);
                    }
                    adapter.notifyDataSetChanged();
                    gv.increaseCharm();
                    build.setMessage(R.string.incr_charm)
                            .show();
                }
                break;
            case Item.INCR_LUCK:
                int luck = gv.getPlayerLuck();
                if (luck == GameValues.MAX_LUCK) {
                    build.setMessage(R.string.max_luck)
                            .show();
                } else {
                    dbm.changeItemAmount(i.getId(), -1);
                    i.setQuantity(i.getQuantity() - 1);
                    if (i.getQuantity() == 0) {
                        items.remove(position);
                    }
                    adapter.notifyDataSetChanged();
                    gv.increaseLuck();
                    build.setMessage(R.string.incr_luck)
                            .show();
                }
                break;
        }
    }

    private void updateCharisma() {
        TextView charisma = (TextView) layout.findViewById(R.id.status_charisma);

        int currCharisma = gv.getPlayerCharisma();
        int maxCharisma = GameValues.MAX_CHARISMA;

        float dp = 50 + ((float) currCharisma / (float) maxCharisma) * 200;
        float pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
        Log.d(TAG, "Charisma Barsize is " + dp + " dp, " + pixels + " pixels");

        charisma.setText(String.format("%d / %d", currCharisma, maxCharisma));
        ViewGroup.LayoutParams params = charisma.getLayoutParams();
        params.width = (int) pixels;
        charisma.setLayoutParams(params);
    }

    private void updateCharm() {
        TextView charm = (TextView) layout.findViewById(R.id.status_charm);

        int currCharm = gv.getPlayerCharm();
        int maxCharm = GameValues.MAX_CHARM;

        float dp = 50 + ((float) currCharm / (float) maxCharm) * 200;
        float pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
        Log.d(TAG, "Charm Barsize is " + dp + " dp, " + pixels + " pixels");

        charm.setText(String.format("%d / %d", currCharm, maxCharm));
        ViewGroup.LayoutParams params = charm.getLayoutParams();
        params.width = (int) pixels;
        charm.setLayoutParams(params);
    }

    private void updateLuck() {
        TextView luck = (TextView) layout.findViewById(R.id.status_luck);

        int currLuck = gv.getPlayerLuck();
        int maxLuck = GameValues.MAX_LUCK;

        float dp = 50 + ((float) currLuck / (float) maxLuck) * 200;
        float pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
        Log.d(TAG, "Luck Barsize is " + dp + " dp, " + pixels + " pixels");

        luck.setText(String.format("%d / %d", currLuck, maxLuck));
        ViewGroup.LayoutParams params = luck.getLayoutParams();
        params.width = (int) pixels;
        luck.setLayoutParams(params);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!continueMusic)
            BackgroundMusicManager.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        continueMusic = false;
        BackgroundMusicManager.start(getContext(), BackgroundMusicManager.MUSIC_VELVET_ROOM);
    }

}
