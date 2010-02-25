package seanfoy.wherering;

import static seanfoy.wherering.intent.IntentHelpers.fullname;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import seanfoy.Greenspun.Func1;
import seanfoy.wherering.intent.action;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.SystemClock;
import android.util.Log;

public class WRBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (intentAction.equals(fullname(action.PROXIMITY))) {
            updateRing(context, intent);
            context.sendBroadcast(new Intent(fullname(action.ALERT)));
        }
        else if (intentAction.equals(fullname(action.ALERT))) {
            raiseAlert(context);
        }
        else if (intentAction.equals(fullname(action.SHUTDOWN))) {
            AlarmManager am =
                (AlarmManager)getSystemService(context, Context.ALARM_SERVICE);
            synchronized(alarmSubs) {
                for (PendingIntent a : alarmSubs) {
                    Log.i(getClass().getName(), "cancelling an alarm");
                    am.cancel(a);
                }
                alarmSubs.clear();
            }
            unsubscribe(context);
        }
        else if (intentAction.equals(fullname(action.SUBSCRIBE))) {
            unsubscribe(context);
            subscribe(context, getConfig(context));
        }
        else if (intentAction.equals(android.content.Intent.ACTION_BOOT_COMPLETED)) {
            if (!getPrefs(context).getBoolean(android.content.Intent.ACTION_BOOT_COMPLETED, true)) {
                return;
            }
            context.sendBroadcast(new Intent(fullname(action.STARTUP)));
        }
        else if (intentAction.equals(fullname(action.STARTUP))) {
            // If this app is uninstalled, its
            // non-expiring PAs will persist
            // until reboot. So, the PAs ought
            // to expire and we'll renew them
            // periodically before the
            // expirations and until we're
            // uninstalled or shutdown.
            AlarmManager am =
                (AlarmManager)getSystemService(context, Context.ALARM_SERVICE);
            PendingIntent ai =
                PendingIntent.getBroadcast(
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
    }
    
    private Set<PendingIntent> alarmSubs =
        Collections.synchronizedSet(new HashSet<PendingIntent>());
    private static final long LM_EXPIRY =
        AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    
    private void raiseAlert(Context context) {
            String app_name = context.getString(R.string.app_name);
            String app_ticker = context.getString(R.string.alert_ticker);
            String alert_text = context.getString(R.string.alert_text);
            NotificationManager nm =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification note =
                new Notification(R.drawable.icon, app_ticker, System.currentTimeMillis());
            note.setLatestEventInfo(
                    context,
                    app_name,
                    alert_text,
                    PendingIntent.getActivity(
                            context,
                            0,
                            new Intent(context, Control.class),
                            0));
            note.flags = note.flags | Notification.FLAG_AUTO_CANCEL;
            nm.notify(1, note);
    }

    private final static Set<PendingIntent> lmsubs =
        Collections.synchronizedSet(new HashSet<PendingIntent>());

    @SuppressWarnings("unchecked")
    public static <T> T getSystemService(Context ctx, String name) {
        return (T)ctx.getSystemService(name);
    }
    
    private static final String placeKeyName = WRBroadcastReceiver.class.getName() + ":place-key";
    private void subscribe(Context ctx, HashMap<Integer, Place> config) {
        Log.i(getClass().getName(), String.format("System.currentTimeMillis() = %s", System.currentTimeMillis()));
        LocationManager lm = getSystemService(ctx, Context.LOCATION_SERVICE);
        for (Map.Entry<Integer, Place> c : config.entrySet()) {
            Intent i = new Intent(fullname(action.PROXIMITY));
            i.putExtra(placeKeyName, c.getKey());
            PendingIntent pi =
                PendingIntent.getBroadcast(
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

    private void unsubscribe(Context ctx) {
        LocationManager lm = getSystemService(ctx, Context.LOCATION_SERVICE);
        synchronized(lmsubs) {
            for (PendingIntent i : lmsubs) {
                lm.removeProximityAlert(i);
            }
            lmsubs.clear();
        }
    }
    
    private int previous_ringer_mode = AudioManager.RINGER_MODE_NORMAL;
    private void updateRing(Context ctx, Intent intent) {
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
    static SharedPreferences getPrefs(Context ctx) {
        return ctx.getSharedPreferences(
                ctx.getApplicationInfo().name,
                Context.MODE_PRIVATE);
    }

    static final int radiusM = 25;
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
}
