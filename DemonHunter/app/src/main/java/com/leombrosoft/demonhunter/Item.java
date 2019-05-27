package com.leombrosoft.demonhunter;

/**
 * Created by neku on 29/10/15.
 */
public class Item {

    public static final int NOT_USABLE = 0, INCR_CHARISMA = 1, INCR_CHARM = 2, INCR_LUCK = 3;
    public static final int GLOBE = 0,
            SNACK = 1,
            INFUSE = 2,
            PUDDING = 3,
            BATTERY = 4,
            UPGRADE = 5;
    public static final int MAX_ITEMS = 6;

    private int id;
    private String name;
    private String description;
    private String action;
    private int use;
    private int quantity;

    public Item (int i, String n, String d, String a, int u, int q) {
        id = i;
        name = n;
        description = d;
        action = a;
        use = u;
        quantity = q;
    }

    public int getId() {
        return id;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getDescription() {
        return description;
    }

    public int getUse() {
        return use;
    }

    public String getName() {
        return name;
    }

    public String getAction() {
        return action;
    }

    public void setQuantity(int q) {
        quantity = q;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
