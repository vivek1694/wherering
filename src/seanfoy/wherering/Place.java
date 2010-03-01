/*
 * Copyright 2010 Sean M. Foy
 * 
 * This file is part of WhereRing.
 *
 *  WhereRing is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  WhereRing is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with WhereRing.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package seanfoy.wherering;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.media.AudioManager;
import seanfoy.Greenspun;
import seanfoy.Greenspun.Func1;
import seanfoy.Greenspun.ROIterator;
public class Place {
    public Place(Location l, RingerMode ringerMode, String name) {
        this.location = l;
        this.ringerMode = ringerMode;
        this.name = name;
    }
    
    public void upsert(DBAdapter adapter) {
        adapter.upsert(TABLE_NAME, makeContentValues());
    }
    
    public void delete(DBAdapter adapter) {
        adapter.delete(TABLE_NAME, DBAdapter.makeWhereClause(getValueEquality()));
    }
    
    private static Place retrieve(final Cursor c) {
        Location l = new Location("whatever");
        l.setLatitude(c.getDouble(c.getColumnIndex("latitude")));
        l.setLongitude(c.getDouble(c.getColumnIndex("longitude")));
        Place result =
            new Place(
                l,
                RingerMode.fromInt(
                    c.getInt(c.getColumnIndex("ringer_mode"))),
                c.getString(c.getColumnIndex("name")));
        return result;
    }
    
    private static final String [] columnList =
        new String [] {
            "latitude",
            "longitude",
            "name",
            "ringer_mode"};
    
    public static Place fetch(DBAdapter adapter, final Location key) {
        Place template = new Place(key, RingerMode.normal, "");
        try {
            return
                adapter.withCursor(
                    TABLE_NAME,
                    columnList,
                    DBAdapter.makeWhereClause(template.getValueEquality()),
                    new Func1<Cursor, Place>() {
                        public Place f(Cursor c) {
                            c.moveToFirst();
                            return retrieve(c);
                        }
                    });
        }
        catch (CursorIndexOutOfBoundsException e) {
            return null;
        }
    }
    
    public static Cursor fetchPlaces(DBAdapter adapter) {
        return
            adapter.withDB(
                new Func1<SQLiteDatabase, Cursor>() {
                    public Cursor f(SQLiteDatabase db) {
                        return
                            db.query(
                                TABLE_NAME,
                                columnList,
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
                        Place result = retrieve(c);
                        hn = c.moveToNext();
                        if (!hn) c.close();
                        return result;
                    }
                };
            }
        };
    }

    private ContentValues makeContentValues() {
        ContentValues initialValues = new ContentValues();
        initialValues.put("latitude", location.getLatitude());
        initialValues.put("longitude", location.getLongitude());
        initialValues.put("name", name);
        initialValues.put("ringer_mode", ringerMode.ringer_mode);
        return initialValues;
    }
    
    public static void ddl(SQLiteDatabase db) {
        db.execSQL(
                String.format(
                    "create table %s (" +
                    " latitude double precision," +
                    " longitude double precision," +
                    " name text," +
                    " ringer_mode integer," +
                    " constraint %s_PK primary key (latitude, longitude))",
                    TABLE_NAME, TABLE_NAME));
        int homeRing = AudioManager.RINGER_MODE_NORMAL;
        int parkRing = AudioManager.RINGER_MODE_VIBRATE;
        Location l = new Location("whatever");
        l.setAccuracy(WRService.radiusM);
        // 1805 Park
        l.setLatitude(38.629205);
        l.setLongitude(-90.226615);
        createPlace(db, l, parkRing, "Park Avenue");
        l = new Location(l);
        // 2050 Lafayette
        l.setLatitude(38.616951);
        l.setLongitude(-90.21152);
        createPlace(db, l, homeRing, "Lafayette Avenue");
    }
    
    private static Place createPlace(SQLiteDatabase db, final Location l, final int ringerMode, final String name) {
        Place result = new Place(l, RingerMode.fromInt(ringerMode), name);
        ContentValues initialValues = result.makeContentValues();
        db.insert(TABLE_NAME, null, initialValues);
        return result;
    }

    public static void teardownDDL(SQLiteDatabase db) {
        db.execSQL(
                String.format(
                        "drop table if exists %s",
                        TABLE_NAME));
    }
    
    private static final String TABLE_NAME = "Places";
    
    public final Location location;
    public RingerMode ringerMode;
    public String name;
    
    private Map<String, Object> getValueEquality() {
        HashMap<String, Object> ve = new HashMap<String, Object>();
        ve.put("latitude", location.getLatitude());
        ve.put("longitude", location.getLongitude());
        return ve;
    }
    private static Func1<Place, Map<String, ?>> getVE =
        new Func1<Place, Map<String, ?>>() {
            public Map<String, ?> f(Place p) {
                return p.getValueEquality();
            }
        };
    @Override
    public boolean equals(Object o) {
        return Greenspun.equals(this, o, getVE);
    }
    @Override
    public int hashCode() {
        return Greenspun.hashCode(this, getVE);
    }
    @Override
    public String toString() {
        return Greenspun.toString(this, getVE);
    }
    
    public enum RingerMode {
        normal(AudioManager.RINGER_MODE_NORMAL),
        silent(AudioManager.RINGER_MODE_SILENT),
        vibrate(AudioManager.RINGER_MODE_VIBRATE);
        
        private RingerMode(int ringer_mode) {
            this.ringer_mode = ringer_mode;
        }
        public final int ringer_mode;
        public static RingerMode fromInt(int rm) {
            for (RingerMode m : values()) {
                if (m.ringer_mode == rm) return m;
            }
            throw new IllegalArgumentException();
        }
    }
}

