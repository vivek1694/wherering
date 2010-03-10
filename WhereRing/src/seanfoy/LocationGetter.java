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

