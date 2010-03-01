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
