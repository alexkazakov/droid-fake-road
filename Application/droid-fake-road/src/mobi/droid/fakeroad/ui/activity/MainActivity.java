package mobi.droid.fakeroad.ui.activity;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.Toast;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;
import mobi.droid.fakeroad.R;
import mobi.droid.fakeroad.location.MapsHelper;

public class MainActivity extends BaseMapViewActivity{

    private IconGenerator mIconGenerator;
    private MarkerOptions mRouteStartMarkerOptions;
    private MarkerOptions mRouteEndMarkerOptions;
    private ProgressDialog mProgressDialog;

    @Override
    protected void configureMap(final Bundle savedInstanceState){
        super.configureMap(savedInstanceState);

        mIconGenerator = new IconGenerator(this);
        mIconGenerator.setBackground(new ColorDrawable(Color.LTGRAY));
        mIconGenerator.setContentPadding(2, 2, 2, 2);

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
    protected void onAddMarker(final LatLng aLatLng){
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

            String text = distance < 1000 ?
                    String.valueOf(distance) + " m" :
                    String.valueOf(distance / 1000) + "." + String.valueOf(distance % 1000) + " km";
            Bitmap icon = mIconGenerator.makeIcon(text);
            distanceMarker.icon(BitmapDescriptorFactory.fromBitmap(icon));
            mMap.addMarker(distanceMarker);
        }
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
                Toast.makeText(MainActivity.this, "Failed to determine routing", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRoutingStart(){
                mProgressDialog = ProgressDialog.show(MainActivity.this, null, "Routing calculation...", true);
            }

            @Override
            public void onRoutingSuccess(final PolylineOptions mPolyOptions){
                hideProgress();
                if(mMap != null){
                    mMap.clear();

                    // Start marker
                    MarkerOptions options = new MarkerOptions();
                    options.position(aFrom);
                    options.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue));
                    mMap.addMarker(options);

                    // End marker
                    options.position(aTo);
                    options.icon(BitmapDescriptorFactory.fromResource(R.drawable.end_green));
                    mMap.addMarker(options);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(aTo));

                    PolylineOptions polylineOptions = new PolylineOptions();
                    polylineOptions.color(Color.BLUE);
                    polylineOptions.width(10);
                    polylineOptions.addAll(mPolyOptions.getPoints());

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

    private void hideProgress(){
        try{
            mProgressDialog.dismiss();
        } catch(Exception ignored){
        }
    }
}

