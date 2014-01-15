package mobi.droid.fakeroad.ui.activity;

import android.app.*;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.ui.IconGenerator;
import mobi.droid.fakeroad.R;
import mobi.droid.fakeroad.location.MapsHelper;
import mobi.droid.fakeroad.service.FakeLocationService;
import mobi.droid.fakeroad.ui.fragments.PathFragment;
import mobi.droid.fakeroad.ui.fragments.SearchLocationFragment;

import java.util.LinkedList;
import java.util.List;

public class MainActivity extends BaseMapViewActivity{

    private IconGenerator mIconGenerator;
    ///
    private MarkerOptions mRouteStartMarkerOptions;
    private MarkerOptions mRouteEndMarkerOptions;
    private LinkedList<LatLng> mMarkers = new LinkedList<LatLng>();
    private List<LatLng> mRoutingPoints;
    ///
    private ProgressDialog mProgressDialog;
    boolean calculateRoute = true; //TODO need add settings.


    private int mSpeed;

    private void cleanup(){
        mMap.clear();
        mMarkers.clear();
        mRouteStartMarkerOptions = null;
        mRouteEndMarkerOptions = null;
        mRoutingPoints = null;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        pushFragment(SearchLocationFragment.class, null, R.id.fragmentHeader);
        if(mMap != null){
            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener(){

                @Override
                public void onMapLongClick(final LatLng aLatLng){
                    onAddMarker(aLatLng);
                }
            });
        }
    }

    @Override
    protected void configureMap(final Bundle savedInstanceState){
        super.configureMap(savedInstanceState);

        mIconGenerator = new IconGenerator(this);
        mIconGenerator.setBackground(new ColorDrawable(Color.LTGRAY));
        mIconGenerator.setContentPadding(2, 2, 2, 2);
    }

    @Override
    protected void onAddMarker(final LatLng aLatLng){
        MarkerOptions pointMarker = new MarkerOptions();
        pointMarker.draggable(false);
        if(mMarkers.isEmpty()){
            pointMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue));

        }
        pointMarker.position(aLatLng);
        pointMarker.visible(true);
        mMap.addMarker(pointMarker);

        if(!mMarkers.isEmpty()){
            LatLng last = mMarkers.getLast();
            if(calculateRoute){
                calculateRoute(aLatLng, last);
            } else{
                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.add(last, aLatLng);
                polylineOptions.width(5);
                mMap.addPolyline(polylineOptions);
            }
        }
        LatLng oldLast = mMarkers.peekLast();
        mMarkers.add(aLatLng);
        if(mMarkers.size() > 1){
            int distance = (int) MapsHelper.distance(oldLast, aLatLng) / 2;
            addDistanceMarker(distance, MapsHelper.calcLngLat(oldLast, distance, MapsHelper.bearing(oldLast, aLatLng)));

        }
    }

    private void addDistanceMarker(final int aDistance, final LatLng aPosition){
        MarkerOptions distanceMarker = new MarkerOptions();

        distanceMarker.position(aPosition);
        distanceMarker.draggable(false);
        distanceMarker.visible(true);

        String text = aDistance < 1000 ?
                String.valueOf(aDistance) + " m" :
                String.valueOf(aDistance / 1000) + "." + String.valueOf(aDistance % 1000) + " km";
        Bitmap icon = mIconGenerator.makeIcon(text);
        distanceMarker.icon(BitmapDescriptorFactory.fromBitmap(icon));
        mMap.addMarker(distanceMarker);
    }

    public void addMarkerStart(LatLng aLatLng){
        // Start marker
        if(mRouteStartMarkerOptions != null){
            mRouteStartMarkerOptions.visible(false);
        }
        mRouteStartMarkerOptions = new MarkerOptions();
        mRouteStartMarkerOptions.position(aLatLng);
        mRouteStartMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue));

        mMap.addMarker(mRouteStartMarkerOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(aLatLng));
    }

    public void addMarkerEnd(LatLng aLatLng){
        // End marker
        if(mRouteEndMarkerOptions != null){
            mRouteEndMarkerOptions.visible(false);
        }
        mRouteEndMarkerOptions = new MarkerOptions();
        mRouteEndMarkerOptions.position(aLatLng);
        mRouteEndMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green));

        mMap.addMarker(mRouteEndMarkerOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(aLatLng));
    }

    public void calculateRoute(final LatLng aFrom, final LatLng aTo){
        MapsHelper.calculateRoute(aFrom, aTo, new RoutingListener(){

            @Override
            public void onRoutingFailure(){
                hideProgress();
                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.add(aFrom, aTo);
                polylineOptions.width(5);
                mMap.addPolyline(polylineOptions);
                Toast.makeText(MainActivity.this, "Failed to determine routing", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRoutingStart(){
                String message = "Routing calculation...";
                showProgress(message);
            }

            @Override
            public void onRoutingSuccess(final PolylineOptions aPolyOptions){
                hideProgress();
                if(mMap != null){
//                    mMap.clear();

//                    // Start marker
//                    MarkerOptions options = new MarkerOptions();
//                    options.position(aFrom);
//                    options.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue));
//                    mMap.addMarker(options);
//
//                    // End marker
//                    options.position(aTo);
//                    options.icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green));
//                    mMap.addMarker(options);
                    LatLngBounds.Builder include = LatLngBounds.builder().include(aFrom).include(aTo);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(include.build(), 25));

                    PolylineOptions polylineOptions = new PolylineOptions();
                    polylineOptions.color(Color.BLUE);
                    polylineOptions.width(10);
                    mRoutingPoints = aPolyOptions.getPoints();
                    polylineOptions.addAll(mRoutingPoints);

                    double distance = MapsHelper.distance(mRoutingPoints);
                    addDistanceMarker((int) distance, mRoutingPoints.get(mRoutingPoints.size() / 2));
                    mMap.addPolyline(polylineOptions);

                }
            }
        });
    }

    @Override
    public void onPause(){
        hideProgress();
        super.onPause();
    }

    private void showProgress(final String aMessage){
        mProgressDialog = ProgressDialog.show(this, null, aMessage, true);
    }

    private void hideProgress(){
        try{
            mProgressDialog.dismiss();
        } catch(Exception ignored){
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.routing_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        // Handle item selection
        // TODO
        switch(item.getItemId()){
            case R.id.action_new_route:
                cleanup();
                break;
            case R.id.action_start_route:
                showSpeedDialog();
                break;
            case R.id.action_stop_route:
                FakeLocationService.stop(this);
                break;
            case R.id.action_add_new_point:

                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void showSpeedDialog(){
        mSpeed = 10;
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        final SeekBar seekBar = new SeekBar(this);
        ab.setView(seekBar);
        seekBar.setMax(99);
        seekBar.setProgress(mSpeed);
        seekBar.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                             ViewGroup.LayoutParams.WRAP_CONTENT));
        ab.setTitle(String.format("Movement speed: %d m/s", mSpeed));
        ab.setPositiveButton("Go", new DialogInterface.OnClickListener(){

            @Override
            public void onClick(final DialogInterface dialog, final int which){
                dialog.dismiss();
                FakeLocationService.start(MainActivity.this, mSpeed, -1, mMarkers);
            }
        });
        ab.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){

            @Override
            public void onClick(final DialogInterface dialog, final int which){
                dialog.dismiss();
            }
        });

        final AlertDialog alertDialog = ab.show();
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser){
                mSpeed = (1 + progress);
                alertDialog.setTitle(String.format("Movement speed: %d m/s", mSpeed));
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar){
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar){
            }
        });
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

}

