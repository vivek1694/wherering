package seanfoy.wherering;

import static seanfoy.wherering.intent.IntentHelpers.fullname;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class Control extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control);
        
        class BroadcastingClickListener implements OnClickListener {
            private String action;
            public BroadcastingClickListener(String action) {
                this.action = action;
            }
            public void onClick(View v) {
                getApplicationContext().
                    sendBroadcast(
                        new Intent(
                            action));
            }
        }
        
        findViewById(R.id.startup).
            setOnClickListener(
                new BroadcastingClickListener(
                    fullname(seanfoy.wherering.intent.action.STARTUP)));
        findViewById(R.id.shutdown).
            setOnClickListener(
                new BroadcastingClickListener(
                    fullname(seanfoy.wherering.intent.action.SHUTDOWN)));
        TextView txt = (TextView)findViewById(R.id.txt);
        txt.setOnClickListener(
            new OnClickListener() {
                public void onClick(View v) {
                    updateTxt((TextView)v);
                }
            });
        updateTxt(txt);
        
        findViewById(R.id.alert).
            setOnClickListener(
                new BroadcastingClickListener(
                    fullname(seanfoy.wherering.intent.action.SAY_HI)));
    }
    
    private void updateTxt(TextView txt) {
        Context ctx = getApplicationContext();
        LocationManager lm = WRBroadcastReceiver.getSystemService(ctx, ctx.LOCATION_SERVICE);
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        String p = lm.getBestProvider(c, true);
        Location l = lm.getLastKnownLocation(p);
        Log.i(getClass().getName(), String.format("updated txt from %s", lm.getProvider(p).getName()));
        txt.setText(String.format("you are at %s", l));
    }
}
