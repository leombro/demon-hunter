package com.leombrosoft.demonhunter;

/**
 * Created by neku on 21/10/15.
 */
public class Demon {
    private int databaseKey;
    private String name;
    private String description;
    private String arcana;
    private String quirk;
    private int caught;
    private int image;

    public Demon(int dbKey, String name, String description, String arcana, String quirk, int caught, int image) {
        databaseKey = dbKey;
        this.name = name;
        this.description = description;
        this.arcana = arcana;
        this.caught = caught;
        this.image = image;
        this.quirk = quirk;
    }

    public Demon(String name, String description, String arcana, String quirk, int image) {
        this(-1, name, description, arcana, quirk, 0, image);
    }

    public int getDatabaseKey() {
        return databaseKey;
    }

    public int getCaught() {
        return caught;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getArcana() {
        return arcana;
    }

    public int getImage() {
        return image;
    }

    public String getQuirk(boolean uppercase) {
        if (uppercase)
            return quirk.substring(0,1).toUpperCase() + quirk.substring(1);
        else
            return quirk;
    }

    public void setCaught(int caught) {
        this.caught = caught;
    }

    @Override
    public String toString() {
        return name;
    }

}
