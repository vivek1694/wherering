package seanfoy.wherering;

import java.util.Iterator;

import seanfoy.Greenspun.Pair;
import seanfoy.Greenspun.ROIterator;
import android.content.ContentValues;
import android.content.Context;
import android.database.*;
import android.database.sqlite.*;
import android.location.Location;
import android.media.AudioManager;

public class DBAdapter {
    public DBAdapter(Context ctx) {
        this.ctx = ctx;
    }
    
    public void open() throws SQLException {
        dbHelper = new DatabaseHelper(ctx);
        db = dbHelper.getWritableDatabase();
    }
    
    public void close() {
        dbHelper.close();
    }
    
    public Location createPlace(Location l, int ringerMode) {
        return createPlace(db, l, ringerMode);
    }
    
    public static Location createPlace(SQLiteDatabase db, Location l, int ringerMode) {
        ContentValues initialValues = makeContentValues(l, ringerMode);
        db.insert(TABLE_NAME, null, initialValues);
        return l;
    }

    private static ContentValues makeContentValues(Location l, int ringerMode) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("latitude", l.getLatitude());
        initialValues.put("longitude", l.getLongitude());
        initialValues.put("ringer_mode", ringerMode);
        return initialValues;
    }
    
    public void deletePlace(Location l) {
        db.delete(
                TABLE_NAME,
                makeWhereClause(l),
                null);
    }

    private String makeWhereClause(Location l) {
        return String.format(
                "latitude = %f and longitude = %f",
                l.getLatitude(),
                l.getLongitude());
    }
    
    public int updatePlace(Location l, int ringerMode) {
        ContentValues args = makeContentValues(l, ringerMode);
        return db.update(TABLE_NAME, args, makeWhereClause(l), null);
    }
    
    public Cursor fetchPlaces() {
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
    
    public Iterable<Pair<Location, Integer>> allPlaces() {
        return new Iterable<Pair<Location, Integer>>() {
            public Iterator<Pair<Location, Integer>> iterator() {
                final Cursor c = fetchPlaces();
                return new ROIterator<Pair<Location, Integer>>() {
                    boolean hn = c.moveToFirst();
                    public boolean hasNext() {
                        return hn;
                    }

                    public Pair<Location, Integer> next() {
                        Location l = new Location("whatever");
                        l.setLatitude(c.getDouble(c.getColumnIndex("latitude")));
                        l.setLongitude(c.getDouble(c.getColumnIndex("longitude")));
                        Pair<Location, Integer> result =
                            new Pair<Location, Integer>(
                                    l,
                                    c.getInt(c.getColumnIndex("ringer_mode")));
                        hn = c.moveToNext();
                        return result;
                    }
                };
            }
        };
    }
    
    private static class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VER);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
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

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(
                    String.format(
                            "drop table if exists %s",
                            TABLE_NAME));
            onCreate(db);
        }
    }
    
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private static final String DB_NAME = "WhereRing";
    private static final int DB_VER = 1;
    private static final String TABLE_NAME = "Places";
    private Context ctx;
}
