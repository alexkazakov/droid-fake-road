package mobi.droid.fakeroad.ui.activity;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;
import mobi.droid.fakeroad.R;
import mobi.droid.fakeroad.location.MapsHelper;
import mobi.droid.fakeroad.ui.fragments.SearchLocationFragment;

import java.util.LinkedList;

public class MainActivity extends Activity{

    private final LinkedList<PolylineOptions> mPolylines = new LinkedList<PolylineOptions>();
    private MapView mMapView;
    private GoogleMap mMap;
    private LinkedList<LatLng> mMarkers = new LinkedList<LatLng>();
    private IconGenerator mIconGenerator;

    private void assignViews(){
        mMapView = (MapView) findViewById(R.id.mapView);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        assignViews();

        mIconGenerator = new IconGenerator(this);
        mIconGenerator.setBackground(new ColorDrawable(Color.LTGRAY));
        mIconGenerator.setContentPadding(2, 2, 2, 2);

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
                        distanceMarker.position(
                                MapsHelper.calcLngLat(oldLast, distance, MapsHelper.bearing(oldLast, aLatLng)));
                        distanceMarker.draggable(false);
                        distanceMarker.visible(true);

                        // TODO use https://github.com/googlemaps/android-maps-utils to generate icons with text
                        Bitmap icon;
                        if(distance < 1000){
                            icon = mIconGenerator.makeIcon(String.valueOf(distance) + " m");
                        } else{
                            icon = mIconGenerator.makeIcon(
                                    String.valueOf(distance / 1000) + "." + String.valueOf(distance % 1000) + " km");
                        }
                        distanceMarker.icon(BitmapDescriptorFactory.fromBitmap(icon));
                        mMap.addMarker(distanceMarker);
                    }
                }
            });

        } catch(GooglePlayServicesNotAvailableException e){
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void calculateRoute(final LatLng aStart, final LatLng aEnd){

        Routing routing = new Routing(Routing.TravelMode.DRIVING);
        routing.registerListener(new RoutingListener(){

            @Override
            public void onRoutingFailure(){
                int i = 0;
            }

            @Override
            public void onRoutingStart(){
                int i = 0;
            }

            @Override
            public void onRoutingSuccess(final PolylineOptions mPolyOptions){
                mMap.clear();

                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.color(Color.BLUE);
                polylineOptions.width(10);
                polylineOptions.addAll(mPolyOptions.getPoints());

                mMap.addPolyline(polylineOptions);

                addMarkerStart(aStart);
                addMarkerEnd(aEnd);
            }
        });
        routing.execute(aStart, aEnd);
        mMap.clear();
    }

    public void addMarkerStart(LatLng aLatLng){
        // Start marker
        MarkerOptions options = new MarkerOptions();
        options.position(aLatLng);
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue));
        mMap.addMarker(options);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(aLatLng));

    }

    public void addMarkerEnd(LatLng aLatLng){
        // End marker
        MarkerOptions options = new MarkerOptions();
        options.position(aLatLng);
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green));
        mMap.addMarker(options);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(aLatLng));
    }

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
