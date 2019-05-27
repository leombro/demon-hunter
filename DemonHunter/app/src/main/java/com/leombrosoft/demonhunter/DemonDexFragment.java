package com.leombrosoft.demonhunter;

import android.app.DialogFragment;
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
 * Created by neku on 02/11/15.
 */
public class DemonDexFragment extends Fragment {

    public static final String TAG = MainActivity.TAG + "DemonDex";
    private FragmentActivity activity;
    private LinearLayout layout;
    private GameValues gv;
    private ArrayList<Demon> demons;
    private DatabaseManager dbm;
    private boolean continueMusic;
    private AlertDialog.Builder adb;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        gv = GameValues.get(getContext());
        dbm = DatabaseManager.get(getContext());
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layout = (LinearLayout) inflater.inflate(R.layout.demondex_fragment, container, false);

        ListView lw = (ListView) layout.findViewById(R.id.ddex_listView);

        adb = new AlertDialog.Builder(getContext());

        demons = dbm.getDemons(true);

        if (demons.isEmpty()) {
            lw.setVisibility(View.GONE);
        } else {
            (layout.findViewById(R.id.nodemonscard)).setVisibility(View.GONE);
            DemonAdapter da = new DemonAdapter(getContext(), R.layout.demon_list_adapter, demons);
            lw.setAdapter(da);
            lw.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Demon d = demons.get(position);
                    View v = inflater.inflate(R.layout.demondex_card_layout, null);
                    View v1 = inflater.inflate(R.layout.demon_custom_title, null);
                    ((ImageView)v.findViewById(R.id.ddex_image)).setImageDrawable(ContextCompat.getDrawable(getContext(), d.getImage()));
                    ((TextView)v.findViewById(R.id.ddex_demon)).setText(d.getName());
                    ((TextView)v.findViewById(R.id.ddex_arcana)).setText(d.getArcana());
                    ((TextView)v.findViewById(R.id.ddex_quirk)).setText(d.getQuirk(true) + "!");
                    ((TextView)v.findViewById(R.id.ddex_caught)).setText(String.format("%d", d.getCaught()));
                    ((TextView)v.findViewById(R.id.ddex_description)).setText(d.getDescription());
                    adb.setCustomTitle(v1);
                    adb.setView(v);
                    adb.show();
                }
            });
        }

        ((Toolbar)activity.findViewById(R.id.toolbar)).setTitle(R.string.dex);
        return layout;
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
