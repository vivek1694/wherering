package seanfoy.wherering;

import seanfoy.Greenspun.Func1;
import android.content.Context;
import android.database.*;
import android.database.sqlite.*;
import android.location.Location;

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
    
    public <T> T withDB(Func1<SQLiteDatabase, T> f) {
        return f.f(db);
    }
    
    public static <T> T withDBAdapter(Context ctx, Func1<DBAdapter, T> f) {
        DBAdapter a = new DBAdapter(ctx);
        try {
            a.open();
            return f.f(a);
        }
        finally {
            a.close();
        }
    }
    
    public static String makeWhereClause(Location l) {
        return String.format(
                "latitude = %f and longitude = %f",
                l.getLatitude(),
                l.getLongitude());
    }
    
    private static class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VER);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Place.ddl(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Place.teardownDDL(db);
            onCreate(db);
        }
    }
    
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private static final String DB_NAME = "WhereRing";
    private static final int DB_VER = 1;
    private Context ctx;
}
