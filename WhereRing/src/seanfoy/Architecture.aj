/*
 * Copyright 2010 Sean M. Foy
 * 
 *  This program is free software: you can redistribute it and/or modify
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

public aspect Architecture {
    declare warning :
        within(seanfoy..*) &&
        within(android.content.BroadcastReceiver) &&
        call(* android.content.Context+.bindService(..)):
            // http://developer.android.com/intl/fr/reference/android/content/BroadcastReceiver.html
            "You may not show a dialog or bind to a service from within a BroadcastReceiver. For the former, you should instead use the NotificationManager API. For the latter, you can use Context.startService() to send a command to the service.";
    declare warning :
        within(seanfoy..*) &&
        within(android.os.AsyncTask+) &&
        call(* android.os.Looper+.*(..)) :
            // http://groups.google.com/group/android-developers/browse_thread/thread/8fd0b86f1dd310b8/3917193dbc42c494?hl=en&lnk=gst&q=looper+inside+an+asynctask#3917193dbc42c494
            "Looper expects to to own the thread that you associate it with, while AsyncTask owns the thread it creates for you to run in thebackground. They thus conflict with each other, and can't be used together. Consider using seanfoy.AsyncLooperTask instead.";
    declare warning :
        within(seanfoy..*) &&
        !within(Logging) &&
        call(* android.util.Log.*(..)) :
        "Use aspects for logging.";
}
