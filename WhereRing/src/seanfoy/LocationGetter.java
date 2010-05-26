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
package seanfoy;

import seanfoy.wherering.WRService;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class LocationGetter implements LocationListener {
    public volatile Location l;
    public Looper looper;
    public LocationGetter(Looper looper) {
        this.looper = looper;
    }
    
    public void onLocationChanged(Location location) {
        l = location;
        checkGoodEnough(l.getExtras());
    }

    public void onProviderDisabled(String provider) {}

    public void onProviderEnabled(String provider) {}

    public void onStatusChanged(String provider, int status,
            Bundle extras) {
        if (status == LocationProvider.OUT_OF_SERVICE) {
            l = null;
            looper.quit();
        }
        checkGoodEnough(extras);
    }
    
    private void checkGoodEnough(Bundle extras) {
        if (extras != null && extras.containsKey("satellites")) {
            if (extras.getInt("satellites") >= 4) {
                looper.quit();
            }
        }
    }
    
    /**
     * Make a best-effort determination of the current
     * position, given the time constraint. I want to
     * use a Looper, so call me from an AsyncTask
     * background thread or something.
     * 
     * @param ctx a Context providing access to a
     * LocationManager
     * @param timeout milliseconds
     * @return The last known location (or null) after
     * acquiring at least four satellites or using the
     * alloted time searching for them.
     */
    public static Location get(final Context ctx, final long timeout) {
        Looper.prepare();
        final Handler handler = new Handler();
        LocationManager lm = WRService.getSystemService(ctx, Context.LOCATION_SERVICE);
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        String p = lm.getBestProvider(c, true);
        if (p == null) return null;
        
        final LocationGetter getter = new LocationGetter(handler.getLooper());
        try {
            lm.requestLocationUpdates(
                p,
                0,
                0,
                getter);
            handler.postDelayed(
                new Runnable() {
                    public void run() {
                        handler.getLooper().quit();
                    }
                },
                timeout);
            Looper.loop();
        }
        finally {
            lm.removeUpdates(getter);
        }
        
        return lm.getLastKnownLocation(p);
    }
}

