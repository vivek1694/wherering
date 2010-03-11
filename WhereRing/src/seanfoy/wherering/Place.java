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
import seanfoy.Greenspun.Disposable;
import seanfoy.Greenspun.Func1;
import seanfoy.Greenspun.ROIterator;
public class Place {
    public Place(Location l, float radius, RingerMode ringerMode, String name) {
        this.location = l;
        this.radius = radius;
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
        l.setLatitude(Location.convert(c.getString(c.getColumnIndex("latitude"))));
        l.setLongitude(Location.convert(c.getString(c.getColumnIndex("longitude"))));
        Place result =
            new Place(
                l,
                c.getFloat(c.getColumnIndex("radius")),
                RingerMode.fromInt(
                    c.getInt(c.getColumnIndex("ringer_mode"))),
                c.getString(c.getColumnIndex("name")));
        return result;
    }
    
    private static final String [] columnList =
        new String [] {
            "latitude",
            "longitude",
            "radius",
            "name",
            "ringer_mode"};
    
    public static Place fetch(DBAdapter adapter, final Location key) {
        Place template = new Place(key, 0, RingerMode.normal, "");
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
                return new CursorIterator<Place>(c) {
                    public Place extractCurrent(Cursor c) {
                        return retrieve(c);
                    }
                };
            }
        };
    }

    private ContentValues makeContentValues() {
        ContentValues initialValues = new ContentValues();
        initialValues.put("latitude", Location.convert(location.getLatitude(), Location.FORMAT_SECONDS));
        initialValues.put("longitude", Location.convert(location.getLongitude(), Location.FORMAT_SECONDS));
        initialValues.put("name", name);
        initialValues.put("ringer_mode", ringerMode.ringer_mode);
        initialValues.put("radius", radius);
        return initialValues;
    }
    
    public static void ddl(SQLiteDatabase db) {
        db.execSQL(
                String.format(
                    "create table %s (" +
                    " latitude text," +
                    " longitude text," +
                    " name text," +
                    " ringer_mode integer," +
                    " radius real," +
                    " constraint %s_PK primary key (latitude, longitude))",
                    TABLE_NAME, TABLE_NAME));
    }

    public static void teardownDDL(SQLiteDatabase db) {
        db.execSQL(
                String.format(
                        "drop table if exists %s",
                        TABLE_NAME));
    }
    
    public static final String TABLE_NAME = "Places";
    
    public final Location location;
    public RingerMode ringerMode;
    public String name;
    public float radius = 20;
    
    private Map<String, Object> getValueEquality() {
        HashMap<String, Object> ve = new HashMap<String, Object>();
        ve.put("latitude", Location.convert(location.getLatitude(), Location.FORMAT_SECONDS));
        ve.put("longitude", Location.convert(location.getLongitude(), Location.FORMAT_SECONDS));
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
    
    abstract static class CursorIterator<T> extends ROIterator<T> implements Disposable<RuntimeException> {
        Cursor c;
        public CursorIterator(Cursor c) {
            this.c = c;
            hn = c.moveToFirst();
        }
        boolean hn;
        public boolean hasNext() {
            return hn;
        }
        public final T next() {
            T result = extractCurrent(c);
            hn = c.moveToNext();
            //this is not good enough
            // (no guarantee that clients
            //  will read to end)
            //ResourceManagement aspect
            // provides some syntactic
            // sugar for that.
            if (!hn) c.close();
            return result;
        }
        public abstract T extractCurrent(Cursor c);
        public void close() {
            if (!c.isClosed()) c.close();
        }
    }
}

