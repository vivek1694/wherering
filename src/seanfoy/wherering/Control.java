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
import android.util.Log;
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
                
        findViewById(R.id.startup).
            setOnClickListener(
                new BroadcastingClickListener(
                    appCtx,
                    fullname(seanfoy.wherering.intent.action.STARTUP)));
        findViewById(R.id.shutdown).
            setOnClickListener(
                new BroadcastingClickListener(
                    appCtx,
                    fullname(seanfoy.wherering.intent.action.SHUTDOWN)));
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
                    fullname(seanfoy.wherering.intent.action.SAY_HI)));
    }
    
    private void updateLastLocation(TextView txt) {
        Context ctx = getApplicationContext();
        LocationManager lm = WRBroadcastReceiver.getSystemService(ctx, Context.LOCATION_SERVICE);
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        String p = lm.getBestProvider(c, true);
        Location l = lm.getLastKnownLocation(p);
        Log.i(getClass().getName(), String.format("updated lastLocation from %s", lm.getProvider(p).getName()));
        txt.setText(String.format("you are at %s", l));
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
