package seanfoy.wherering.test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.test.ServiceTestCase;
import android.util.Log;
import seanfoy.wherering.*;

public class TestProximity extends ServiceTestCase<WRService> {
    public void testStartEmpty() {
        final CounterContext ctx = new CounterContext(getContext().getApplicationContext());
        setContext(ctx);
        startService(new Intent(ctx, WRService.class));
        Integer c = ctx.systemServiceCounter.get(Context.ALARM_SERVICE);
        assertNotNull(c);
        assertTrue(c.compareTo(0) > 0);
    }
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        db = new DBAdapter(getContext());
        db.open();
        db.delete(Place.TABLE_NAME, "");
    }
    
    class CounterContext extends ContextWrapper {
        @Override
        public Context getApplicationContext() {
            Log.i("CounterContext", "getApplicationContext()");
            Context appContext = super.getApplicationContext();
            if (appContext == getBaseContext()) return this;
            return new CounterContext(super.getApplicationContext());
        }

        @Override
        public Object getSystemService(String name) {
            Log.i("CounterContext", "getSystemService()");
            systemServiceCounter.put(
                name,
                1 + seanfoy.Greenspun.setDefault(
                        systemServiceCounter,
                        name,
                        0));
            return super.getSystemService(name);
        }
        public Map<String, Integer> systemServiceCounter =
            new HashMap<String, Integer>();
        
        public CounterContext(Context base) {
            super(base);
        }        
    }
    
    public TestProximity() {
        super(WRService.class);
    }
    
    private DBAdapter db;
    
    /**
     * Start the service under test, in the same way as if it was started by
     * {@link android.content.Context#startService Context.startService()}, providing the 
     * arguments it supplied.  If you use this method to start the service, it will automatically
     * be stopped by {@link #tearDown}.
     *  
     * @param intent The Intent as if supplied to {@link android.content.Context#startService}.
     */
    protected void startService(Intent intent) {
        //TODO: report the bug that super.startService
        // calls onStart rather than onStartCommand.
        // onStart is deprecated and onStartCommand
        // is documented to call onStart before returning
        // a stickiness constant from Service.
        // Note: ApplicationContext.startService uses
        // ActivityManagerNative to talk to
        // ActivityManagerService to scheduleServiceArgs
        // on the ActivityThread, which is handled by
        // invoking... onStartCommand.
        assertFalse(mServiceStarted());
        assertFalse(mServiceBound());
        
        if (!mServiceAttached()) {
            setupService();
        }
        assertNotNull(mService());
        
        if (!mServiceCreated()) {
            mService().onCreate();
            set_mServiceCreated(true);
        }
        mService().onStartCommand(intent, serviceFlag, mServiceId());
        
        set_mServiceStarted(true);
    }
    private int serviceFlag = 0;
    private <T> T privateGetter(String fieldname) {
        try {
            Field f = ServiceTestCase.class.getDeclaredField(fieldname);
            f.setAccessible(true);
            return (T)f.get(this);
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    private boolean mServiceStarted() {
        return privateGetter("mServiceStarted");
    }
    private boolean mServiceBound() {
        return privateGetter("mServiceBound");
    }
    private boolean mServiceAttached() {
        return privateGetter("mServiceAttached");
    }
    private Service mService() {
        return privateGetter("mService");
    }
    private boolean mServiceCreated() {
        return privateGetter("mServiceCreated");
    }
    private <T> void privateSetter(String fieldname, T v) {
        try {
            Field f = ServiceTestCase.class.getDeclaredField(fieldname);
            f.setAccessible(true);
            f.set(this, v);
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    private void set_mServiceCreated(boolean v) {
        privateSetter("mServiceCreated", v);
    }
    private int mServiceId() {
        return privateGetter("mServiceId");
    }
    private void set_mServiceStarted(boolean mServiceStarted) {
        privateSetter("mServiceStarted", mServiceStarted);
    }
}
