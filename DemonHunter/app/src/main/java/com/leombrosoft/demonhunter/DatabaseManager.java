package com.leombrosoft.demonhunter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by neku on 20/10/15.
 */
public class DatabaseManager {

    public static final String TAG = MainActivity.TAG + "/DBManager";

    private Helper dbhelper;
    private Context context;
    private int demonsNo = -1;
    private int demonsCaught = -1;
    private boolean invalidateDemons = true;
    private static DatabaseManager dbm = null;
    private boolean theresmarkers = false;
    private LruCache<Integer, Item> item_cache;
    private LruCache<Integer, Demon> demon_cache;
    private DatabaseManager(Context context) {
        dbhelper = new Helper(context, Helper.DATABASE_NAME, null, Helper.DATABASE_VERSION);
        this.context = context;
        item_cache = new LruCache<>(5);
        demon_cache = new LruCache<>(5);
    }

    public static DatabaseManager get(Context context) {
        if (dbm == null) {
            dbm = new DatabaseManager(context);
        }
        return dbm;
    }

    private boolean checkIfTableIsPopulated(String table) {
        SQLiteDatabase db = dbhelper.getReadableDatabase();

        Cursor cursor =
            db.rawQuery("SELECT * FROM " + table, null);

        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }

    public void addMarker (CustomMarker customMarker) {
        ContentValues cv = new ContentValues();
        cv.put(Helper.KEY_MARKER_LATITUDE, customMarker.getLatitude());
        cv.put(Helper.KEY_MARKER_LONGITUDE, customMarker.getLongitude());
        cv.put(Helper.KEY_MARKER_DEMON, customMarker.getDemonID());
        cv.put(Helper.KEY_MARKER_DISTANCE, customMarker.getDistance());

        SQLiteDatabase db = dbhelper.getWritableDatabase();
        db.insert(Helper.MARKER_TABLE, null, cv);
    }

    public void deleteMarker (int markerID) {
        String where = Helper.KEY_MARKER_ID + "=" + markerID;
        SQLiteDatabase db = dbhelper.getWritableDatabase();

        db.delete(Helper.MARKER_TABLE, where, null);
    }

    public boolean checkAndPutGuest(String guestUUID) {
        SQLiteDatabase db = dbhelper.getWritableDatabase();

        String[] columns = {Helper.KEY_GUEST_ID, Helper.KEY_GUEST_LAST_ENCOUNTER};
        String where = Helper.KEY_GUEST_UUID + " = ?";
        String[] whereArgs = {guestUUID};

        boolean toCreate = true;
        int toUpdate = -1;

        Cursor c = db.query(Helper.GUEST_TABLE, columns, where, whereArgs, null, null, null);
        if (c != null) {
            if (c.getCount() > 0) {
                toCreate = false;
                int idcol = c.getColumnIndexOrThrow(Helper.KEY_GUEST_ID);
                int col = c.getColumnIndexOrThrow(Helper.KEY_GUEST_LAST_ENCOUNTER);
                c.moveToNext();
                long time = c.getLong(col);
                Log.d(TAG, "time retrieved is " + time + " current is " + System.currentTimeMillis());
                if (System.currentTimeMillis() >= time + 86400 * 1000) {
                    toUpdate = c.getInt(idcol);
                    Log.d(TAG, "must update row " + toUpdate);
                }
            }
            c.close();
        }

        if (toUpdate != -1) {
            String selection = Helper.KEY_GUEST_ID + " = " + toUpdate;
            ContentValues cv = new ContentValues();
            cv.put(Helper.KEY_GUEST_LAST_ENCOUNTER, System.currentTimeMillis());
            db.update(Helper.GUEST_TABLE, cv, selection, null);
            return true;
        }
        if (toCreate) {
            ContentValues cv = new ContentValues();
            cv.put(Helper.KEY_GUEST_UUID, guestUUID);
            cv.put(Helper.KEY_GUEST_LAST_ENCOUNTER, System.currentTimeMillis());
            db.insert(Helper.GUEST_TABLE, null, cv);
            return true;
        }
        return false;
    }

    public ArrayList<CustomMarker> getMarkers() {
        ArrayList<CustomMarker> customMarkers = new ArrayList<>();

        SQLiteDatabase db = dbhelper.getReadableDatabase();
        Cursor query = db.query(Helper.MARKER_TABLE, null, null, null, null, null, null);
        if (query != null) {
            int lat_column = query.getColumnIndexOrThrow(Helper.KEY_MARKER_LATITUDE);
            int long_column = query.getColumnIndexOrThrow(Helper.KEY_MARKER_LONGITUDE);
            int demon_column = query.getColumnIndexOrThrow(Helper.KEY_MARKER_DEMON);
            int id_column = query.getColumnIndexOrThrow(Helper.KEY_MARKER_ID);
            int dist_column = query.getColumnIndexOrThrow(Helper.KEY_MARKER_DISTANCE);

            while (query.moveToNext()) {
                double lat = query.getDouble(lat_column),
                        lng = query.getDouble(long_column),
                        distance = query.getDouble(dist_column);
                int demon = query.getInt(demon_column),
                        key = query.getInt(id_column);
                customMarkers.add(new CustomMarker(key, lat, lng, distance, demon));
            }
            query.close();
        }

        return customMarkers;
    }

    public Demon getDemon(int demonID) {
        Demon d = demon_cache.get(demonID);
        if (d == null) {
            SQLiteDatabase db = dbhelper.getReadableDatabase();
            int res = -1;

            String[] column = {Helper.KEY_DEMON_NAME, Helper.KEY_DEMON_IMAGE, Helper.KEY_DEMON_DESCRIPTION, Helper.KEY_DEMON_ARCANA, Helper.KEY_DEMON_CAUGHT, Helper.KEY_DEMON_QUIRK};
            String where = Helper.KEY_DEMON_ID + " = ?";
            String[] whereArgs = {String.format("%d", demonID)};
            String name, description, arcana, quirk;
            int caught;
            int image;
            Cursor query = db.query(Helper.DEMON_TABLE, column, where, whereArgs, null, null, null);

            if (query != null) {
                if (query.getCount() > 0) {
                    int namecol = query.getColumnIndex(Helper.KEY_DEMON_NAME);
                    int desccol = query.getColumnIndex(Helper.KEY_DEMON_DESCRIPTION);
                    int arcacol = query.getColumnIndex(Helper.KEY_DEMON_ARCANA);
                    int caughtcol = query.getColumnIndex(Helper.KEY_DEMON_CAUGHT);
                    int imagecol = query.getColumnIndex(Helper.KEY_DEMON_IMAGE);
                    int quirkcol = query.getColumnIndex(Helper.KEY_DEMON_QUIRK);
                    query.moveToNext();
                    name = query.getString(namecol);
                    description = query.getString(desccol);
                    arcana = query.getString(arcacol);
                    caught = query.getInt(caughtcol);
                    image = query.getInt(imagecol);
                    quirk = query.getString(quirkcol);
                    d = new Demon(demonID, name, description, arcana, quirk, caught, image);
                    demon_cache.put(demonID, d);
                }
                query.close();
            }
            Log.d(TAG, "Returned demon is " + d);
        }
        return d;
    }

    public int getDemonsCaught() {
        if (invalidateDemons) {
            SQLiteDatabase db = dbhelper.getReadableDatabase();
            String[] columns = {Helper.KEY_DEMON_ID};
            String where = Helper.KEY_DEMON_CAUGHT + " > 0";
            Cursor c = db.query(Helper.DEMON_TABLE, columns, where, null, null, null, null);
            if (c != null) {
                demonsCaught = c.getCount();
                c.close();
            }
            invalidateDemons = false;
        }

        return demonsCaught;
    }

    public ArrayList<Demon> getDemons(boolean onlyCaught) {
        ArrayList<Demon> array = new ArrayList<>();
        SQLiteDatabase db = dbhelper.getReadableDatabase();

        String[] columns = { Helper.KEY_DEMON_ID, Helper.KEY_DEMON_NAME, Helper.KEY_DEMON_DESCRIPTION,
                            Helper.KEY_DEMON_QUIRK, Helper.KEY_DEMON_ARCANA, Helper.KEY_DEMON_IMAGE, Helper.KEY_DEMON_CAUGHT};
        String where = null;
        if (onlyCaught) where = Helper.KEY_DEMON_CAUGHT + " > 0";

        Cursor c = db.query(Helper.DEMON_TABLE, columns, where, null, null, null, null);

        if (c != null) {
            int idcol = c.getColumnIndexOrThrow(Helper.KEY_DEMON_ID),
                    namecol = c.getColumnIndex(Helper.KEY_DEMON_NAME),
                    desccol = c.getColumnIndex(Helper.KEY_DEMON_DESCRIPTION),
                    quircol = c.getColumnIndex(Helper.KEY_DEMON_QUIRK),
                    arcacol = c.getColumnIndex(Helper.KEY_DEMON_ARCANA),
                    imagcol = c.getColumnIndex(Helper.KEY_DEMON_IMAGE),
                    caugcol = c.getColumnIndex(Helper.KEY_DEMON_CAUGHT);
            while (c.moveToNext()) {
                int id = c.getInt(idcol),
                        image = c.getInt(imagcol),
                        caught = c.getInt(caugcol);
                String name = c.getString(namecol),
                        description = c.getString(desccol),
                        quirk = c.getString(quircol),
                        arcana = c.getString(arcacol);
                array.add(new Demon(id, name, description, arcana, quirk, caught, image));
            }
            c.close();
        }
        return array;
    }

    public ArrayList<Item> getItems(boolean onlyHave) {
        ArrayList<Item> array = new ArrayList<>();
        SQLiteDatabase db = dbhelper.getReadableDatabase();

        String[] columns = { Helper.KEY_ITEM_ID, Helper.KEY_ITEM_NAME, Helper.KEY_ITEM_QUANTITY,
                            Helper.KEY_ITEM_DESCRIPTION, Helper.KEY_ITEM_USEABLE, Helper.KEY_ITEM_ACTION};
        String where = null;
        if (onlyHave) where = Helper.KEY_ITEM_QUANTITY + " > 0";

        Cursor c = db.query(Helper.ITEM_TABLE, columns, where, null, null, null, null);

        if (c != null) {
            int idcol = c.getColumnIndexOrThrow(Helper.KEY_ITEM_ID),
                    namecol = c.getColumnIndex(Helper.KEY_ITEM_NAME),
                    desccol = c.getColumnIndex(Helper.KEY_ITEM_DESCRIPTION),
                    quanticol = c.getColumnIndex(Helper.KEY_ITEM_QUANTITY),
                    usecol = c.getColumnIndex(Helper.KEY_ITEM_USEABLE),
                    actcol = c.getColumnIndex(Helper.KEY_ITEM_ACTION);
            while (c.moveToNext()) {
                int id = c.getInt(idcol),
                        use = c.getInt(usecol),
                        quantity = c.getInt(quanticol);
                String name = c.getString(namecol),
                        description = c.getString(desccol),
                        action = c.getString(actcol);
                array.add(new Item(id, name, description, action, use, quantity));
            }
            c.close();
        }
        return array;
    }

    public int getTotalDemons() {
        if (demonsNo == -1) {
            SQLiteDatabase db = dbhelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + Helper.DEMON_TABLE, null);
            demonsNo = cursor.getCount();
            cursor.close();
        }
        return demonsNo;
    }

    public boolean hasMarkers(){
        SQLiteDatabase db = dbhelper.getReadableDatabase();
        boolean response = false;
        Cursor c = db.rawQuery("SELECT * FROM " + Helper.MARKER_TABLE, null);
        if (c.getCount()>0) response = true;
        c.close();
        return response;
    }

    public void flushMarkers() {
        SQLiteDatabase db = dbhelper.getWritableDatabase();

        db.execSQL("DROP TABLE IF EXISTS " + Helper.MARKER_TABLE);
        db.execSQL(Helper.CREATE_MARKER_TABLE);
    }

    public Item getItem(int id) {
        Item it = item_cache.get(id);
        if (it == null) {
            SQLiteDatabase db = dbhelper.getReadableDatabase();

            String[] column = {Helper.KEY_ITEM_NAME,
                    Helper.KEY_ITEM_DESCRIPTION,
                    Helper.KEY_ITEM_ACTION,
                    Helper.KEY_ITEM_USEABLE,
                    Helper.KEY_ITEM_QUANTITY};
            String where = Helper.KEY_ITEM_ID + " = ?";
            String[] whereArgs = {String.format("%d", id)};
            String name, desc, act;
            int use, quan;

            Cursor query = db.query(Helper.ITEM_TABLE, column, where, whereArgs, null, null, null);
            if (query != null) {
                if (query.getCount() > 0) {
                    int namecol = query.getColumnIndexOrThrow(Helper.KEY_ITEM_NAME),
                            desccol = query.getColumnIndex(Helper.KEY_ITEM_DESCRIPTION),
                            actcol = query.getColumnIndex(Helper.KEY_ITEM_ACTION),
                            usecol = query.getColumnIndex(Helper.KEY_ITEM_USEABLE),
                            quanticol = query.getColumnIndex(Helper.KEY_ITEM_QUANTITY);
                    query.moveToNext();
                    name = query.getString(namecol);
                    desc = query.getString(desccol);
                    act = query.getString(actcol);
                    use = query.getInt(usecol);
                    quan = query.getInt(quanticol);
                    it = new Item(id, name, desc, act, use, quan);
                    item_cache.put(id, it);
                }
                query.close();
            }
        }
        return it;
    }

    public void changeItemAmount(int item, int amount) {
        SQLiteDatabase db = dbhelper.getWritableDatabase();

        Item it = item_cache.get(item);

        if (it == null) it = getItem(item);

        int quantity = it.getQuantity();

        String where = Helper.KEY_ITEM_ID + " = ?";
        String[] whereArgs = { String.format("%d", item) };

        if (quantity < 0 || quantity+amount < 0) throw new RuntimeException("Illegal item quantity");
        ContentValues cv = new ContentValues();
        cv.put(Helper.KEY_ITEM_QUANTITY, quantity+amount);
        db.update(Helper.ITEM_TABLE, cv, where, whereArgs);

        it.setQuantity(quantity + amount);
        item_cache.put(item, it);
    }

    public void addRemoveCaughtDemon(int demonID, boolean removeInstead) {
        invalidateDemons = true;
        SQLiteDatabase db = dbhelper.getWritableDatabase();

        Demon d = demon_cache.get(demonID);

        if (d == null) d = getDemon(demonID);

        int caught = d.getCaught();
        if (removeInstead) caught -= 1;
        else caught += 1;

        String where = Helper.KEY_DEMON_ID + " = ?";
        String[] whereArgs = { String.format("%d", demonID) };

        ContentValues cv = new ContentValues();
        cv.put(Helper.KEY_DEMON_CAUGHT, caught);
        db.update(Helper.DEMON_TABLE, cv, where, whereArgs);

        d.setCaught(caught);
        demon_cache.put(demonID, d);
    }

    /*
        Items emoticon:
        \uD83D\uDD2E dark globe
        \uD83C\uDF6B protein snack
        \uD83D\uDCBE upgrade
        \uD83D\uDD0B battery
        \uD83C\uDF75 tea
        \uD83C\uDF6E budeeno
        \uD83D\uDCB4 soldi
     */

    public boolean populateItemsDB() {
        if (checkIfTableIsPopulated(Helper.ITEM_TABLE)) return false;
        SQLiteDatabase db = dbhelper.getWritableDatabase();

        String i = Helper.KEY_ITEM_ID, a = Helper.KEY_ITEM_NAME, b = Helper.KEY_ITEM_DESCRIPTION,
                c = Helper.KEY_ITEM_ACTION, d = Helper.KEY_ITEM_USEABLE, e = Helper.KEY_ITEM_QUANTITY,
                name;

        ContentValues cv = new ContentValues();

        //Dark Globe
        name = String.format("\uD83D\uDD2E %s", context.getString(R.string.dark_globe));
        cv.put(i, Item.GLOBE);
        cv.put(a, name);
        cv.put(b, context.getString(R.string.dark_globe_desc));
        cv.put(d, Item.NOT_USABLE);
        cv.put(e, 0);
        db.insertOrThrow(Helper.ITEM_TABLE, null, cv);

        //Protein Snack
        name = String.format("\uD83C\uDF6B %s", context.getString(R.string.charisma_proteins));
        cv.put(i, Item.SNACK);
        cv.put(a, name);
        cv.put(b, context.getString(R.string.charisma_proteins_desc));
        cv.put(c, context.getString(R.string.ate));
        cv.put(d, Item.INCR_CHARISMA);
        cv.put(e, 0);
        db.insertOrThrow(Helper.ITEM_TABLE, null, cv);

        //Charm Infusion
        name = String.format("\uD83C\uDF75 %s", context.getString(R.string.charm_infusion));
        cv.put(i, Item.INFUSE);
        cv.put(a, name);
        cv.put(b, context.getString(R.string.charm_infusion_desc));
        cv.put(c, context.getString(R.string.drank));
        cv.put(d, Item.INCR_CHARM);
        cv.put(e, 0);
        db.insertOrThrow(Helper.ITEM_TABLE, null, cv);

        //Luck Pudding
        name = String.format("\uD83C\uDF6E %s", context.getString(R.string.luck_pudding));
        cv.put(i, Item.PUDDING);
        cv.put(a, name);
        cv.put(b, context.getString(R.string.luck_pudding_desc));
        cv.put(c, context.getString(R.string.ate));
        cv.put(d, Item.INCR_LUCK);
        cv.put(e, 0);
        db.insertOrThrow(Helper.ITEM_TABLE, null, cv);

        //Battery Pack
        name = String.format("\uD83D\uDD0B %s", context.getString(R.string.battery_pack));
        cv.put(i, Item.BATTERY);
        cv.put(a, name);
        cv.put(b, context.getString(R.string.battery_pack_description));
        cv.put(d, Item.NOT_USABLE);
        cv.put(e, 0);
        db.insertOrThrow(Helper.ITEM_TABLE, null, cv);

        //Upgrade Software
        name = String.format("\uD83D\uDCBE %s", context.getString(R.string.upgraded_code));
        cv.put(i, Item.UPGRADE);
        cv.put(a, name);
        cv.put(b, context.getString(R.string.upgraded_code_desc));
        cv.put(d, Item.NOT_USABLE);
        cv.put(e, 0);
        db.insertOrThrow(Helper.ITEM_TABLE, null, cv);

        return true;
    }

    public boolean populateDemonDB() {
        if (checkIfTableIsPopulated(Helper.DEMON_TABLE)) return false;
        SQLiteDatabase db = dbhelper.getWritableDatabase();

        String a = Helper.KEY_DEMON_NAME, b = Helper.KEY_DEMON_DESCRIPTION,
                c = Helper.KEY_DEMON_ARCANA, d = Helper.KEY_DEMON_IMAGE, e = Helper.KEY_DEMON_CAUGHT,
                f = Helper.KEY_DEMON_QUIRK;

        ContentValues cv = new ContentValues();

        //Abaddon
        cv.put(a, context.getString(R.string.abaddon));
        cv.put(b, context.getString(R.string.abaddon_description));
        cv.put(c, context.getString(R.string.tower_arcana));
        cv.put(d, R.drawable.abaddon);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.abaddon_quirk));
        db.insertOrThrow(Helper.DEMON_TABLE, null, cv);

        //Ares
        cv.put(a, context.getString(R.string.ares));
        cv.put(b, context.getString(R.string.ares_description));
        cv.put(c, context.getString(R.string.chariot_arcana));
        cv.put(d, R.drawable.ares);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.ares_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        //Alice
        cv.put(a, context.getString(R.string.alice));
        cv.put(b, context.getString(R.string.alice_description));
        cv.put(c, context.getString(R.string.death_arcana));
        cv.put(d, R.drawable.alice);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.alice_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        //Belphegor
        cv.put(a, context.getString(R.string.belphegor));
        cv.put(b, context.getString(R.string.belphegor_description));
        cv.put(c, context.getString(R.string.devil_arcana));
        cv.put(d, R.drawable.belphegor);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.belphegor_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Thoth
        cv.put(a, context.getString(R.string.thoth));
        cv.put(b, context.getString(R.string.thoth_description));
        cv.put(c, context.getString(R.string.emperor_arcana));
        cv.put(d, R.drawable.thoth);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.thoth_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Isis
        cv.put(a, context.getString(R.string.isis));
        cv.put(b, context.getString(R.string.isis_description));
        cv.put(c, context.getString(R.string.empress_arcana));
        cv.put(d, R.drawable.isis);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.isis_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Orpheus
        cv.put(a, context.getString(R.string.orpheus));
        cv.put(b, context.getString(R.string.orpheus_description));
        cv.put(c, context.getString(R.string.fool_arcana));
        cv.put(d, R.drawable.orpheus);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.orpheus_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Lachesis
        cv.put(a, context.getString(R.string.lachesis));
        cv.put(b, context.getString(R.string.lachesis_description));
        cv.put(c, context.getString(R.string.fortune_arcana));
        cv.put(d, R.drawable.lachesis);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.lachesis_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Berith
        cv.put(a, context.getString(R.string.berith));
        cv.put(b, context.getString(R.string.berith_description));
        cv.put(c, context.getString(R.string.hangedman_arcana));
        cv.put(d, R.drawable.berith);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.berith_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Nidhoggr
        cv.put(a, context.getString(R.string.nidhoggr));
        cv.put(b, context.getString(R.string.nidhoggr_description));
        cv.put(c, context.getString(R.string.hermit_arcana));
        cv.put(d, R.drawable.nidhoggr);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.nidhoggr_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Cerberus
        cv.put(a, context.getString(R.string.cerberus));
        cv.put(b, context.getString(R.string.cerberus_description));
        cv.put(c, context.getString(R.string.hierophant_arcana));
        cv.put(d, R.drawable.cerberus);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.cerberus_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Metatron
        cv.put(a, context.getString(R.string.metatron));
        cv.put(b, context.getString(R.string.metatron_description));
        cv.put(c, context.getString(R.string.judgement_arcana));
        cv.put(d, R.drawable.metatron);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.metatron_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Melchizedek
        cv.put(a, context.getString(R.string.melchizedek));
        cv.put(b, context.getString(R.string.melchizedek_description));
        cv.put(c, context.getString(R.string.justice_arcana));
        cv.put(d, R.drawable.melchizedek);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.melchizedek_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Ishtar
        cv.put(a, context.getString(R.string.ishtar));
        cv.put(b, context.getString(R.string.ishtar_description));
        cv.put(c, context.getString(R.string.lovers_arcana));
        cv.put(d, R.drawable.ishtar);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.ishtar_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Jack Frost
        cv.put(a, context.getString(R.string.jackfrost));
        cv.put(b, context.getString(R.string.jackfrost_description));
        cv.put(c, context.getString(R.string.magician_arcana));
        cv.put(d, R.drawable.jackfrost);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.jackfrost_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Alraune
        cv.put(a, context.getString(R.string.alraune));
        cv.put(b, context.getString(R.string.alraune_description));
        cv.put(c, context.getString(R.string.moon_arcana));
        cv.put(d, R.drawable.alraune);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.alraune_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // High Pixie
        cv.put(a, context.getString(R.string.highpixie));
        cv.put(b, context.getString(R.string.highpixie_description));
        cv.put(c, context.getString(R.string.priestess_arcana));
        cv.put(d, R.drawable.highpixie);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.highpixie_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Neko Shogun
        cv.put(a, context.getString(R.string.nekoshogun));
        cv.put(b, context.getString(R.string.nekoshogun_description));
        cv.put(c, context.getString(R.string.star_arcana));
        cv.put(d, R.drawable.nekoshogun);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.nekoshogun_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Valkyrie
        cv.put(a, context.getString(R.string.valkyrie));
        cv.put(b, context.getString(R.string.valkyrie_description));
        cv.put(c, context.getString(R.string.strength_arcana));
        cv.put(d, R.drawable.valkyrie);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.valkyrie_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Yatagarasu
        cv.put(a, context.getString(R.string.yatagarasu));
        cv.put(b, context.getString(R.string.yatagarasu_description));
        cv.put(c, context.getString(R.string.sun_arcana));
        cv.put(d, R.drawable.yatagarasu);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.yatagarasu_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Vishnu
        cv.put(a, context.getString(R.string.vishnu));
        cv.put(b, context.getString(R.string.vishnu_description));
        cv.put(c, context.getString(R.string.temperance_arcana));
        cv.put(d, R.drawable.vishnu);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.vishnu_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        // Izanagi-no-Okami
        cv.put(a, context.getString(R.string.izanaginookami));
        cv.put(b, context.getString(R.string.izanaginookami_description));
        cv.put(c, context.getString(R.string.world_arcana));
        cv.put(d, R.drawable.izanaginookami);
        cv.put(e, 0);
        cv.put(f, context.getString(R.string.izanaginookami_quirk));
        db.insert(Helper.DEMON_TABLE, null, cv);

        return true;
    }

    public void reset() {
        SQLiteDatabase db = dbhelper.getWritableDatabase();

        db.execSQL("DROP TABLE IF EXISTS " + Helper.DEMON_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + Helper.ITEM_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + Helper.MARKER_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + Helper.GUEST_TABLE);
        dbhelper.onCreate(db);
    }

    public class Helper extends SQLiteOpenHelper {

        public static final String DATABASE_NAME = "database.db";
        public static final int DATABASE_VERSION = 4;

        // Guest table
        public static final String GUEST_TABLE = "GUEST_TABLE";
        public static final String KEY_GUEST_ID = "GUEST_ID";
        public static final String KEY_GUEST_UUID = "GUEST_UUID";
        public static final String KEY_GUEST_LAST_ENCOUNTER = "GUEST_LAST_ENCOUNTER";

        // Marker table
        public static final String MARKER_TABLE = "MARKER_TABLE";
        public static final String KEY_MARKER_ID = "MARKER_ID";
        public static final String KEY_MARKER_LATITUDE = "MARKER_LATITUDE";
        public static final String KEY_MARKER_LONGITUDE = "MARKER_LONGITUDE";
        public static final String KEY_MARKER_DEMON = "MARKER_DEMON";
        public static final String KEY_MARKER_DISTANCE = "MARKER_DISTANCE";

        // Demon table
        public static final String DEMON_TABLE = "DEMON_TABLE";
        public static final String KEY_DEMON_ID = "DEMON_ID";
        public static final String KEY_DEMON_NAME = "DEMON_NAME";
        public static final String KEY_DEMON_ARCANA = "DEMON_ARCANA";
        public static final String KEY_DEMON_DESCRIPTION = "DEMON_DESCRIPTION";
        public static final String KEY_DEMON_IMAGE = "DEMON_IMAGE";
        public static final String KEY_DEMON_CAUGHT = "DEMON_CAUGHT";
        public static final String KEY_DEMON_QUIRK = "DEMON_QUIRK";

        // Items table
        public static final String ITEM_TABLE = "ITEM_TABLE";
        public static final String KEY_ITEM_ID = "ITEM_ID";
        public static final String KEY_ITEM_NAME = "ITEM_NAME";
        public static final String KEY_ITEM_DESCRIPTION = "ITEM_DESC";
        public static final String KEY_ITEM_QUANTITY = "ITEM_QUANTITY";
        public static final String KEY_ITEM_USEABLE = "ITEM_USEABLE";
        public static final String KEY_ITEM_ACTION = "ITEM_ACTION";

        // DB Creation strings
        public static final String CREATE_MARKER_TABLE =
                "CREATE TABLE " + MARKER_TABLE + " ("
                        + KEY_MARKER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + KEY_MARKER_LATITUDE + " DOUBLE NOT NULL, "
                        + KEY_MARKER_LONGITUDE + " DOUBLE NOT NULL, "
                        + KEY_MARKER_DISTANCE + " DOUBLE NOT NULL, "
                        + KEY_MARKER_DEMON + " INTEGER NOT NULL, "
                        + "FOREIGN KEY (" + KEY_MARKER_DEMON + ") REFERENCES "
                        + DEMON_TABLE + " (" + KEY_DEMON_ID + "));";
        public static final String CREATE_DEMON_TABLE =
                "CREATE TABLE " + DEMON_TABLE + " ("
                        + KEY_DEMON_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + KEY_DEMON_NAME + " TEXT NOT NULL, "
                        + KEY_DEMON_ARCANA + " TEXT NOT NULL, "
                        + KEY_DEMON_DESCRIPTION + " TEXT NOT NULL, "
                        + KEY_DEMON_IMAGE + " INTEGER NOT NULL, "
                        + KEY_DEMON_QUIRK + " TEXT NOT NULL, "
                        + KEY_DEMON_CAUGHT + " INTEGER NOT NULL);";
        public static final String CREATE_ITEM_TABLE =
                "CREATE TABLE " + ITEM_TABLE + " ("
                        + KEY_ITEM_ID + " INTEGER PRIMARY KEY, "
                        + KEY_ITEM_NAME + " TEXT NOT NULL, "
                        + KEY_ITEM_DESCRIPTION + " TEXT NOT NULL, "
                        + KEY_ITEM_USEABLE + " INTEGER NOT NULL, "
                        + KEY_ITEM_ACTION + " TEXT, "
                        + KEY_ITEM_QUANTITY + " INTEGER NOT NULL);";
        public static final String CREATE_GUEST_TABLE =
                "CREATE TABLE " + GUEST_TABLE + " ("
                        + KEY_GUEST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + KEY_GUEST_UUID + " TEXT NOT NULL, "
                        + KEY_GUEST_LAST_ENCOUNTER + " INTEGER NOT NULL);";

        public Helper (Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_DEMON_TABLE);
            db.execSQL(CREATE_ITEM_TABLE);
            db.execSQL(CREATE_MARKER_TABLE);
            db.execSQL(CREATE_GUEST_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading from version " +
                    oldVersion + " to " +
                    newVersion + ", which will destroy all old data");

            db.execSQL("DROP TABLE IF EXISTS " + DEMON_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + ITEM_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + MARKER_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + GUEST_TABLE);
            onCreate(db);
        }
    }
}
