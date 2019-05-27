package com.leombrosoft.demonhunter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by neku on 02/11/15.
 */
public class DemonAdapter extends ArrayAdapter<Demon> {

    int resource;

    public DemonAdapter(Context context, int resource, List<Demon> items) {
        super(context, resource, items);
        this.resource = resource;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        LinearLayout todoView;

        Demon d = getItem(pos);

        String demonName = d.getName(),
                demonArcana = d.getArcana(),
                arcana_name = getContext().getString(R.string.arcana),
                caught_name = getContext().getString(R.string.caught_name);
        int caught = d.getCaught();
        String infostring = String.format("%s: %s, %s: %d", arcana_name, demonArcana, caught_name, caught);

        if (convertView == null) {
            todoView = new LinearLayout(getContext());
            LayoutInflater li = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            li.inflate(resource, todoView, true);
        } else {
            todoView = (LinearLayout) convertView;
        }

        TextView demonNameSlot = (TextView) todoView.findViewById(R.id.demon_name);
        TextView demonInfoSlot = (TextView) todoView.findViewById(R.id.demon_infos);

        demonNameSlot.setText(demonName);
        demonInfoSlot.setText(infostring);

        return todoView;
    }
}