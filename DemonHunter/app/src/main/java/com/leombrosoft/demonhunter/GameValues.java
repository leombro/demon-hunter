package com.leombrosoft.demonhunter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.Random;
import java.util.UUID;

/**
 * Created by neku on 17/10/15.
 */
public class GameValues {

    public static final String TAG = MainActivity.TAG + "/GameValues";

    public static final String GAME_PREFERENCES = "GAME_PREFERENCES";

    // Keys
    private static final String KEY_UUID = "UUID";
    private static final String TUTORIAL_DONE = "TUTORIAL_DONE";
    private static final String LAST_SEARCH = "LAST_SEARCH";
    private static final String SEARCH_TIMES = "SEARCH_TIMES";
    private static final String PLAYER_STATS_ASSIGNED = "PLAYER_STATS_ASSIGNED";
    private static final String PLAYER_SURNAME = "PLAYER_SURNAME";
    private static final String PLAYER_NAME = "PLAYER_NAME";
    private static final String PLAYER_MONEY = "PLAYER_MONEY";
    private static final String PLAYER_LAST_LAT = "PLAYER_LAST_LAT";
    private static final String PLAYER_LAST_LNG = "PLAYER_LAST_LNG";
    private static final String PLAYER_LAST_TIME = "PLAYER_LAST_TIME";

    private static final String STAT_CHARISMA = "STAT_CHARISMA";
    private static final String STAT_CHARM = "STAT_CHARM";
    private static final String STAT_LUCK = "STAT_LUCK";

    private static GameValues instance;

    private SharedPreferences prefs;
    private Context context;

    public static final int MAX_CHARISMA = 50;
    public static final int MAX_CHARM = 50;
    public static final int MAX_LUCK = 30;

    public GameValues(Context c) {
        context = c;
        prefs = context.getSharedPreferences(GAME_PREFERENCES, Context.MODE_PRIVATE);
    }

    public static GameValues get(Context context) {
        if (instance == null)
            instance = new GameValues(context);
        return instance;
    }

    public void reset() {
        prefs.edit()
                .clear()
                .apply();
    }

    public boolean getTutorialDone() {
        return prefs.getBoolean(TUTORIAL_DONE, false);
    }

    public void setTutorialDone() {
        prefs.edit()
            .putBoolean(TUTORIAL_DONE, true)
            .apply();
    }

    public void setUUID() {
        String uuid = UUID.randomUUID().toString();
        prefs.edit()
                .putString(KEY_UUID, uuid)
                .apply();
    }

    public String getUUID() {
        String uuid = prefs.getString(KEY_UUID, "");
        return uuid;
    }

    public void setLastLocation(LatLng loc) {
        double lat = loc.latitude;
        double lng = loc.longitude;
        prefs.edit()
                .putFloat(PLAYER_LAST_LAT, (float) lat)
                .putFloat(PLAYER_LAST_LNG, (float) lng)
                .putLong(PLAYER_LAST_TIME, System.currentTimeMillis())
                .apply();
    }

    public LatLng getLastLocation() {
        double lat = prefs.getFloat(PLAYER_LAST_LAT, 0);
        double lng = prefs.getFloat(PLAYER_LAST_LNG, 0);
        return new LatLng(lat, lng);
    }

    public long getLastLocationTime() {
        return prefs.getLong(PLAYER_LAST_TIME, 0);
    }

    public boolean setPlayerStats() {
        if (!prefs.getBoolean(PLAYER_STATS_ASSIGNED, false)) {
            Random r = new Random();
            int charisma = 10 + r.nextInt(5);
            int charm = 10 + r.nextInt(5);
            int luck = 5 + r.nextInt(5);
            Log.d(TAG,  "Player stats are " + charisma + ", " + charm + ", " + luck);
            prefs
                    .edit()
                    .putInt(STAT_CHARISMA, charisma)
                    .putInt(STAT_CHARM, charm)
                    .putInt(STAT_LUCK, luck)
                    .putBoolean(PLAYER_STATS_ASSIGNED, true)
                    .putInt(PLAYER_MONEY, 500)
                    .apply();
            return true;
        } else return false;
    }

    public int getPlayerCharisma() {
        return prefs.getInt(STAT_CHARISMA, -1);
    }

    public void increaseCharisma() {
        int curr = prefs.getInt(STAT_CHARISMA, -1);
        prefs.edit()
                .putInt(STAT_CHARISMA, curr + 1)
                .apply();
    }

    public int getPlayerCharm() {
        return prefs.getInt(STAT_CHARM, -1);
    }

    public void increaseCharm() {
        int curr = prefs.getInt(STAT_CHARM, -1);
        prefs.edit()
                .putInt(STAT_CHARM, curr + 1)
                .apply();
    }

    public int getPlayerLuck() {
        return prefs.getInt(STAT_LUCK, -1);
    }

    public void increaseLuck() {
        int curr = prefs.getInt(STAT_LUCK, -1);
        prefs.edit()
                .putInt(STAT_LUCK, curr + 1)
                .apply();
    }

    public int getPlayerMoney() {
        return prefs.getInt(PLAYER_MONEY, 0);
    }

    public void setPlayerName(String surname, String name) {
        prefs
                .edit()
                .putString(PLAYER_SURNAME, surname)
                .putString(PLAYER_NAME, name)
                .apply();
    }

    public String getPlayerSurname() {
        return prefs.getString(PLAYER_SURNAME, "Yuki");
    }

    public String getPlayerName() {
        return prefs.getString(PLAYER_NAME, "Makoto");
    }

    public boolean canSearch(int i) { // TODO: 17/10/15 remove
        return true;
    } //// TODO: 26/10/15 remove, it's debug

    private int maxSearchTimes() {
        return DatabaseManager.get(context).getItem(Item.BATTERY).getQuantity();
    }

    public boolean canSearch() {
        Log.d(DemonScopeFragment.TAG, "Search times " + prefs.getInt(SEARCH_TIMES, 0) + " max " + maxSearchTimes());
        Log.d(DemonScopeFragment.TAG, "Last search " + prefs.getLong(LAST_SEARCH, 0) + " retry after " +
                (prefs.getLong(LAST_SEARCH, 0) + (86400*1000)) + " now is " + System.currentTimeMillis() + " (diff is " +
                (prefs.getLong(LAST_SEARCH, 0) + (86400 * 1000) - System.currentTimeMillis()) );
        if (prefs.getInt(SEARCH_TIMES, 0) > maxSearchTimes()) {
            if (prefs.getLong(LAST_SEARCH, 0) + (86400 * 1000) > System.currentTimeMillis()) {
                return false;
            }
            else {
                prefs.edit()
                    .putInt(SEARCH_TIMES, 0)
                    .apply();
                return true;
            }
        } else
            return true;
    }

    public long[] explainRejection() {
        return new long[] { prefs.getInt(SEARCH_TIMES, 0), prefs.getLong(LAST_SEARCH, 0) };
    }

    public void doneSearch() {
        int i = prefs.getInt(SEARCH_TIMES, 0);
        prefs.edit()
            .putLong(LAST_SEARCH, System.currentTimeMillis())
            .putInt(SEARCH_TIMES, i+1)
            .apply();
    }

    public void changePlayerMoney(int amount) {
        int act = prefs.getInt(PLAYER_MONEY, 0);
        prefs.edit()
                .putInt(PLAYER_MONEY, act + amount)
                .apply();
    }
}
