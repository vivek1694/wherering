package seanfoy.wherering;

import static seanfoy.wherering.intent.IntentHelpers.fullname;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import seanfoy.wherering.intent.action;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
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
        else {
            subscribe(context, getConfig(context));
        }
    }
    
    private void raiseAlert(Context context) {
            NotificationManager nm =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification note = new Notification(R.drawable.icon, "gbr receipt", System.currentTimeMillis());
            note.setLatestEventInfo(
                    context,
                    "title",
                    "GeographicBroadcastReceiver here.",
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
        AudioManager am = getSystemService(ctx, ctx.AUDIO_SERVICE);
        am.setRingerMode(ringerMode);
    }
    private static final int radiusM = 25;
    private final static Map<Location, Integer> getConfig(Context ctx) {
        HashMap<Location, Integer> config = new HashMap<Location, Integer>();
        RingtoneManager rm = new RingtoneManager(ctx);
        Integer homeRing = AudioManager.RINGER_MODE_NORMAL;
        Integer parkRing = AudioManager.RINGER_MODE_VIBRATE;
        Location l = new Location("whatever");
        l.setAccuracy(radiusM);
        // 1805 Park
        l.setLatitude(38.629205);
        l.setLongitude(-90.226615);
        config.put(l, parkRing);
        l = new Location(l);
        // 2050 Lafayette
        l.setLatitude(38.616951);
        l.setLongitude(-90.21152);
        config.put(l, homeRing);
        return config;
    }
}
