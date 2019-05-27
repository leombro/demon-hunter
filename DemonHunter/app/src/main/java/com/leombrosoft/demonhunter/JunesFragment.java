package com.leombrosoft.demonhunter;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by neku on 03/11/15.
 */
public class JunesFragment extends Fragment {

    public static final String TAG = MainActivity.TAG + "Junes";
    private FragmentActivity activity;
    private LinearLayout layout;
    private GameValues gv;
    private ArrayList<Item> items;
    private DatabaseManager dbm;
    private boolean continueMusic;
    private TextView money_quantity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        gv = GameValues.get(getContext());
        dbm = DatabaseManager.get(getContext());
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layout = (LinearLayout) inflater.inflate(R.layout.junes_fragment, container, false);

        TextView tv = (TextView)layout.findViewById(R.id.junes_ad);
        String ad = getString(R.string.junes_ad);
        String jingle = String.format(getString(R.string.junes_jingle), "\uD83C\uDFB6", "\uD83C\uDFB6");

        tv.setText(String.format("%s %s", ad, jingle));
        tv.setSelected(true);

        money_quantity = (TextView)layout.findViewById(R.id.junes_money_quantity);
        updateMoney();

        items = new ArrayList<>();
        items.add(dbm.getItem(Item.SNACK));
        items.add(dbm.getItem(Item.INFUSE));
        items.add(dbm.getItem(Item.PUDDING));

        ListView lw = (ListView) layout.findViewById(R.id.junes_item_list);
        ItemAdapter ia = new ItemAdapter(getContext(), R.layout.item_list_adapter, items, true);
        lw.setAdapter(ia);
        lw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int price = 0;
                final Item i = items.get(position);
                switch (i.getUse()) {
                    case Item.INCR_CHARISMA:
                    case Item.INCR_CHARM:
                        price = 1000;
                        break;
                    case Item.INCR_LUCK:
                        price = 5000;
                        break;
                    default:
                        break;
                }
                final int finalprice = price;
                String moneyz = String.format("\uD83D\uDCB4 %d", price);
                String buy_message = String.format(getString(R.string.wanna_buy), i.getName(), moneyz);
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.clerk)
                        .setMessage(buy_message)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        buy(i.getId(), finalprice);
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });


        ((Toolbar)activity.findViewById(R.id.toolbar)).setTitle(R.string.junes_department_store);
        return layout;
    }

    private void buy(int itemID, int price) {
        AlertDialog.Builder ab = new AlertDialog.Builder(getContext())
                .setTitle(R.string.clerk)
                .setPositiveButton(android.R.string.ok, null);
        if (gv.getPlayerMoney() > price) {
            gv.changePlayerMoney(-price);
            dbm.changeItemAmount(itemID, 1);
            updateMoney();
            ab.setMessage(R.string.purchase_ok)
                    .show();
        } else {
            ab.setMessage(R.string.no_enough_money)
                    .show();
        }
    }

    private void updateMoney(){
        int money = gv.getPlayerMoney();
        String moneystring = String.format(" \uD83D\uDCB4 %d ", money);
        money_quantity.setText(moneystring);
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
        BackgroundMusicManager.start(getContext(), BackgroundMusicManager.MUSIC_THEME_OF_JUNES);
    }
}
