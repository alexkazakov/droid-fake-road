package mobi.droid.fakeroad.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import mobi.droid.fakeroad.R;

import java.util.LinkedList;

public class MainActivity extends Activity{

    private MapView mMapView;
    private GoogleMap mMap;
    private LinkedList<LatLng> mMarkers = new LinkedList<LatLng>();
    private void assignViews(){
        mMapView = (MapView) findViewById(R.id.mapView);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        assignViews();

        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();//needed to get the map to display immediately

        try{
            MapsInitializer.initialize(this);
            mMap = mMapView.getMap();
            mMap.setMyLocationEnabled(true);
            UiSettings uiSettings = mMap.getUiSettings();
            uiSettings.setAllGesturesEnabled(true);
            uiSettings.setCompassEnabled(true);
            uiSettings.setZoomControlsEnabled(true);
            uiSettings.setMyLocationButtonEnabled(true);
            mMap.setTrafficEnabled(false);

            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener(){

                @Override
                public void onMapLongClick(final LatLng aLatLng){
                    MarkerOptions options = new MarkerOptions();
                    options.position(aLatLng);
                    options.visible(true);
                    mMap.addMarker(options);

                    if(!mMarkers.isEmpty()){
                        LatLng last = mMarkers.getLast();

                        PolylineOptions polylineOptions = new PolylineOptions();
                        polylineOptions.add(last, aLatLng);
                        polylineOptions.width(5);
                        mMap.addPolyline(polylineOptions);
                    }
                    mMarkers.add(aLatLng);
                }
            });
        } catch(GooglePlayServicesNotAvailableException e){
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
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
