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

import seanfoy.AsyncLooperTask;
import seanfoy.wherering.Place.RingerMode;
import seanfoy.wherering.intent.action;
import static seanfoy.wherering.intent.IntentHelpers.fullname;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
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
            new ArrayAdapter<RingerMode>(getApplicationContext(), android.R.layout.simple_spinner_item);
        ringers.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
                            Place nouveau = asPlace();
                            nouveau.upsert(db);
                            if (old != null && !old.equals(nouveau)) old.delete(db);
                            PlaceEdit.this.startService(new Intent(fullname(action.SIGHUP)));
                            PlaceEdit.this.finish();
                        }
                        catch (NonCoordinateException e) {
                            Toast.makeText(
                                PlaceEdit.this,
                                String.format(
                                    getString(R.string.unsaveable_place),
                                    String.format(
                                        getString(R.string.not_a_coordinate),
                                        e.getMessage())),
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
    public void fillData(Place p) {
        EditText nom = findTypedViewById(R.id.name);
        Spinner rm = findTypedViewById(R.id.ringer_mode);
        fillDataLocation(p.location);
        rm.setSelection(
            positionFor(
                rm,
                p.ringerMode));
        nom.setText(p.name);
    }
    private void fillDataLocation(Location location) {
        EditText lat = findTypedViewById(R.id.latitude);
        EditText lng = findTypedViewById(R.id.longitude);
        lat.setText(Double.toString(location.getLatitude()));
        lng.setText(Double.toString(location.getLongitude()));
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(
            android.view.Menu.NONE,
            R.string.place_edit_here,
            android.view.Menu.NONE,
            R.string.place_edit_here);

        Place dummy = new Place(new Location(""), RingerMode.normal, "");
        if (existsIntentActivity(showOnMap(dummy))) {
            menu.add(
                android.view.Menu.NONE,
                R.string.show_on_map,
                android.view.Menu.NONE,
                R.string.show_on_map);
        }
        return true;
    }
    
    private boolean existsIntentActivity(Intent i) {
        return
            getPackageManager().
            queryIntentActivities(
                i,
                PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }
    
    private Intent showOnMap() {
        return showOnMap(asPlace());
    }
    private Intent showOnMap(Place p) {
        Intent showOnMap = new Intent(Intent.ACTION_VIEW);
        showOnMap.setData(
            Uri.parse(
                String.format(
                    "geo:%s,%s",
                    p.location.getLatitude(),
                    p.location.getLongitude())));
        return showOnMap;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        if (item.getItemId() == R.string.show_on_map) {
            try {                
                startActivity(showOnMap());
                return true;
            }
            catch (NonCoordinateException e) {
                Toast.makeText(
                    this,
                    String.format(
                        getString(R.string.not_a_coordinate),
                        e.noncoordinate),
                    Toast.LENGTH_SHORT);
                return false;
            }
        }
        else if (item.getItemId() == R.string.place_edit_here) {
            new AsyncLooperTask<PlaceEdit, Void, Location>() {
                @Override
                public Location doInBackground(PlaceEdit... P) {
                    return seanfoy.LocationGetter.get(P[0], 4096);
                }
                public void onPostExecute(Location l) {
                    super.onPostExecute(l);
                    if (l == null) {
                        Toast.makeText(
                            PlaceEdit.this,
                            R.string.no_location_available,
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PlaceEdit.this.fillDataLocation(l);
                }
            }.execute(PlaceEdit.this);

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
    
    public Place asPlace() throws NonCoordinateException {
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
    private double extractCoordinate(int id) throws NonCoordinateException {
        EditText text = findTypedViewById(id);
        String txt = text.getText().toString();
        try {
            try {
                return Location.convert(txt);
            }
            catch (Exception e) {
                // try something else
            }
            return Double.parseDouble(txt);
        }
        catch (NumberFormatException e) {
            String message = String.format("%s is not a coordinate", txt);
            //TODO: setError is a nice idea but we need to do it
            // conditionally on the user providing input. If we're
            // calling this from a context in which we use a place
            // when one is available and otherwise proceed with
            // a default, we could trigger this setError at a time
            // when the user wouldn't expect that. Worse, we might
            // then change the state of the coordinate control by
            // means other than the typing events that would clear
            // this message. Then we'd complain about a condition
            // the user didn't cause and which has been remedied
            // or at least altered by our code.
            //text.setError(message);
            throw
                new NonCoordinateException(
                    message,
                    txt,
                    e);
        }
    }

    public static Intent intentToEdit(Context context, Place place) {
        Intent edit =
            new Intent(
                    android.content.Intent.ACTION_EDIT,
                    null,
                    context,
                    PlaceEdit.class);
        if (place != null) {
            edit.putExtra(
                context.getString(R.string.latitude),
                place.location.getLatitude());
            edit.putExtra(
                context.getString(R.string.longitude),
                place.location.getLongitude());
        }
        return edit;
    }

    @SuppressWarnings("unchecked")
    private <T> T findTypedViewById(int id) {
        return (T)findViewById(id);
    }
    private DBAdapter db;
    
    public class NonCoordinateException extends IllegalStateException {
        public NonCoordinateException(String message, String noncoordinate, Exception e) {
            super(message, e);
            this.noncoordinate = noncoordinate;
        }
        public String noncoordinate;
    }
}
