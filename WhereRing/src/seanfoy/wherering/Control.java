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
import seanfoy.Greenspun.Func1;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
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
                        Boolean emptyDB =
                            DBAdapter.withDBAdapter(appCtx, new Func1<DBAdapter, Boolean>() {
                                public Boolean f(DBAdapter db) {
                                    return Place.emptyDB(db);
                                }
                            });
                        startActivity(
                            new Intent(
                                Control.this,
                                emptyDB ? PlaceEdit.class : NotablePlaces.class));
                        if (emptyDB) {
                            Toast.makeText(
                                appCtx,
                                String.format(
                                    getString(R.string.empty_places),
                                    getString(R.string.notable_places)),
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        findViewById(R.id.activate).
            setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        appCtx.startService(new Intent(appCtx, WRService.class));
                    }
                });
        findViewById(R.id.deactivate).
            setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        appCtx.stopService(new Intent(appCtx, WRService.class));
                    }
                });
        ((CheckBox)findViewById(R.id.initd)).
            setOnCheckedChangeListener(
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

        setupView();
    }
    
    private void setupView() {
        Context appCtx = getApplicationContext();
        SharedPreferences prefs = WRBroadcastReceiver.getPrefs(appCtx);
        CheckBox initd = (CheckBox)findViewById(R.id.initd);
        initd.setChecked(prefs.getBoolean(android.content.Intent.ACTION_BOOT_COMPLETED, true));
        final TextView welcome =
            (TextView)findViewById(R.id.welcome);
        welcome.setText(
                String.format(
                    getText(R.string.welcome).toString(),
                    getText(R.string.notable_places)));
        DBAdapter.withDBAdapter(
                appCtx,
                new seanfoy.Greenspun.Func1<DBAdapter, Void>() {
                    public Void f(DBAdapter db) {
                        if (!Place.emptyDB(db)) {
                            welcome.setVisibility(View.GONE);
                        }
                        return null;
                    }
                });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        setupView();
    }
        
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	menu.add(0, R.string.known_issues, 0, getString(R.string.known_issues));
    	menu.add(0, R.string.suggestion_box, 0, getString(R.string.suggestion_box));
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (super.onOptionsItemSelected(item)) return true;
    	if (item.getItemId() == R.string.suggestion_box) {
        	startActivity(
                new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://code.google.com/p/wherering/issues/entry?template=Defect%20report%20from%20user")));
        	return true;
    	}
    	else if (item.getItemId() == R.string.known_issues) {
    	    startActivity(
    	        new Intent(
    	            Intent.ACTION_VIEW,
    	            Uri.parse("http://code.google.com/p/wherering/issues")));
    	    return true;
    	}
        return false;
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
