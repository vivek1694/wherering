package seanfoy.wherering;

import static seanfoy.wherering.intent.IntentHelpers.fullname;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import seanfoy.Greenspun.Func1;
import seanfoy.wherering.intent.action;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

/**
 * Subscribe and periodically renew
 * subscriptions to proximity alerts
 */
public class WRService extends Service {
    public void proximate(Intent intent) {
        Context context = getApplicationContext();
        updateRing(context, intent);
        context.sendBroadcast(new Intent(fullname(action.ALERT)));
    }
    
    public void renew() {
        Context context = getApplicationContext();
        unsubscribe(context);
        subscribe(context, getConfig(context));
    }
    
    public void subscribe(Context ctx, HashMap<Integer, Place> config) {
        Log.i(getClass().getName(), String.format("System.currentTimeMillis() = %s", System.currentTimeMillis()));
        LocationManager lm = getSystemService(ctx, Context.LOCATION_SERVICE);
        for (Map.Entry<Integer, Place> c : config.entrySet()) {
            Intent i = new Intent(fullname(action.PROXIMITY));
            i.putExtra(placeKeyName, c.getKey());
            PendingIntent pi =
                PendingIntent.getService(
                    ctx,
                    // docs say "currently not used"
                    0,
                    i,
                    0);
            lmsubs.add(pi);
            lm.addProximityAlert(
                c.getValue().location.getLatitude(),
                c.getValue().location.getLongitude(),
                radiusM,
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

    public void updateRing(Context ctx, Intent intent) {
        int localRingMode = getConfig(ctx).get(intent.getExtras().getInt(placeKeyName)).ringerMode.ringer_mode;
        AudioManager am = getSystemService(ctx, Context.AUDIO_SERVICE);
        if (intent.getExtras().getBoolean(LocationManager.KEY_PROXIMITY_ENTERING)) {
            previous_ringer_mode = am.getRingerMode();
            am.setRingerMode(localRingMode);
        }
        else {
            // leave the ringer as we found it
            // (unless the user has changed it meanwhile)
            if (am.getRingerMode() == localRingMode) {
                am.setRingerMode(previous_ringer_mode);
            }
        }
    }

    private final static HashMap<Integer, Place> getConfig(Context ctx) {
        final HashMap<Integer, Place> config = new HashMap<Integer, Place>();
        DBAdapter.withDBAdapter(
            ctx,
            new Func1<DBAdapter, Void>() {
                public Void f(DBAdapter adapter) {
                    for (Place p : Place.allPlaces(adapter)) {
                        config.put(p.hashCode(), p);
                    }
                    return null;
                }
            });
        return config;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
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
            return result;
        }
        if (intentAction.equals(fullname(action.PROXIMITY))) {
            proximate(intent);
        }
        else if (intentAction.equals(fullname(action.SUBSCRIBE))) {
            renew();
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
    private static final String placeKeyName = WRBroadcastReceiver.class.getName() + ":place-key";
    private int previous_ringer_mode = AudioManager.RINGER_MODE_NORMAL;
    static final int radiusM = 25;
    
    @SuppressWarnings("unchecked")
    public static <T> T getSystemService(Context ctx, String name) {
        return (T)ctx.getSystemService(name);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
