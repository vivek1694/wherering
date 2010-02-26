package seanfoy.wherering;

import static seanfoy.wherering.intent.IntentHelpers.fullname;

import seanfoy.wherering.intent.action;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class WRBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (intentAction.equals(fullname(action.ALERT))) {
            raiseAlert(context);
        }
        else if (intentAction.equals(android.content.Intent.ACTION_BOOT_COMPLETED)) {
            if (!getPrefs(context).getBoolean(android.content.Intent.ACTION_BOOT_COMPLETED, true)) {
                return;
            }
            context.startService(new Intent(context.getApplicationContext(), WRService.class));
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

    static SharedPreferences getPrefs(Context ctx) {
        return ctx.getSharedPreferences(
                ctx.getApplicationInfo().name,
                Context.MODE_PRIVATE);
    }
}
