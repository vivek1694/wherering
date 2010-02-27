package seanfoy.wherering;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import seanfoy.wherering.Place.RingerMode;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class PlaceEdit extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.place_edit);
        db = new DBAdapter(getApplicationContext());
        db.open();
        
        Spinner rm = (Spinner)findViewById(R.id.ringer_mode);
        ArrayAdapter<RingerMode> ringers =
            new ArrayAdapter<RingerMode>(getApplicationContext(), R.layout.list_item);
        for (RingerMode m : RingerMode.values()) {
            ringers.add(m);
        }
        rm.setAdapter(ringers);

        fillData();
        
        findViewById(R.id.done).
            setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        try {
                            PlaceEdit.this.setResult(RESULT_OK);
                            Place old = getOriginalPlace();
                            if (old != null) old.delete(db);
                            asPlace().upsert(db);
                            PlaceEdit.this.finish();
                        }
                        catch (IllegalStateException e) {
                            Toast.makeText(
                                PlaceEdit.this,
                                String.format(
                                    "This place cannot be saved (%s)",
                                    e.getMessage()),
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        findViewById(R.id.revert).
        setOnClickListener(
            new OnClickListener() {
                public void onClick(View v) {
                    setResult(RESULT_CANCELED);
                    PlaceEdit.this.finish();
                }
            });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        db.open();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        db.close();
    }
    
    private Place getOriginalPlace() {
        Intent i = getIntent();
        String latitude = getString(R.string.latitude);
        if (!i.hasExtra(latitude)) return null;
        
        Bundle extras = i.getExtras();
        String longitude = getString(R.string.longitude);
        Location l = new Location("whatever");
        l.setLatitude(extras.getDouble(latitude));
        l.setLongitude(extras.getDouble(longitude));
        return Place.fetch(db, l);
    }
    
    private void fillData() {
        Place p = getOriginalPlace();
        if (p == null) return;
        fillData(p);
    }
    private void fillData(Place p) {
        EditText nom = findTypedViewById(R.id.name);
        EditText lat = findTypedViewById(R.id.latitude);
        EditText lng = findTypedViewById(R.id.longitude);
        Spinner rm = findTypedViewById(R.id.ringer_mode);
        lat.setText(Double.toString(p.location.getLatitude()));
        lng.setText(Double.toString(p.location.getLongitude()));
        rm.setSelection(
            positionFor(
                rm,
                p.ringerMode));
        nom.setText(p.name);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(
            android.view.Menu.NONE,
            R.string.place_edit_here,
            android.view.Menu.NONE,
            R.string.place_edit_here);
        return true;
    }
    
    private static class LocationGetter implements LocationListener {
        public volatile Location l;
        
        public void onLocationChanged(Location location) {
            l = location;
            Looper.myLooper().quit();
        }

        public void onProviderDisabled(String provider) {}

        public void onProviderEnabled(String provider) {}

        public void onStatusChanged(String provider, int status,
                Bundle extras) {}
        
        private static Location get(final Context ctx, final long timeout) {
            LocationManager lm = WRService.getSystemService(ctx, Context.LOCATION_SERVICE);
            Criteria c = new Criteria();
            c.setAccuracy(Criteria.ACCURACY_FINE);
            String p = lm.getBestProvider(c, true);
            final LocationGetter getter = new LocationGetter();
            Looper.prepare();
            try {
                lm.requestLocationUpdates(
                    p,
                    0,
                    0,
                    getter);
                final Looper myLooper = Looper.myLooper();
                Executors.newSingleThreadScheduledExecutor().schedule(
                    new Runnable() {
                        public void run() {
                            try {
                                myLooper.quit();
                            }
                            catch (RuntimeException e) {
                                if (getter.l == null) {
                                    throw e;
                                }
                                //expected: the looper shut down when we got the location.
                            }
                        }
                    },
                    timeout,
                    TimeUnit.MILLISECONDS);
                Looper.loop();
            }
            finally {
                lm.removeUpdates(getter);
            }
            
            return lm.getLastKnownLocation(p);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        if (item.getItemId() == R.string.place_edit_here) {
            new AsyncTask<Context, Void, Location>() {
                private Context ctx;
                
                @Override
                protected Location doInBackground(Context... params) {
                    ctx = params[0];
                    return LocationGetter.get(PlaceEdit.this, 512);
                }
                protected void onPostExecute(Location l) {
                    if (l == null) {
                        Toast.makeText(
                            ctx,
                            R.string.no_location_available,
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Place here = null;
                    try {
                        here = asPlace();
                    }
                    catch (IllegalStateException e) {
                        // not a place
                        here = new Place(new Location("whatever"), RingerMode.normal, "");
                    }
                    here.location.set(l);
                    fillData(here);
                    return;
                }
            }.execute(this);

            return true;
        }
        return result;
    }
    
    private <T> int positionFor(Spinner s, T x) {
        ArrayAdapter<T> rma = (ArrayAdapter<T>)s.getAdapter();
        int c = rma.getCount();
        for (int i = 0; i < c; ++i) {
            if (x.equals(rma.getItem(i))) return i;
        }
        throw
            new IllegalArgumentException(
                String.format("no such item %s", x));
    }
    
    private Place asPlace() {
        EditText name = findTypedViewById(R.id.name);
        Location l = new Location("whatever");
        l.setLatitude(extractCoordinate(R.id.latitude));
        l.setLongitude(extractCoordinate(R.id.longitude));
        Spinner rms = findTypedViewById(R.id.ringer_mode);
        ArrayAdapter<RingerMode> rma =
            (ArrayAdapter<RingerMode>)rms.getAdapter();
        RingerMode rm =
            rma.getItem(rms.getSelectedItemPosition());
        return new Place(l, rm, name.getText().toString());
    }
    private double extractCoordinate(int id) {
        EditText text = findTypedViewById(id);
        String txt = text.getText().toString();
        try {
            return Double.parseDouble(txt);
        }
        catch (NumberFormatException e) {
            String message = String.format("%s is not a coordinate", txt);
            text.setError(message);
            throw
                new IllegalStateException(
                    message,
                    e);
        }
    }
    private <T> T findTypedViewById(int id) {
        return (T)findViewById(id);
    }
    private DBAdapter db;
}
