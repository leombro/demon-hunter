package com.leombrosoft.demonhunter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Created by neku on 03/11/15.
 */
public class ItemAdapter extends ArrayAdapter<Item> {

    int resource;
    boolean shop;

    public ItemAdapter(Context context, int resource, List<Item> items, boolean _shop) {
        super(context, resource, items);
        this.resource = resource;
        shop = _shop;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        LinearLayout todoView;

        Item i = getItem(pos);

        String itemName = i.getName();
        int quantity = i.getQuantity();

        if (shop) {
            switch (i.getUse()) {
                case Item.INCR_CHARISMA:
                case Item.INCR_CHARM:
                    quantity = 1000;
                    break;
                case Item.INCR_LUCK:
                    quantity = 5000;
                    break;
                default:
                    quantity = 0;
                    break;
            }
        }

        String much = (shop ? String.format("\uD83D\uDCB4 %d", quantity) : String.format("%d", quantity));

        if (convertView == null) {
            todoView = new LinearLayout(getContext());
            LayoutInflater li = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            li.inflate(resource, todoView, true);
        } else {
            todoView = (LinearLayout) convertView;
        }

        TextView itemNameSlot = (TextView) todoView.findViewById(R.id.item_name);
        TextView itemQuantitySlot = (TextView) todoView.findViewById(R.id.item_quantity);

        itemNameSlot.setText(itemName);
        itemQuantitySlot.setText(much);

        return todoView;
    }
}
