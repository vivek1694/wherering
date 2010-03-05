package seanfoy.wherering.test;

import seanfoy.Greenspun;
import seanfoy.wherering.DBAdapter;
import seanfoy.wherering.Place;
import seanfoy.wherering.PlaceEdit;
import seanfoy.wherering.R;
import seanfoy.wherering.Place.RingerMode;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class TestPlaceEdit extends ActivityInstrumentationTestCase2<PlaceEdit> {
    public TestPlaceEdit() {
        super("seanfoy.wherering", PlaceEdit.class);
    }
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        db = new DBAdapter(getInstrumentation().getTargetContext());
        db.open();
        db.delete(Place.TABLE_NAME, "");
        lm =
            (LocationManager)getInstrumentation().getContext().getSystemService(Context.LOCATION_SERVICE);
        lm.addTestProvider(
            testProviderName,
            "requiresNetwork" == "",
            "requiresSatellite" == "",
            "requiresCell" == "",
            "hasMonetaryCost" == "",
            "supportsAltitude" == "",
            "supportsSpeed" == "",
            "supportsBearing" == "",
            android.location.Criteria.POWER_LOW,
            android.location.Criteria.ACCURACY_FINE);
    }

    private DBAdapter db;

    @Override
    public void tearDown() throws Exception {
        // closures feel like overkill, efficient macros would be nice.
        // I hear such a macro is coming in javac 7 or whatever.
        Exception first = null;
        try {
            db.close();
        }
        catch (Exception e) {
            if (first == null) first = e;
        }
        try {
            lm.removeTestProvider(testProviderName);            
        }
        catch (Exception e) {
            if (first == null) first = e;
        }
        try {
            super.tearDown();
        }
        catch (Exception e) {
            if (first == null) first = e;
        }
        if (first != null) throw first;
    }
    
    @UiThreadTest
    public void testNewPlace() {
        PlaceEdit placeEdit = getActivity();
        Place place = makePlaceDeLaConcorde();
        placeEdit.fillData(place);

        Button done = findTypedViewById(placeEdit, R.id.done);
        done.performClick();
        assertEquals(1, countPlaces(db));
    }

    @UiThreadTest
    public void testRevertNew() {
        PlaceEdit placeEdit = getActivity();
        int expected = countPlaces(db);
        Place place = makePlaceDeLaConcorde();
        placeEdit.fillData(place);
        Button revert = findTypedViewById(placeEdit, R.id.revert);
        revert.performClick();
        assertEquals(expected, countPlaces(db));
    }
    
    public void testExistingPlace() throws Throwable {
        int expected = countPlaces(db) + 1;
        makeThenEdit();
        finishEditing(R.id.done);
        assertEquals(expected, countPlaces(db));
    }
    
    public void testRevertExistingPlace() throws Throwable {
        int expected = countPlaces(db) + 1;
        makeThenEdit();
        finishEditing(R.id.revert);
        assertEquals(expected, countPlaces(db));
    }
    
    private void finishEditing(final int finishButtonId) throws Throwable {
        runTestOnUiThread(
            new Runnable() {
                public void run() {
                    Button done = findTypedViewById(getActivity(), finishButtonId);
                    done.performClick();
                }
            });
    }
    
    public void testChangePlace() throws Throwable {
        int expected = countPlaces(db) + 1;
        Place originalPlace = makeThenEdit();
        final Place [] placeholder = new Place[1];
        runTestOnUiThread(
            new Runnable() {
                public void run() {
                    PlaceEdit a = getActivity();
                    EditText latitude = findTypedViewById(a, R.id.latitude);
                    latitude.setText("42");
                    placeholder[0] = a.asPlace();
                    Button done = findTypedViewById(a, R.id.done);
                    done.performClick();
                }
            });
        assertEquals(expected, countPlaces(db));
        Place loaded = Place.fetch(db, placeholder[0].location);
        assertEquals(placeholder[0], loaded);
        assertTrue(!originalPlace.equals(loaded));
    }

    public void testRevertChangePlace() throws Throwable {
        int expected = countPlaces(db) + 1;
        Place originalPlace = makeThenEdit();
        final Place [] placeholder = new Place[1];
        runTestOnUiThread(
            new Runnable() {
                public void run() {
                    PlaceEdit a = getActivity();
                    EditText latitude = findTypedViewById(a, R.id.latitude);
                    latitude.setText("42");
                    placeholder[0] = a.asPlace();
                    Button revert = findTypedViewById(a, R.id.revert);
                    revert.performClick();
                }
            });
        assertEquals(expected, countPlaces(db));
        Place loaded = Place.fetch(db, placeholder[0].location);
        assertNull(loaded);
        assertTrue(!originalPlace.equals(placeholder[0]));
        assertEquals(originalPlace, Place.fetch(db, originalPlace.location));
    }
    
    private Place makeThenEdit() throws Throwable {
        final Context ctx = getInstrumentation().getTargetContext();
        final Place place = makePlaceDeLaConcorde();
        place.upsert(db);
        setActivityIntent(PlaceEdit.intentToEdit(ctx, place));
        final PlaceEdit placeEdit = getActivity();
        runTestOnUiThread(
            new Runnable() {
                public void run() {
                    Place loaded = placeEdit.asPlace();
                    assertEquals(place, loaded);
                }
            });
        return place;
    }
    
    public void testUseCurrentLocation() throws Throwable {
        final PlaceEdit placeEdit = getActivity();
        runTestOnUiThread(
            new Runnable() {
                public void run() {
                    EditText latitude = findTypedViewById(placeEdit, R.id.latitude);
                    latitude.setText("42");
                }
            });
        final Place delaconcorde = teleport();
        final TextChangedCounter c = new TextChangedCounter();
        runTestOnUiThread(
            new Runnable() {
                public void run() {
                    placeEdit.openOptionsMenu();
                    EditText latitude = findTypedViewById(placeEdit, R.id.latitude);
                    latitude.addTextChangedListener(c);
                    EditText longitude = findTypedViewById(placeEdit, R.id.longitude);
                    longitude.addTextChangedListener(c);
                }
            });
        getInstrumentation().invokeMenuActionSync(placeEdit, R.string.place_edit_here, 0);
        while (c.counter < 2) {
            sleep(10);
        }
        runTestOnUiThread(
            new Runnable() {
                public void run() {
                    assertEquals(delaconcorde, placeEdit.asPlace());
                }
            });
    }

    @UiThreadTest
    public void testTransporter() {
        teleport();
    }

    private Place teleport() {
        Place delaconcorde = makePlaceDeLaConcorde();
        delaconcorde.location.setTime(System.currentTimeMillis());
        lm.setTestProviderEnabled(testProviderName, true);
        assertEquals(testProviderName, lm.getBestProvider(new Criteria(), true));
        PendingIntent pi = PendingIntent.getBroadcast(getActivity(), 0, new Intent(), 0);
        lm.addProximityAlert(2, 2, 20, 10, pi);
        try {
            lm.setTestProviderStatus(testProviderName, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
            lm.setTestProviderLocation(testProviderName, delaconcorde.location);
            while (lm.getLastKnownLocation(testProviderName) == null) {
                sleep(10);
            }
        }
        finally {
            lm.removeProximityAlert(pi);
        }
        Location lastLoc = lm.getLastKnownLocation(testProviderName);
        double parisLat = delaconcorde.location.getLatitude();
        double lastLat = lastLoc.getLatitude();
        assertEquals(parisLat, lastLat);
        return delaconcorde;
    }

    private Place makePlaceDeLaConcorde() {
        Place place =
            new Place(
                new Location(testProviderName),
                RingerMode.normal,
                "Place de la Concorde");
        place.location.setLatitude(48.865594443);
        place.location.setLongitude(2.32071667);
        return place;
    }

    private int countPlaces(DBAdapter db) {
        return Greenspun.count(Place.allPlaces(db));
    }
    
    private LocationManager lm;
    private final String testProviderName = LocationManager.GPS_PROVIDER;
    
    @SuppressWarnings("unchecked")
    private static <T> T findTypedViewById(Activity a, int id) {
        return (T)a.findViewById(id);
    }
    @SuppressWarnings("unused")
    private static <T> T findTypedViewById(View v, int id) {
        return (T)v.findViewById(id);
    }
    
    private void sleep(long millis) {
        long deadline = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(deadline - System.currentTimeMillis());
            }
            catch (InterruptedException e) {
                // try, try again
            }
        }
    }
    
    public class TextChangedCounter implements TextWatcher {
        public volatile int counter = 0;
        public void afterTextChanged(Editable arg0) {
            ++counter;
        }
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before,
                int count) {
        }            
    }
}
