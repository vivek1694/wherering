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

import static seanfoy.wherering.intent.IntentHelpers.fullname;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import seanfoy.Greenspun;
import seanfoy.Greenspun.Func1;
import seanfoy.wherering.intent.action;
import seanfoy.wherering.WRBroadcastReceiver.AlertExtras;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;

/**
 * Subscribe and periodically renew
 * subscriptions to proximity alerts
 */
public class WRService extends Service {
    public void proximate(Intent intent) {
        Context context = getApplicationContext();
        Bundle alertExtras = updateRing(context, intent);
        if (AlertExtras.interesting(alertExtras)) {
            Intent alert = new Intent(fullname(action.ALERT));
            alert.putExtras(alertExtras);
            context.sendBroadcast(alert);
        }
    }
    
    public void renew() {
        Context context = getApplicationContext();
        unsubscribe(context);
        subscribe(context, getConfig(context));
    }
    
    public void subscribe(Context ctx, HashMap<Integer, Place> config) {
        LocationManager lm = getSystemService(ctx, Context.LOCATION_SERVICE);
        for (Map.Entry<Integer, Place> c : config.entrySet()) {
            Intent i = new Intent(fullname(action.PROXIMITY));
            //PI equality doesn't consider extras, so we obtain
            // independent PIs by supplying different URIs
            Place p = c.getValue();
            i.setData(
                Uri.parse(
                    String.format(
                        "wherering://seanfoy.wherering/places/%s/%s",
                        p.hashCode(),
                        p.ringerMode)));
            i.putExtra(rmKeyName, p.ringerMode.ringer_mode);
            i.putExtra(placenameKeyName, p.name);
            i.putExtra(placeHCKeyName, p.hashCode());
            PendingIntent pi =
                PendingIntent.getService(
                    ctx,
                    // docs say "currently not used"
                    0,
                    i,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            lmsubs.add(pi);
            lm.addProximityAlert(
                p.location.getLatitude(),
                p.location.getLongitude(),
                p.radius,
                System.currentTimeMillis() + LM_EXPIRY,
                pi);
        }
    }

    public void unsubscribe(Context ctx) {
        LocationManager lm =
            getSystemService(ctx, Context.LOCATION_SERVICE);
        synchronized(lmsubs) {
            for (PendingIntent i : lmsubs) {
                lm.removeProximityAlert(i);
            }
            lmsubs.clear();
        }
    }

    public Bundle updateRing(Context ctx, Intent intent) {
        Bundle extras = intent.getExtras();
        String placename = extras.getString(placenameKeyName);
        boolean entering = extras.getBoolean(LocationManager.KEY_PROXIMITY_ENTERING);
        int phc = extras.getInt(placeHCKeyName);
        int localRM = extras.getInt(rmKeyName);
        return updateRing(ctx, phc, placename, entering, localRM);
    }

    public Bundle updateRing(Context ctx, int phc, String placename, boolean entering, int localRingMode) {
        AudioManager am = getSystemService(ctx, Context.AUDIO_SERVICE);
        if (entering && last_known_place_hc != phc) {
            if (last_known_place_hc == 0) {
                default_ringer_mode = am.getRingerMode();
            }
            last_known_place_hc = phc;
            am.setRingerMode(localRingMode);
            return AlertExtras.asBundle(placename, entering, true, localRingMode);
        }
        else if (!entering && last_known_place_hc == phc) {
            last_known_place_hc = 0;
            // leave the ringer as we found it
            // (unless the user has changed it meanwhile)
            if (am.getRingerMode() == localRingMode) {
                am.setRingerMode(default_ringer_mode);
                return AlertExtras.asBundle(placename, entering, true, default_ringer_mode);
            }
            else {
                default_ringer_mode = am.getRingerMode();
            }
        }
        return AlertExtras.asBundle(placename, entering, false, am.getRingerMode());
    }
    private final static HashMap<Integer, Place> getConfig(Context ctx) {
        final HashMap<Integer, Place> config = new HashMap<Integer, Place>();
        DBAdapter.withDBAdapter(
            ctx,
            new Func1<DBAdapter, Void>() {
                public Void f(DBAdapter adapter) {
                    Greenspun.enhfor(
                        Place.allPlaces(adapter),
                        new Greenspun.Func1<Place, Void>() {
                            public Void f(Place p) {
                                config.put(p.hashCode(), p);
                                return null;
                            }
                        });
                    return null;
                }
            });
        return config;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
    
    public boolean isRunning() {
        return alarmSubs.size() > 0;
    }
    public boolean isSubscribed() {
        return lmsubs.size() > 0;
    }
    public synchronized void startup() {
        if (isRunning()) return;
        
        Context context = getApplicationContext();
        // If this app is uninstalled, its
        // non-expiring PAs will persist
        // until reboot. So, the PAs ought
        // to expire and we'll renew them
        // periodically before the
        // expirations and until we're
        // uninstalled or shutdown.
        AlarmManager am =
            getSystemService(context, Context.ALARM_SERVICE);
        PendingIntent ai =
            PendingIntent.getService(
                context.getApplicationContext(),
                // not used --- Docs
                0,
                new Intent(fullname(action.SUBSCRIBE)),
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarmSubs.add(ai);
        am.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime(),
            LM_EXPIRY / 2,
            ai);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = START_STICKY;
        String intentAction = intent.getAction();
        if (intentAction == null) {
            startup();
            return result;
        }
        if (intentAction.equals(fullname(action.PROXIMITY))) {
            proximate(intent);
        }
        else if (intentAction.equals(fullname(action.SUBSCRIBE))) {
            renew();
        }
        else if (intentAction.equals(fullname(action.SIGHUP))) {
            if (isRunning()) renew();
        }
        return result;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Context context = getApplicationContext();
        AlarmManager am =
            getSystemService(context, Context.ALARM_SERVICE);
        synchronized(alarmSubs) {
            for (PendingIntent a : alarmSubs) {
                am.cancel(a);
            }
            alarmSubs.clear();
        }
        unsubscribe(context);
    }
    
    private Set<PendingIntent> alarmSubs =
        Collections.synchronizedSet(new HashSet<PendingIntent>());
    private static final long LM_EXPIRY =
        AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    private final static Set<PendingIntent> lmsubs =
        Collections.synchronizedSet(new HashSet<PendingIntent>());
    private static final String placenameKeyName = WRBroadcastReceiver.class.getName() + ":placename-key";
    private static final String rmKeyName = WRBroadcastReceiver.class.getName() + ":rm-key";
    private static final String placeHCKeyName = WRBroadcastReceiver.class.getName() + ":lkphc-key";
    private int default_ringer_mode = AudioManager.RINGER_MODE_NORMAL;
    private int last_known_place_hc = 0;
    
    @SuppressWarnings("unchecked")
    public static <T> T getSystemService(Context ctx, String name) {
        return (T)ctx.getSystemService(name);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
