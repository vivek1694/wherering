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

import junit.framework.TestCase;
import seanfoy.Greenspun;
import seanfoy.wherering.Place;
import seanfoy.wherering.Place.RingerMode;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;

public class LocationHelpers {
    public static void setupTestProvider(LocationManager lm) {
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
        lm.setTestProviderEnabled(testProviderName, true);
    }
    public static void teardownTestProvider(LocationManager lm) {
        lm.removeTestProvider(testProviderName);
    }
    
    public static Place teleport(Context ctx, LocationManager lm, Place destination) {
        Location lastLoc = null;
        //TODO: why is this ProximityAlert necessary?
        // Because LocationManagerService.handleLocationChangedLocked
        // looks for mRecordsByProvider.get(provider) first thing.
        // That dict stores info about subscribed Receivers. When
        // nobody's subscribed, the service doesn't bother to
        // update mLastKnownLocation.
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, new Intent(), 0);
        lm.addProximityAlert(0, 0, 0, -1, pi);
        destination.location.setTime(System.currentTimeMillis());
        lm.setTestProviderLocation(testProviderName, destination.location);
        TestCase.assertTrue(lm.isProviderEnabled(testProviderName));
        try {
            while (lastLoc == null || destination.location.distanceTo(lastLoc) > 0f) {
                Location l = new Location(destination.location);
                l.setTime(System.currentTimeMillis());
                lm.setTestProviderLocation(testProviderName, l);
                Greenspun.sleep(10);
                lastLoc = lm.getLastKnownLocation(testProviderName);
            }
        }
        finally {
            lm.removeProximityAlert(pi);
        }
        double destLat = destination.location.getLatitude();
        double lastLat = lastLoc.getLatitude();
        TestCase.assertEquals(destLat, lastLat);
        return destination;
    }

    public static Place makePlaceDeLaConcorde() {
        return makePlace("Place de la Concorde", 48.865594443, 2.32071667);
    }
    
    public static Place makeNewbury() {
        return makePlace("Newbury Street", 42.34928, -71.083725);
    }
    
    public static Place makePlace(String name, double lat, double lng) {
        Place place =
            new Place(
                new Location(testProviderName),
                RingerMode.normal,
                name);
        place.location.setLatitude(lat);
        place.location.setLongitude(lng);
        return place;
    }
    
    public static Place makeGoogleplex() {
        return makePlace("Googleplex", 37.0625,-95.677068);
    }
    
    public final static String testProviderName = LocationManager.GPS_PROVIDER;
}
