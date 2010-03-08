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

import org.aspectj.lang.annotation.SuppressAjWarnings;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import seanfoy.wherering.*;

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
			    Object v = b.get(k);
				Log.i(
				    tag,
				    String.format("%s: %s", k, v == null ? "(null)" : v.toString()));
			}
		}
	}
	
	before(int localRingMode) :
	    call(* WRService.updateRing(Context, String, boolean, int)) &&
	    args(Context, String, boolean, localRingMode) {
	    Log.i(thisJoinPoint.toString(), "localRingMode:" + localRingMode);
	}
	
	pointcut ServiceStart(Service service, Intent intent) :
	    this(service) && execution(* onStartCommand(Intent, int, int)) && args(intent, int, int);
	
	before(Service service, Intent intent) : ServiceStart(service, intent) {
	    Log.i(thisJoinPoint.toString(), intent.toString());
	}
	
	//the method spec for call identifies all methods defined
	// on all subtypes of Looper. The warning reminds us that
	// this does not include methods defined on strict
	// supertypes of Looper such as toString, even when these
	// methods are invoked on Looper instances. No worries,
	// I meant what I wrote.
	pointcut dynamicLooperInAsyncTask() :
	    within(seanfoy..*) &&
        (
                cflow(execution(* android.os.AsyncTask+.doInBackground(..))) ||
                cflow(execution(* android.os.AsyncTask+.publishProgress(..)))) &&
	    call(* android.os.Looper+.*(..)) &&
	    !within(Logging);
    @SuppressAjWarnings("unmatchedSuperTypeInCall")
	before() : dynamicLooperInAsyncTask() {
        // http://groups.google.com/group/android-developers/browse_thread/thread/8fd0b86f1dd310b8/3917193dbc42c494?hl=en&lnk=gst&q=looper+inside+an+asynctask#3917193dbc42c494
	    String tag = thisJoinPoint.toString();
	    Log.w(tag, "Looper expects to to own the thread that you associate it with, while AsyncTask owns the thread it creates for you to run in thebackground. They thus conflict with each other, and can't be used together. Consider using seanfoy.AsyncLooperTask instead.");
	}
    
    before() :
        cflow(call(* seanfoy.ResourceManagement.cleanup(..))) &&
        call(* seanfoy.Greenspun.Disposable.close(..)) {
        Log.w(thisJoinPoint.toString(), "cleaning up disposable");
    }
}
