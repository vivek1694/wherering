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

import seanfoy.wherering.Place.RingerMode;
import seanfoy.wherering.intent.action;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class WRBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (intentAction.equals(fullname(action.ALERT))) {
            raiseAlert(context, intent);
        }
        else if (intentAction.equals(android.content.Intent.ACTION_BOOT_COMPLETED)) {
            if (!getPrefs(context).getBoolean(android.content.Intent.ACTION_BOOT_COMPLETED, true)) {
                return;
            }
            context.startService(new Intent(context.getApplicationContext(), WRService.class));
        }
    }
    
    private void raiseAlert(Context context, Intent intent) {
        String app_name = context.getString(R.string.app_name);
        String app_ticker = context.getString(R.string.alert_ticker);
        String alert_text = AlertExtras.describe(context, intent.getExtras());
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

    public enum AlertExtras {
        PLACE_NAME,
        ENTERING,
        UPDATED,
        NEW_RINGER_MODE;
        
        public static Bundle asBundle(String placeName, boolean entering, boolean updated, int newRingerMode) {
            Bundle result = new Bundle();
            result.putString(PLACE_NAME.toString(), placeName);
            result.putBoolean(ENTERING.toString(), entering);
            result.putBoolean(UPDATED.toString(), updated);
            result.putInt(NEW_RINGER_MODE.toString(), newRingerMode);
            return result;
        }
        public static String describe(Context ctx, Bundle b) {
            String event, outcome;
            try {
                event =
                    String.format(
                        ctx.getString(b.getBoolean(ENTERING.toString()) ? R.string.entering : R.string.leaving),
                        b.getString(PLACE_NAME.toString()));
                outcome =
                    String.format(
                        ctx.getString(b.getBoolean(UPDATED.toString()) ? R.string.change : R.string.nochange),
                        RingerMode.fromInt(b.getInt(NEW_RINGER_MODE.toString())));
            }
            catch (NullPointerException e) {
                event = ctx.getString(R.string.name);
                outcome = ctx.getString(R.string.ringer_mode);
            }
            return
                String.format(
                    ctx.getString(R.string.alert_text),
                    event,
                    outcome);
        }
        public static boolean interesting(Bundle b) {
            String upd = UPDATED.toString();
            return b.containsKey(upd) && b.getBoolean(upd);
        }
    }
}