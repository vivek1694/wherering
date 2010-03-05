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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class Control extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control);
        final Context appCtx = getApplicationContext();

        findViewById(R.id.notable_places).
            setOnClickListener(
                new OnClickListener() {
                    public void onClick(View arg0) {
                        startActivity(
                            new Intent(Control.this, NotablePlaces.class));
                    }
                });
        findViewById(R.id.startup).
            setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        appCtx.startService(new Intent(appCtx, WRService.class));
                    }
                });
       findViewById(R.id.shutdown).
            setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        appCtx.stopService(new Intent(appCtx, WRService.class));
                    }
                });
        TextView lastLocation = (TextView)findViewById(R.id.lastLocation);
        lastLocation.setOnClickListener(
            new OnClickListener() {
                public void onClick(View v) {
                    updateLastLocation((TextView)v);
                }
            });
        updateLastLocation(lastLocation);
        
        SharedPreferences prefs = WRBroadcastReceiver.getPrefs(appCtx);
        CheckBox initd = (CheckBox)findViewById(R.id.initd);
        initd.setChecked(prefs.getBoolean(android.content.Intent.ACTION_BOOT_COMPLETED, true));
        initd.setOnCheckedChangeListener(
            new OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    WRBroadcastReceiver.getPrefs(appCtx).
                        edit().
                        putBoolean(android.content.Intent.ACTION_BOOT_COMPLETED, isChecked).
                        commit();
                }
            });
        
        findViewById(R.id.alert).
            setOnClickListener(
                new BroadcastingClickListener(
                    appCtx,
                    fullname(seanfoy.wherering.intent.action.ALERT)));
    }
    
    private void updateLastLocation(TextView txt) {
        Context ctx = getApplicationContext();
        LocationManager lm = WRService.getSystemService(ctx, Context.LOCATION_SERVICE);
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        String p = lm.getBestProvider(c, true);
        Location l = lm.getLastKnownLocation(p);
        txt.setText(String.format(getString(R.string.last_location), l));
    }
    
    private static class BroadcastingClickListener implements OnClickListener {
        private Context appCtx;
        private String action;
        public BroadcastingClickListener(Context appCtx, String action) {
            this.appCtx = appCtx;
            this.action = action;
        }
        public void onClick(View v) {
            appCtx.
                sendBroadcast(
                    new Intent(
                        action));
        }
    }
}
