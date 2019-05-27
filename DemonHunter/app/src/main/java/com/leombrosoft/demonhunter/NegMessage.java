package com.leombrosoft.demonhunter;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;

/**
 * Created by neku on 28/10/15.
 */
public class NegMessage {
    private String name;
    private String message;
    private String btnpos_text;
    private String btnneg_text;
    private View.OnClickListener btnpos_action;
    private View.OnClickListener btnneg_action;

    public NegMessage(String n,
                      @NonNull String m,
                      String ptext,
                      String ntext,
                      View.OnClickListener pb,
                      View.OnClickListener nb) {
        name = n;
        message = m;
        btnpos_text = ptext;
        btnneg_text = ntext;
        btnpos_action = pb;
        btnneg_action = nb;
    }

    public NegMessage(String n,
                      @NonNull String m,
                      String ptext,
                      View.OnClickListener pb) {
        this(n, m, ptext, null, pb, null);
    }

    public NegMessage(String n,
                      @NonNull String m) {
        this(n, m, null, null, null, null);
    }

    public NegMessage(@NonNull String m,
                      String ptext,
                      View.OnClickListener pb) {
        this(null, m, ptext, null, pb, null);
    }

    public NegMessage(@NonNull String m) {
        this(null, m, null, null, null, null);
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public String getPositiveButtonText() {
        return btnpos_text;
    }

    public String getNegativeButtonText() {
        return btnneg_text;
    }

    public View.OnClickListener getPositiveButtonAction() {
        return btnpos_action;
    }

    public View.OnClickListener getNegativeButtonAction() {
        return btnneg_action;
    }
}
