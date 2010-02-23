package seanfoy.wherering;

import java.util.Iterator;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.media.AudioManager;
import android.util.Log;
import seanfoy.Greenspun.Func1;
import seanfoy.Greenspun.Pair;
import seanfoy.Greenspun.ROIterator;

public class Place extends Pair<Location, Integer> {
    public Place(Location l, Integer ringerMode) {
        super(l, ringerMode);
    }
    
    public void upsert(DBAdapter adapter) {
        adapter.withDB(
            new Func1<SQLiteDatabase, Void>() {
                public Void f(SQLiteDatabase db) {
                    ContentValues V = makeContentValues(fst, snd);
                    db.replace(TABLE_NAME, null, V);                   
                    return null;
                }
            });
    }
    
    public void delete(DBAdapter adapter) {
        deletePlace(adapter, fst);
    }
    
    private static Place makePlace(final Cursor c) {
        Location l = new Location("whatever");
        l.setLatitude(c.getDouble(c.getColumnIndex("latitude")));
        l.setLongitude(c.getDouble(c.getColumnIndex("longitude")));
        Place result =
            new Place(
                    l,
                    c.getInt(c.getColumnIndex("ringer_mode")));
        return result;
    }

    public static void createPlace(SQLiteDatabase db, final Location l, final int ringerMode) {
        ContentValues initialValues = makeContentValues(l, ringerMode);
        db.insert(TABLE_NAME, null, initialValues);
    }
    
    public static Location createPlace(DBAdapter adapter, final Location l, final int ringerMode) {
        adapter.withDB(
            new Func1<SQLiteDatabase, Void>() {
                public Void f(SQLiteDatabase db) {
                    createPlace(db, l, ringerMode);
                    return null;
                }
            });
        return l;
    }

    private static ContentValues makeContentValues(Location l, int ringerMode) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("latitude", l.getLatitude());
        initialValues.put("longitude", l.getLongitude());
        initialValues.put("ringer_mode", ringerMode);
        return initialValues;
    }
    
    public static void deletePlace(DBAdapter adapter, final Location l) {
        adapter.withDB(
            new Func1<SQLiteDatabase, Void>() {
                public Void f(SQLiteDatabase db) {
                    db.delete(
                            TABLE_NAME,
                            DBAdapter.makeWhereClause(l),
                            null);
                    return null;
                }
            });
    }

    public static int updatePlace(DBAdapter adapter, final Location l, final int ringerMode) {
        return
            adapter.withDB(
                new Func1<SQLiteDatabase, Integer>() {
                    public Integer f(SQLiteDatabase db) {
                        ContentValues args = makeContentValues(l, ringerMode);
                        return
                            db.update(
                                TABLE_NAME,
                                args,
                                DBAdapter.makeWhereClause(l),
                                null);
                    }
                });
    }
    
    public static Place fetch(DBAdapter adapter, final Location key) {
        return
            adapter.withDB(
                new Func1<SQLiteDatabase, Place>() {
                    public Place f(SQLiteDatabase db) {
                        Cursor c =
                            db.query(
                                TABLE_NAME,
                                new String [] {
                                    "latitude",
                                    "longitude",
                                    "ringer_mode"},
                                DBAdapter.makeWhereClause(key),
                                null,
                                null,
                                null,
                                null);
                        try {
                            c.moveToFirst();
                            return makePlace(c);
                        }
                        finally {
                            c.close();
                        }
                    }
                });
    }
    
    public static Cursor fetchPlaces(DBAdapter adapter) {
        return
            adapter.withDB(
                new Func1<SQLiteDatabase, Cursor>() {
                    public Cursor f(SQLiteDatabase db) {
                        return
                            db.query(
                                TABLE_NAME,
                                new String [] {
                                        "latitude",
                                        "longitude",
                                        "ringer_mode"},
                                null,
                                null,
                                null,
                                null,
                                null);
                    }
                });
    }
    
    public static Iterable<Place> allPlaces(final DBAdapter adapter) {
        return new Iterable<Place>() {
            public Iterator<Place> iterator() {
                final Cursor c = fetchPlaces(adapter);
                return new ROIterator<Place>() {
                    boolean hn = c.moveToFirst();
                    public boolean hasNext() {
                        return hn;
                    }

                    public Place next() {
                        Place result = makePlace(c);
                        hn = c.moveToNext();
                        if (!hn) c.close();
                        return result;
                    }
                };
            }
        };
    }
    
    public static void ddl(SQLiteDatabase db) {
        db.execSQL(
                String.format(
                    "create table %s (" +
                    " latitude double precision," +
                    " longitude double precision," +
                    " ringer_mode integer," +
                    " constraint %s_PK primary key (latitude, longitude))",
                    TABLE_NAME, TABLE_NAME));
        int homeRing = AudioManager.RINGER_MODE_NORMAL;
        int parkRing = AudioManager.RINGER_MODE_VIBRATE;
        Location l = new Location("whatever");
        l.setAccuracy(WRBroadcastReceiver.radiusM);
        // 1805 Park
        l.setLatitude(38.629205);
        l.setLongitude(-90.226615);
        createPlace(db, l, parkRing);
        l = new Location(l);
        // 2050 Lafayette
        l.setLatitude(38.616951);
        l.setLongitude(-90.21152);
        createPlace(db, l, homeRing);
    }

    public static void teardownDDL(SQLiteDatabase db) {
        db.execSQL(
                String.format(
                        "drop table if exists %s",
                        TABLE_NAME));
    }
    
    private static final String TABLE_NAME = "Places";
}
