package mobi.droid.fakeroad.ui.activity;

import android.app.*;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.ui.IconGenerator;
import mobi.droid.fakeroad.R;
import mobi.droid.fakeroad.location.MapsHelper;
import mobi.droid.fakeroad.service.FakeLocationService;
import mobi.droid.fakeroad.ui.view.AutoCompleteAddressTextView;

import java.util.*;

public class MainActivity extends BaseMapViewActivity implements LocationListener, GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener{

    public static final String APP_PREFERENCES = "map";
    public static final String PREF_DIRECTION_CALCULATE = "direction.calculate";
    public static final String PREF_DIRECTION_TRAVELMODE = "direction.travelmode";
    MarkerOptions moveMarker = new MarkerOptions();
    private IconGenerator mIconGenerator;
    private LinkedList<LatLng> mPoints = new LinkedList<LatLng>();
    ///
    private ProgressDialog mProgressDialog;
    private Routing.TravelMode mTravelMode = Routing.TravelMode.DRIVING;
    private boolean mDirectionCalculate = true;
    private int mSpeed;
    private Random mColorRandom = new Random(Color.BLUE);
    private int mTotalDistance;
    private LocationClient mLocationClient;
    private TextView mViewById;

    private static String makeDistanceString(final int aDistance){
        return aDistance < 1000 ?
                String.valueOf(aDistance) + " m" :
                String.valueOf(aDistance / 1000) + "." + String.valueOf((aDistance % 1000) / 10) + " km";
    }

    private void cleanup(){
        mMap.clear();
        mPoints.clear();
        mTotalDistance = 0;
        mColorRandom = new Random(Color.BLUE);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(final Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        restorePreferences();

        getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_HOME);

        if(mMap != null){
            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener(){

                @Override
                public void onMapLongClick(final LatLng aLatLng){
                    onAddMarker(aLatLng);
                }
            });
        }
        if(mLocationClient == null){
            mLocationClient = new LocationClient(getApplicationContext(), this, this);
        }

        mViewById = (TextView) findViewById(R.id.tvTotal);
        mViewById.setText((appendToFullDistance(0)));
    }

    private String appendToFullDistance(final int aDistance){
        mTotalDistance += aDistance;
        return "Total: " + makeDistanceString(mPoints.isEmpty() ? 0 : mTotalDistance);
    }

    private void restorePreferences(){
        SharedPreferences prefs = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);
        mDirectionCalculate = prefs.getBoolean(PREF_DIRECTION_CALCULATE, true);
        mTravelMode = Routing.TravelMode.valueOf(
                prefs.getString(PREF_DIRECTION_TRAVELMODE, Routing.TravelMode.DRIVING.name()));
    }

    @Override
    protected void configureMap(final Bundle savedInstanceState){
        super.configureMap(savedInstanceState);

        mIconGenerator = new IconGenerator(this);
        mIconGenerator.setContentPadding(2, 2, 2, 2);
    }

    @Override
    protected void onStart(){
        super.onStart();
        mLocationClient.connect();

    }

    @Override
    protected void onStop(){
        super.onStop();
        mLocationClient.disconnect();
    }

    @Override
    protected void onAddMarker(final LatLng aLatLng){
        int color = Color.argb(255, mColorRandom.nextInt(256), mColorRandom.nextInt(256), mColorRandom.nextInt(256));
        LatLng oldLast = mPoints.peekLast();
        int distance = oldLast == null ? 0 : (int) MapsHelper.distance(oldLast, aLatLng) / 2;
        addPointToMap(aLatLng, color, distance);

        if(!mPoints.isEmpty()){
            LatLng last = mPoints.getLast();
            if(mDirectionCalculate){
                calculateRoute(color, last, aLatLng);
            } else{
                addRouteLine(color, last, aLatLng);
                addDistanceMarker(color, distance, MapsHelper.calcLngLat(oldLast, distance,
                                                                         MapsHelper.bearing(oldLast, aLatLng)));
            }
        }
        mPoints.add(aLatLng);
    }

    private void addRouteLine(final int aColor, final LatLng... aLatLng){
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.add(aLatLng);
        polylineOptions.width(8);
        polylineOptions.color(aColor);
        polylineOptions.geodesic(true);
        mMap.addPolyline(polylineOptions);
    }

    private void addDistanceMarker(final int aColor, final int aDistance, final LatLng aPosition){
        MarkerOptions distanceMarker = new MarkerOptions();

        distanceMarker.position(aPosition);
        distanceMarker.draggable(false);
        distanceMarker.visible(true);

        String text = makeDistanceString(aDistance);
        mIconGenerator.setBackground(new ColorDrawable(aColor));
        mIconGenerator.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Widget_IconMenu_Item);
        Bitmap icon = mIconGenerator.makeIcon(text);
        distanceMarker.icon(BitmapDescriptorFactory.fromBitmap(icon));
        mMap.addMarker(distanceMarker);

        mViewById.setText(appendToFullDistance(aDistance));

    }

    public void calculateRoute(final int aColor, final LatLng aFrom, final LatLng aTo){
        MapsHelper.calculateRoute(mTravelMode, aFrom, aTo, new RoutingListener(){

            @Override
            public void onRoutingFailure(){
                hideProgress();

                addRouteLine(aColor, aFrom, aTo);

                int distance = (int) MapsHelper.distance(aFrom, aTo) / 2;
                addDistanceMarker(aColor, distance,
                                  MapsHelper.calcLngLat(aFrom, distance, MapsHelper.bearing(aFrom, aTo)));

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
                    LatLngBounds.Builder include = LatLngBounds.builder().include(aFrom).include(aTo);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(include.build(), 25));

                    List<LatLng> routingPoints = aPolyOptions.getPoints();

                    List<LatLng> sortedPoints = new ArrayList<LatLng>();
                    HashSet<LatLng> pointSet = new HashSet<LatLng>();
                    for(LatLng latLng : routingPoints){
                        if(pointSet.add(latLng)){
                            sortedPoints.add(latLng);
                        }
                    }
                    mPoints.removeLast();
                    mPoints.removeLast();
                    mPoints.addAll(sortedPoints);

                    addRouteLine(aColor, sortedPoints.toArray(new LatLng[sortedPoints.size()]));

                    double distance = MapsHelper.distance(sortedPoints);
                    LatLng centerPoint = sortedPoints.get(sortedPoints.size() / 2);
                    addDistanceMarker(aColor, (int) distance, centerPoint);

                }
            }
        });
    }

    private void addPointToMap(final LatLng aLatLng, final int aColor, final int aDistance){
        MarkerOptions pointMarker = new MarkerOptions();
        pointMarker.draggable(false);
        if(mPoints.isEmpty()){
            pointMarker.icon(BitmapDescriptorFactory.defaultMarker());
        } else{
            float[] hsv = new float[3];
            Color.colorToHSV(aColor, hsv);
            pointMarker.icon(BitmapDescriptorFactory.defaultMarker(hsv[0]));
        }
        pointMarker.position(aLatLng);
        pointMarker.visible(true);
        mMap.addMarker(pointMarker);
    }

    @Override
    public void onPause(){
        hideProgress();

        savePreferences();

        super.onPause();
    }

    private void savePreferences(){
        SharedPreferences prefs = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.
                putBoolean(PREF_DIRECTION_CALCULATE, mDirectionCalculate).
                putString(PREF_DIRECTION_TRAVELMODE, mTravelMode.name()).
                apply();
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

    @SuppressWarnings("ConstantConditions")
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
                mViewById.setText((appendToFullDistance(0)));
                return true;
            case R.id.action_start_route:
                showSpeedDialog();
                return true;
            case R.id.action_stop_route:
                FakeLocationService.stop(this);
                return true;
            case R.id.action_add_new_point:
                promptPoint();
                return true;
            case R.id.direction_none:
                mDirectionCalculate = false;
                item.setChecked(!item.isChecked());
                invalidateOptionsMenu();
                return true;
            case R.id.direction_biking:
                changeRoutingMode(item, Routing.TravelMode.BIKING);
                return true;
            case R.id.direction_driving:
                changeRoutingMode(item, Routing.TravelMode.DRIVING);
                return true;
            case R.id.direction_walking:
                changeRoutingMode(item, Routing.TravelMode.WALKING);
                return true;
            case R.id.direction_transit:
                changeRoutingMode(item, Routing.TravelMode.TRANSIT);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void changeRoutingMode(final MenuItem item, final Routing.TravelMode aBiking){
        mDirectionCalculate = true;
        item.setChecked(!item.isChecked());
        mTravelMode = aBiking;
        invalidateOptionsMenu();
    }

    private void promptPoint(){
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setTitle("Input address of point");
        AutoCompleteAddressTextView tvAddress = new AutoCompleteAddressTextView(this);
        tvAddress.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                               ViewGroup.LayoutParams.WRAP_CONTENT));
        tvAddress.setPadding(4, 20, 2, 4);
        ab.setView(tvAddress);
        ab.setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener(){

            @Override
            public void onClick(final DialogInterface dialog, final int which){
                dialog.dismiss();
            }
        });
        final AlertDialog dialog = ab.show();
        tvAddress.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id){
                dialog.dismiss();
                Address point = (Address) parent.getItemAtPosition(position);
                onAddMarker(new LatLng(point.getLatitude(), point.getLongitude()));
            }
        });

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu){
        if(!mDirectionCalculate){
            menu.findItem(R.id.direction_none).setChecked(true);

        } else{
            // direction travel types
//        menu.findItem(R.id.action_direction).getSubMenu().setGroupEnabled(R.id.action_group_direction_settings,
//                                                                          mDirectionCalculate);
            switch(mTravelMode){
                case WALKING:
                    menu.findItem(R.id.direction_walking).setChecked(true);
                    break;
                case DRIVING:
                    menu.findItem(R.id.direction_driving).setChecked(true);
                    break;
                case BIKING:
                    menu.findItem(R.id.direction_biking).setChecked(true);
                    break;
                case TRANSIT:
                    menu.findItem(R.id.direction_transit).setChecked(true);
                    break;
            }
        }

//        if(mDirectionCalculate){
//            menu.findItem(R.id.action_direction).setTitle("Pathfinding: " + mTravelMode.name());
//        } else{
//            menu.findItem(R.id.action_direction).setTitle("Pathfinding: OFF");
//        }
        return true;
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
                FakeLocationService.start(MainActivity.this, mSpeed, -1, mPoints);
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

    @Override
    public void onLocationChanged(final Location location){
        if(location != null){
            moveMarker.visible(true);
            moveMarker.position(new LatLng(location.getLatitude(), location.getLongitude()));
        }
    }

    @Override
    public void onConnected(final Bundle aBundle){
        LocationRequest locationRequest = LocationRequest.create().setSmallestDisplacement(50);
        locationRequest.setInterval(1000);
        mLocationClient.requestLocationUpdates(locationRequest, this);
    }

    @Override
    public void onDisconnected(){
        mLocationClient.removeLocationUpdates(this);
    }

    @Override
    public void onConnectionFailed(final ConnectionResult aConnectionResult){
        //todo
        int i = 0;
    }
}

