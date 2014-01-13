package mobi.droid.fakeroad.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import mobi.droid.fakeroad.R;
import mobi.droid.fakeroad.location.MapsHelper;

import java.util.LinkedList;

public class MainActivity extends Activity{

    private MapView mMapView;
    private GoogleMap mMap;
    private LinkedList<LatLng> mMarkers = new LinkedList<LatLng>();
    private final LinkedList<PolylineOptions> mPolylines = new LinkedList<PolylineOptions>();

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
                    MarkerOptions pointMarker = new MarkerOptions();
                    pointMarker.draggable(false);
                    pointMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                    pointMarker.position(aLatLng);
                    pointMarker.visible(true);
                    mMap.addMarker(pointMarker);

                    if(!mMarkers.isEmpty()){
                        LatLng last = mMarkers.getLast();

                        PolylineOptions polylineOptions = new PolylineOptions();
                        polylineOptions.add(last, aLatLng);
                        polylineOptions.width(5);
                        mPolylines.addLast(polylineOptions);
                        mMap.addPolyline(polylineOptions);
                    }
                    LatLng oldLast = mMarkers.peekLast();
                    mMarkers.add(aLatLng);
                    if(mMarkers.size() > 1){
                        MarkerOptions distanceMarker = new MarkerOptions();
                        int distance = (int) MapsHelper.distance(oldLast, aLatLng) / 2;
                        distanceMarker.position(MapsHelper.calcLngLat(oldLast, distance, MapsHelper.bearing(oldLast, aLatLng)));
                        distanceMarker.draggable(false);
                        distanceMarker.visible(true);

                        // TODO use https://github.com/googlemaps/android-maps-utils to generate icons with text

                        distanceMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                        distanceMarker.title(String.valueOf(distance) + "m");
                        mMap.addMarker(distanceMarker);
                    }
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
        checkIfMockEnabled();
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

    private void checkIfMockEnabled(){
        try{
            int mock_location = Settings.Secure.getInt(getContentResolver(), "mock_location");
            if(mock_location == 0){
                try{
                    Settings.Secure.putInt(getContentResolver(), "mock_location", 1);
                } catch(Exception ignored){
                }
                mock_location = Settings.Secure.getInt(getContentResolver(), "mock_location");
            }

            if(mock_location == 0){
                AlertDialog.Builder ab = new AlertDialog.Builder(this);
                ab.setCancelable(false);
                ab.setMessage("Enable 'Mock locations' to use this application");
                ab.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(final DialogInterface dialog, final int which){
                        startActivity(new Intent().setClassName("com.android.settings",
                                                                "com.android.settings.DevelopmentSettings"));
                    }
                });
                ab.show();
            }
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }

}
