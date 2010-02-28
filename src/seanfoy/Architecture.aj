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
        call(* android.os.Looper.*(..)) :
            // http://groups.google.com/group/android-developers/browse_thread/thread/8fd0b86f1dd310b8/3917193dbc42c494?hl=en&lnk=gst&q=looper+inside+an+asynctask#3917193dbc42c494
            "Looper expects to to own the thread that you associate it with, while AsyncTask owns the thread it creates for you to run in thebackground. They thus conflict with each other, and can't be used together. Consider using seanfoy.AsyncLooperTask instead.";
}
