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
import com.google.android.gms.maps.model.LatLng;
import mobi.droid.fakeroad.Actions;
import mobi.droid.fakeroad.location.MapsHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
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

        if(speed <= 0 && time <= 0){
            mMoving = false;
            stopSelf();
            return;
        }

        LinkedList<LatLng> pointsList;
        if(speed <= -1){
            pointsList = MapsHelper.getPathPointsForTime(time, sourcePoints);
        } else{
            pointsList = MapsHelper.getPathPointsForSpeed(speed, sourcePoints);
        }

        @SuppressWarnings("deprecation")
        Notification build = buildNotification();
        startForeground(1, build);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, false, true, true, 1,
                                        0);
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        mGenerator = new LocationGenerator(pointsList, speed);
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

        private LinkedList<LatLng> mList;
        private int mSpeed;

        private LocationGenerator(final LinkedList<LatLng> aList, final int aSpeed){
            mList = aList;
            mSpeed = aSpeed;
        }

        @Override
        public void run(){
            if(!mMoving){
                return;
            }
            LatLng latLng = mList.poll();
            if(latLng == null){
                stopMoving();
                return;
            } else{
                LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);

                Location location = new Location(LocationManager.GPS_PROVIDER);
                location.setLatitude(latLng.latitude);
                location.setLongitude(latLng.longitude);
                location.setAccuracy(0.0f);

                location.setSpeed(mSpeed);
                LatLng nextLocation = mList.peek();
                if(nextLocation != null){
                    location.setBearing(MapsHelper.bearing(latLng, nextLocation));
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

                try{
                    lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, location);
                } catch(Exception e){
                    e.printStackTrace(); // TODO AK handle errors
                    stopMoving();
                    return;
                }
            }
            mHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
        }
    }
}
