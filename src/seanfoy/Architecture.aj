package seanfoy;

public aspect Architecture {
    declare warning :
        within(seanfoy..*) &&
        within(android.content.BroadcastReceiver) &&
        call(* android.content.Context+.bindService(..)):
            // http://developer.android.com/intl/fr/reference/android/content/BroadcastReceiver.html
            "You may not show a dialog or bind to a service from within a BroadcastReceiver. For the former, you should instead use the NotificationManager API. For the latter, you can use Context.startService() to send a command to the service.";
}
