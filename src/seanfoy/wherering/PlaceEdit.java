package seanfoy.wherering;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class PlaceEdit extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.place_edit);
        db = new DBAdapter(getApplicationContext());
        db.open();
        
        Spinner rm = (Spinner)findViewById(R.id.ringer_mode);
        ArrayAdapter<RingerMode> ringers =
            new ArrayAdapter<RingerMode>(getApplicationContext(), R.layout.list_item);
        for (RingerMode m : RingerMode.values()) {
            ringers.add(m);
        }
        rm.setAdapter(ringers);

        fillData();
        
        findViewById(R.id.done).
            setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        try {
                            PlaceEdit.this.setResult(RESULT_OK);
                            asPlace().upsert(db);
                            PlaceEdit.this.finish();
                        }
                        catch (IllegalStateException e) {
                            Toast.makeText(
                                PlaceEdit.this,
                                String.format(
                                    "This place cannot be saved (%s)",
                                    e.getMessage()),
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        findViewById(R.id.revert).
        setOnClickListener(
            new OnClickListener() {
                public void onClick(View v) {
                    setResult(RESULT_CANCELED);
                    PlaceEdit.this.finish();
                }
            });
    }
    
    private void fillData() {
        Intent i = getIntent();
        String latitude = getString(R.string.latitude);
        if (!i.hasExtra(latitude)) return;
        
        Bundle extras = i.getExtras();
        String longitude = getString(R.string.longitude);
        Location l = new Location("whatever");
        l.setLatitude(extras.getDouble(latitude));
        l.setLongitude(extras.getDouble(longitude));
        Place p = Place.fetch(db, l);
        fillData(p);
    }
    private void fillData(Place p) {
        EditText lat = findTypedViewById(R.id.latitude);
        EditText lng = findTypedViewById(R.id.longitude);
        Spinner rm = findTypedViewById(R.id.ringer_mode);
        lat.setText(Double.toString(p.fst.getLatitude()));
        lng.setText(Double.toString(p.fst.getLongitude()));
        rm.setSelection(
            positionFor(
                rm,
                RingerMode.fromMode(p.snd)));
    }
    
    private <T> int positionFor(Spinner s, T x) {
        ArrayAdapter<T> rma = (ArrayAdapter<T>)s.getAdapter();
        int c = rma.getCount();
        for (int i = 0; i < c; ++i) {
            if (x.equals(rma.getItem(i))) return i;
        }
        throw
            new IllegalArgumentException(
                String.format("no such item %s", x));
    }
    
    private enum RingerMode {
        normal(AudioManager.RINGER_MODE_NORMAL),
        silent(AudioManager.RINGER_MODE_SILENT),
        vibrate(AudioManager.RINGER_MODE_VIBRATE);
        
        private RingerMode(int ringer_mode) {
            this.ringer_mode = ringer_mode;
        }
        public final int ringer_mode;
        public static RingerMode fromMode(int rm) {
            for (RingerMode m : values()) {
                if (m.ringer_mode == rm) return m;
            }
            throw new IllegalArgumentException();
        }
    }
    
    private Place asPlace() {
        Location l = new Location("whatever");
        l.setLatitude(extractCoordinate(R.id.latitude));
        l.setLongitude(extractCoordinate(R.id.longitude));
        Spinner rms = findTypedViewById(R.id.ringer_mode);
        ArrayAdapter<RingerMode> rma =
            (ArrayAdapter<RingerMode>)rms.getAdapter();
        RingerMode rm =
            rma.getItem(rms.getSelectedItemPosition());
        return new Place(l, rm.ringer_mode);
    }
    private double extractCoordinate(int id) {
        EditText text = findTypedViewById(id);
        String txt = text.getText().toString();
        try {
            return Double.parseDouble(txt);
        }
        catch (NumberFormatException e) {
            String message = String.format("%s is not a coordinate", txt);
            text.setError(message);
            throw
                new IllegalStateException(
                    message,
                    e);
        }
    }
    private <T> T findTypedViewById(int id) {
        return (T)findViewById(id);
    }
    private DBAdapter db;
}
