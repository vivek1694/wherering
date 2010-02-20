package seanfoy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public aspect Logging {
	// type parameters aren't available at runtime
	@SuppressWarnings("unchecked")
	pointcut BroadcastReceipt(BroadcastReceiver receiver, Intent intent):
		this(receiver) &&
		args(*, intent) &&
		execution(void onReceive(Context, Intent));

	// type parameters aren't available at runtime
	@SuppressWarnings("unchecked")
	before(BroadcastReceiver receiver, Intent intent): BroadcastReceipt(receiver, intent) {
	    String tag = receiver.getClass().getName();
	    Log.i(
		    tag,
			String.format(
				"%s received %s",
				receiver.getClass().getName(),
				intent));
		Bundle b = intent.getExtras();
		if (b != null) {
			for (String k : b.keySet()) {
				Log.i(
				    tag,
				    String.format("%s: %s", k, b.get(k).toString()));
			}
		}
	}
}
