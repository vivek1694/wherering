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
package seanfoy.wherering.test;

import seanfoy.Greenspun;
import static seanfoy.Greenspun.*;
import seanfoy.wherering.DBAdapter;
import seanfoy.wherering.Place;
import seanfoy.wherering.PlaceEdit;
import seanfoy.wherering.R;
import android.app.Activity;
import android.content.Context;
import android.location.LocationManager;
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
        LocationHelpers.setupTestProvider(lm);
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
            LocationHelpers.teardownTestProvider(lm);            
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
        Place place = LocationHelpers.makePlaceDeLaConcorde();
        placeEdit.fillData(place);

        Button done = findTypedViewById(placeEdit, R.id.done);
        done.performClick();
        assertEquals(1, countPlaces(db));
    }

    @UiThreadTest
    public void testRevertNew() {
        PlaceEdit placeEdit = getActivity();
        int expected = countPlaces(db);
        Place place = LocationHelpers.makePlaceDeLaConcorde();
        placeEdit.fillData(place);
        Button revert = findTypedViewById(placeEdit, R.id.revert);
        revert.performClick();
        assertEquals(expected, countPlaces(db));
    }
    
    public void testForSmokeN() throws Throwable {
        makeThenEdit(LocationHelpers.makeNewbury());
    }    
    public void testForSmokeG() throws Throwable {
        makeThenEdit(LocationHelpers.makeGoogleplex());
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
                    placeholder[0] = a.toPlace();
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
                    placeholder[0] = a.toPlace();
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
        return makeThenEdit(LocationHelpers.makePlaceDeLaConcorde());
    }
    
    private Place makeThenEdit(final Place place) throws Throwable {
        final Context ctx = getInstrumentation().getTargetContext();
        place.upsert(db);
        setActivityIntent(PlaceEdit.intentToEdit(ctx, place));
        final PlaceEdit placeEdit = getActivity();
        runTestOnUiThread(
            new Runnable() {
                public void run() {
                    Place loaded = placeEdit.toPlace();
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
        final Place delaconcorde =
            LocationHelpers.teleport(
                placeEdit,
                lm,
                LocationHelpers.makePlaceDeLaConcorde());
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
                    assertEquals(delaconcorde, placeEdit.toPlace());
                }
            });
    }

    @UiThreadTest
    public void testTransporter() {
        LocationHelpers.teleport(
            getActivity(),
            lm,
            LocationHelpers.makePlaceDeLaConcorde());
    }

    private int countPlaces(final DBAdapter db) {
        return
            Greenspun.enhtry(
                Place.allPlaces(db),
                new Greenspun.Func1<Iterable<Place>, Integer>() {
                    public Integer f(Iterable<Place> x) {
                        return Greenspun.count(x);
                    }
                });
    }
    
    private LocationManager lm;
    
    @SuppressWarnings("unchecked")
    private static <T> T findTypedViewById(Activity a, int id) {
        return (T)a.findViewById(id);
    }
    @SuppressWarnings("unused")
    private static <T> T findTypedViewById(View v, int id) {
        return (T)v.findViewById(id);
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
