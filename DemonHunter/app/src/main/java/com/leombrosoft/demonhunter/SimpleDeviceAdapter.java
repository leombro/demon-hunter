package com.leombrosoft.demonhunter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Created by neku on 02/11/15.
 */
public class SimpleDeviceAdapter extends ArrayAdapter<BluetoothDevice> {

    private int resource;

    public SimpleDeviceAdapter(Context context, int _resource, List<BluetoothDevice> items) {
        super(context, _resource, items);
        resource = _resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout todoView;

        BluetoothDevice d = getItem(position);

        String name = String.format("\uD83D\uDCF6 %s", d.getName());

        if (convertView == null) {
            todoView = new LinearLayout(getContext());
            LayoutInflater infl = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            infl.inflate(resource, todoView, true);
        } else {
            todoView = (LinearLayout) convertView;
        }

        TextView text = (TextView)todoView.findViewById(R.id.bt_device);

        text.setText(name);

        return todoView;
    }
}
