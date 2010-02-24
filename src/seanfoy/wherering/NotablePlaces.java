package seanfoy.wherering;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class NotablePlaces extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notable_places);

        db = new DBAdapter(getApplicationContext());
        db.open();

        fillData();
        ListView placeList =
            (ListView)findViewById(R.id.place_list);
        placeList.setOnItemClickListener(
            new OnItemClickListener() {
                public void onItemClick(AdapterView<?> aview, View view,
                        int position, long id) {
                    editPlace((Place)aview.getItemAtPosition(position));
                }
            });
        registerForContextMenu(placeList);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        db.open();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        db.close();
    }
    
    public void editPlace(Place place) {
        Intent edit =
            new Intent(
                    android.content.Intent.ACTION_EDIT,
                    null,
                    getApplicationContext(),
                    PlaceEdit.class);
        if (place != null) {
            edit.putExtra(
                getString(R.string.latitude),
                place.location.getLatitude());
            edit.putExtra(
                getString(R.string.longitude),
                place.location.getLongitude());
        }
        startActivityForResult(
            edit,
            PLACE_EDIT);
    }

    private void fillData() {
        ListView placeList =
            (ListView)findViewById(R.id.place_list);
        ArrayAdapter<Place> places =
            new ArrayAdapter<Place>(this, R.layout.list_item) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    TextView result = (TextView)super.getView(position, convertView, parent);
                    result.setText(getItem(position).name);
                    result.getLayoutParams().width = ViewGroup.LayoutParams.FILL_PARENT;
                    return result;
                }
            };
        for (Place p : Place.allPlaces(db)) {
            places.add(p);
        }
        placeList.setAdapter(places);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(
            android.view.Menu.NONE,
            R.string.add_place,
            android.view.Menu.NONE,
            R.string.add_place);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != R.string.add_place) return false;
        startActivityForResult(
            new Intent(getApplicationContext(), PlaceEdit.class),
            PLACE_EDIT);
        return true;
    }
    
    private enum ContextActions {
        DELETE("delete") {
            @Override
            public void selected(NotablePlaces activity, Place place) {
                place.delete(activity.db);
                activity.fillData();
            }
        },
        EDIT("edit") {
            @Override
            public void selected(NotablePlaces activity, Place place) {
                activity.editPlace(place);
            }
        };
        
        public String title;
        ContextActions(String title) {
            this.title = title;
        }
        public abstract void selected(NotablePlaces activity, Place place);
    }
    
    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (requestCode != PLACE_EDIT) throw new IllegalArgumentException("unrecognized request");
        if (responseCode != RESULT_OK) return;
        fillData();
    }
    
    private static final int PLACE_EDIT = 1;
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        for (ContextActions a : ContextActions.values()) {
            menu.add(
                android.view.Menu.NONE,
                a.ordinal(),
                android.view.Menu.NONE,
                a.title);
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContextActions [] A = ContextActions.values();
        int which = item.getItemId();
        if (which >= A.length) return super.onContextItemSelected(item);
        AdapterContextMenuInfo menuInfo =
            (AdapterContextMenuInfo)item.getMenuInfo();
        ArrayAdapter<Place> placeAdapter =
            (ArrayAdapter<Place>)((ListView)findViewById(R.id.place_list)).getAdapter();
        ContextActions.values()[item.getItemId()].selected(
            this,
            placeAdapter.getItem(menuInfo.position));
        return true;
    }
    
    private DBAdapter db;
}
