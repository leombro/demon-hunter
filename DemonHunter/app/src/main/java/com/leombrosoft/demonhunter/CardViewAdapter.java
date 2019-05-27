package com.leombrosoft.demonhunter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

/**
 * Created by neku on 28/10/15.
 */
public class CardViewAdapter extends RecyclerView.Adapter<CardViewAdapter.ViewHolder> {
    private List<NegMessage> cards;

    public CardViewAdapter(List<NegMessage> cds){
        this.cards = cds;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup vg, int i){
        View v = LayoutInflater.from(vg.getContext()).inflate(R.layout.card_view_layout, vg, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder vh, int i) {
        NegMessage msg = cards.get(i);
        if (msg.getName() != null) {
            vh.title.setText(msg.getName());
            if (vh.title.getVisibility() != View.VISIBLE) vh.title.setVisibility(View.VISIBLE);
        } else
            vh.title.setVisibility(View.GONE);
        vh.text.setText(msg.getMessage());
        if (msg.getPositiveButtonText() != null) {
            vh.btn_pos.setText(msg.getPositiveButtonText());
            vh.btn_pos.setOnClickListener(msg.getPositiveButtonAction());
            if (vh.btn_pos.getVisibility() != View.VISIBLE) vh.btn_pos.setVisibility(View.VISIBLE);
        } else {
            vh.btn_pos.setVisibility(View.GONE);
        }
        if (msg.getNegativeButtonText() != null) {
            vh.btn_neg.setText(msg.getNegativeButtonText());
            vh.btn_neg.setOnClickListener(msg.getNegativeButtonAction());
            if (vh.btn_neg.getVisibility() != View.VISIBLE) vh.btn_neg.setVisibility(View.VISIBLE);
        } else {
            vh.btn_neg.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return cards == null ? 0 : cards.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private TextView text;
        private Button btn_pos;
        private Button btn_neg;

        public ViewHolder(View itemview) {
            super(itemview);

            title = (TextView) itemview.findViewById(R.id.card_view_title);
            text = (TextView) itemview.findViewById(R.id.card_view_text);
            btn_pos = (Button) itemview.findViewById(R.id.card_view_btnpos);
            btn_neg = (Button) itemview.findViewById(R.id.card_view_btnneg);
        }
    }
}