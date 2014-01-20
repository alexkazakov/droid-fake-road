package mobi.droid.fakeroad.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
import mobi.droid.fakeroad.Actions;
import mobi.droid.fakeroad.location.MapsHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class FakeLocationService extends Service{

    public static final String EXTRA_POINTS = "points";
    public static final String EXTRA_SPEED = "speed";
    public static final String EXTRA_TIME = "time";
    //
    public static final int LOCATION_UPDATE_INTERVAL = 1000;
    //
    private Handler mHandler = new Handler();
    private boolean mMoving;
    private LocationGenerator mGenerator;

    @Override
    public IBinder onBind(final Intent intent){
        return null;
    }

    public static void start(Context aContext, int aSpeed, long aTime, List<LatLng> aRoute){
        Intent intent = new Intent(Actions.ACTION_START_MOVING);
        intent.setClass(aContext, FakeLocationService.class);
        intent.putExtra(EXTRA_POINTS, new ArrayList<LatLng>(aRoute));
        intent.putExtra(EXTRA_SPEED, aSpeed);
        intent.putExtra(EXTRA_TIME, aTime);
        aContext.startService(intent);
    }

    public static void stop(Context aContext){
        Intent intent = new Intent(Actions.ACTION_STOP_MOVING);
        intent.setClass(aContext, FakeLocationService.class);
        aContext.startService(intent);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId){
        if(Actions.ACTION_START_MOVING.equals(intent.getAction())){
            if(!mMoving){
                startMoving(intent);
            }
        } else if(Actions.ACTION_STOP_MOVING.equals(intent.getAction())){
            if(mMoving){
                stopMoving();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void stopMoving(){
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
        locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);

        mHandler.removeCallbacks(mGenerator);
        mMoving = false;
        stopForeground(true);
        stopSelf();
    }

    private void startMoving(final Intent aIntent){
        mMoving = true;

        ArrayList<LatLng> sourcePoints = aIntent.getParcelableArrayListExtra(EXTRA_POINTS);
        int speed = aIntent.getIntExtra(EXTRA_SPEED, 0);
        long time = aIntent.getLongExtra(EXTRA_TIME, 0);

        if(speed < 1 && time < 1){
            mMoving = false;
            stopSelf();
            return;
        }
        if(speed < 1){
            speed = (int) (time / MapsHelper.distance(sourcePoints));
        }

        @SuppressWarnings("deprecation")
        Notification build = buildNotification();
        startForeground(1, build);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, false, true, true, 1,
                                        0);
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        mGenerator = new LocationGenerator(sourcePoints, speed);
        mHandler.post(mGenerator);
    }

    private Notification buildNotification(){
        Notification.Builder b = new Notification.Builder(this);
        b.setAutoCancel(true);
        b.setOngoing(true);
        b.setContentTitle("Fake movement is running");
        //noinspection ConstantConditions
        b.setLargeIcon(BitmapFactory.decodeResource(getResources(), getApplicationInfo().icon));
        PendingIntent pi = PendingIntent.getService(this, 0, new Intent(Actions.ACTION_STOP_MOVING),
                                                    PendingIntent.FLAG_UPDATE_CURRENT);
        b.setDeleteIntent(pi);
        b.setContentText("Click to stop fake movement");
        b.setWhen(System.currentTimeMillis());

        //noinspection deprecation
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            return b.build();
        }
        //noinspection deprecation
        return b.getNotification();
    }

    //
    private class LocationGenerator implements Runnable{

        private List<LatLng> mSourcePoints;
        private int mSpeed;
        private Pair<LatLng, LatLng> mLastPointPair;

        private LocationGenerator(final ArrayList<LatLng> aList, final int aSpeed){
            mSourcePoints = aList;
            mSpeed = aSpeed;
            mLastPointPair = Pair.create(mSourcePoints.get(0), mSourcePoints.get(0));
        }

        @Override
        public void run(){
            if(!mMoving){
                return;
            }

            mLastPointPair = MapsHelper.nextLatLng(mLastPointPair, mSourcePoints, mSpeed);
            LatLng currentPoint = mLastPointPair.second;

            Pair<LatLng, LatLng> nextPair = MapsHelper.nextLatLng(mLastPointPair, mSourcePoints, mSpeed);
            LatLng nextPoint = nextPair.second;

            Log.v("mobi.droid.fakeroad.gen", "curr=" + currentPoint);
            Log.v("mobi.droid.fakeroad.gen", "next=" + nextPoint);

            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(currentPoint.latitude);
            location.setLongitude(currentPoint.longitude);
            location.setAccuracy(0.0f);

            location.setSpeed(mSpeed);
            if(!currentPoint.equals(nextPoint)){
                location.setBearing(MapsHelper.bearing(currentPoint, nextPoint));
            }

            location.setTime(System.currentTimeMillis());
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
                location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            }

            try{ // trick to initialize all last fields with default values
                Method locationJellyBeanFixMethod = Location.class.getMethod("makeComplete");
                if(locationJellyBeanFixMethod != null){
                    locationJellyBeanFixMethod.invoke(location);
                }
            } catch(Exception e){
                e.printStackTrace();
            }

            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            try{
                lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, location);
            } catch(Exception e){
                Toast.makeText(FakeLocationService.this, "Stopped movement: " + e.getMessage(),
                               Toast.LENGTH_LONG).show();
                e.printStackTrace();
                stopMoving();
                return;
            }

            if(currentPoint.equals(nextPoint)){
                stopMoving();
            } else{
                mHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
            }

        }
    }
}
