package seanfoy.wherering;

import static seanfoy.wherering.intent.IntentHelpers.fullname;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import seanfoy.Greenspun.Pair;
import seanfoy.wherering.intent.action;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.util.Log;

public class WRBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(fullname(action.PROXIMITY))) {
            updateRing(context, intent, AudioManager.RINGER_MODE_SILENT);
        }
        else if (intent.getAction().equals(fullname(action.SAY_HI))) {
            raiseAlert(context);
        }
        else if (intent.getAction().equals(fullname(action.SHUTDOWN))) {
            unsubscribe(context);
        }
        else if (intent.getAction().equals(android.content.Intent.ACTION_BOOT_COMPLETED)) {
            if (!getPrefs(context).getBoolean(android.content.Intent.ACTION_BOOT_COMPLETED, true)) {
                return;
            }
            subscribe(context, getConfig(context));
        }
        else if (intent.getAction().equals(fullname(action.STARTUP))) {
            subscribe(context, getConfig(context));
        }
    }
    
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

    private static volatile Object lmsubsLock = new Object();
    private final static Set<PendingIntent> lmsubs =
        new HashSet<PendingIntent>(); 

    @SuppressWarnings("unchecked")
    public static <T> T getSystemService(Context ctx, String name) {
        return (T)ctx.getSystemService(name);
    }
    
    private void subscribe(Context ctx, Map<Location, Integer> config) {
        LocationManager lm = getSystemService(ctx, Context.LOCATION_SERVICE);
        synchronized(lmsubsLock) {
            for (Map.Entry<Location, Integer> c : config.entrySet()) {
                PendingIntent i =
                    PendingIntent.getBroadcast(
                        ctx,
                        // docs say "currently not used"
                        0,
                        new Intent(fullname(action.PROXIMITY)),
                        0);
                lmsubs.add(i);
                lm.addProximityAlert(
                    c.getKey().getLatitude(),
                    c.getKey().getLongitude(),
                    radiusM,
                    // never time out
                    // If this app is killed, the
                    // PA will persist until reboot.
                    // Maybe it would be better to
                    // let this PA die periodically
                    // with replacement, courtesy of
                    // an Alarm.
                    -1,
                    i);
                PendingIntent j =
                    PendingIntent.getBroadcast(
                            ctx, 0, new Intent(fullname(action.SAY_HI)), 0);
                lmsubs.add(j);
                lm.addProximityAlert(
                    c.getKey().getLatitude(),
                    c.getKey().getLongitude(),
                    radiusM,
                    -1,
                    j);
            }
        }
    }

    private void unsubscribe(Context ctx) {
        Log.i(getClass().getName(), "unsub");
        LocationManager lm = getSystemService(ctx, Context.LOCATION_SERVICE);
        synchronized(lmsubsLock) {
            for (PendingIntent i : lmsubs) {
                lm.removeProximityAlert(i);
            }
        }
    }
    
    private void updateRing(Context ctx, Intent intent, Integer ringerMode) {
        AudioManager am = getSystemService(ctx, Context.AUDIO_SERVICE);
        am.setRingerMode(ringerMode);
    }
    static SharedPreferences getPrefs(Context ctx) {
        return ctx.getSharedPreferences(
                ctx.getApplicationInfo().name,
                Context.MODE_PRIVATE);
    }

    static final int radiusM = 25;
    private final static Map<Location, Integer> getConfig(Context ctx) {
        HashMap<Location, Integer> config = new HashMap<Location, Integer>();
        DBAdapter db = new DBAdapter(ctx);
        try {
            db.open();
            for (Pair<Location, Integer> p : db.allPlaces()) {
                config.put(p.fst, p.snd);
            }
        }
        finally {
            db.close();
        }
        return config;
    }
}
