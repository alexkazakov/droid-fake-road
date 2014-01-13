package mobi.droid.fakeroad.ui.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import mobi.droid.fakeroad.R;
import mobi.droid.fakeroad.location.MapsHelper;
import mobi.droid.fakeroad.ui.fragments.SearchLocationFragment;

import java.util.LinkedList;

/**
 * Created by ak on 13.01.14.
 */
public abstract class BaseMapViewActivity extends Activity{

    protected MapView mMapView;
    protected GoogleMap mMap;
    protected LinkedList<LatLng> mMarkers = new LinkedList<LatLng>();

    private void assignViews(){
        mMapView = (MapView) findViewById(R.id.mapView);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        assignViews();

        configureMap(savedInstanceState);
    }

    protected void configureMap(Bundle savedInstanceState){
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();//needed to get the map to display immediately

        try{
            MapsInitializer.initialize(this);
            mMap = mMapView.getMap();
            mMap.setMyLocationEnabled(true);
            mMap.setTrafficEnabled(false);

            UiSettings uiSettings = mMap.getUiSettings();
            uiSettings.setAllGesturesEnabled(true);
            uiSettings.setCompassEnabled(true);
            uiSettings.setZoomControlsEnabled(true);
            uiSettings.setMyLocationButtonEnabled(true);
        } catch(GooglePlayServicesNotAvailableException e){
            e.printStackTrace();
            finish();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    protected abstract void onAddMarker(LatLng aLatLng);

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        // Handle item selection
        switch(item.getItemId()){
            case R.id.new_route:
                pushFragment(SearchLocationFragment.class, null, R.id.fragmentHeader);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void pushFragment(Class<? extends Fragment> cls, Bundle args, final int rootID){
        FragmentManager manager = getFragmentManager();
        FragmentTransaction tr = manager.beginTransaction();

        if(manager.findFragmentByTag(cls.getName()) == null){
            tr.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
            tr.replace(rootID, Fragment.instantiate(this, cls.getName(), args), cls.getName());
            tr.commitAllowingStateLoss();
        }
    }

    @Override
    public void onDestroy(){
        if(mMapView != null){
            mMapView.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();
        if(mMapView != null){
            mMapView.onLowMemory();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if(mMapView != null){
            mMapView.onResume();
        }
        MapsHelper.checkIfMockEnabled(this);
    }

    @Override
    public void onPause(){
        super.onPause();
        if(mMapView != null){
            mMapView.onPause();
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState){
        super.onSaveInstanceState(outState);
        if(mMapView != null){
            mMapView.onSaveInstanceState(outState);
        }
    }
}
