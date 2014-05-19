package mobi.droid.fakeroad.service;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
import mobi.droid.fakeroad.Actions;
import mobi.droid.fakeroad.R;
import mobi.droid.fakeroad.location.MapsHelper;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

public class FakeLocationService extends Service{

    public static final String EXTRA_ROUTE_ID = "points";
    public static final String EXTRA_SPEED = "speed";
    public static final String EXTRA_MIN_SPEED = "min.speed";
    public static final String EXTRA_TIME = "time";
    public static final String EXTRA_RANDOM_SPEED = "random.speed";
    //
    public static int LOCATION_UPDATE_INTERVAL = 1000;
    //
    private Handler mHandler = new Handler();
    private boolean mMoving;
    private LocationGenerator mGenerator;
    private int mSpeed = 0;
    private int mRouteID = -1;
    private boolean mRandomSpeed;
    private int mMinSpeed = 0;

    public static void start(Context aContext, int aSpeed, final int aMinSpeed, long aTime, int aRoute,
                             final boolean aRandomSpeed,
                             final int aUpdateLocationInterval){
        Intent intent = new Intent(Actions.ACTION_START_MOVING);
        intent.setClass(aContext, FakeLocationService.class);
        intent.putExtra(EXTRA_ROUTE_ID, aRoute);
        intent.putExtra(EXTRA_SPEED, aSpeed);
        intent.putExtra(EXTRA_TIME, aTime);
        intent.putExtra(EXTRA_MIN_SPEED, aMinSpeed);
        intent.putExtra(EXTRA_RANDOM_SPEED, aRandomSpeed);
//        LOCATION_UPDATE_INTERVAL = aUpdateLocationInterval <= 0 ? 1000 : aUpdateLocationInterval * 1000;//it's wrong
        aContext.startService(intent);
    }

    public static void stop(Context aContext){
        Intent intent = new Intent(Actions.ACTION_STOP_MOVING);
        intent.setClass(aContext, FakeLocationService.class);
        aContext.startService(intent);
    }

    public static boolean isFakeRunning(Context aContext){
        ActivityManager manager = (ActivityManager) aContext.getSystemService(Context.ACTIVITY_SERVICE);
        //noinspection ConstantConditions
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if(FakeLocationService.class.getName().equals(service.service.getClassName())){
                return true;
            }
        }
        return false;

    }

    @Override
    public IBinder onBind(final Intent intent){
        return null;
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

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = createNotification(null);
        nm.notify(1, notification);
//        stopForeground(true);
//        stopSelf();
    }

    private void startMoving(final Intent aIntent){
        mMoving = true;

        mRouteID = aIntent.getIntExtra(EXTRA_ROUTE_ID, mRouteID);
        mRandomSpeed = aIntent.getBooleanExtra(EXTRA_RANDOM_SPEED, mRandomSpeed);

        mSpeed = aIntent.getIntExtra(EXTRA_SPEED, mSpeed);
        mMinSpeed = aIntent.getIntExtra(EXTRA_MIN_SPEED, mSpeed);
        long time = aIntent.getLongExtra(EXTRA_TIME, 0);

        if(mSpeed < 1 && time < 1){
            mMoving = false;
            stopSelf();
            return;
        }
        if(mSpeed < 1){
//            speed = (int) (time / MapsHelper.distance(sourcePoints)); //todo
        }
        if(mRouteID == -1){
            return;
        }

        startForeground(1, createNotification(null));

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, false, true, true, 1,
                                        0);
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        mGenerator = new LocationGenerator(mRouteID, mSpeed, mRandomSpeed);
        mHandler.post(mGenerator);
    }

    private Notification createNotification(String aText){
        Notification.Builder builder = new Notification.Builder(this);
        builder.setAutoCancel(true);
        builder.setOngoing(true);
        if(TextUtils.isEmpty(aText)){
            builder.setContentTitle("Fake road: stopped");
        } else{
            builder.setContentTitle(aText);
        }

        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setPriority(Notification.PRIORITY_HIGH);

        if(mMoving){
            PendingIntent pi = PendingIntent.getService(this, 0, new Intent(Actions.ACTION_STOP_MOVING),
                                                        PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(android.R.drawable.ic_media_pause, "Stop", pi);
        } else{
            PendingIntent pi = PendingIntent.getService(this, 1, new Intent(Actions.ACTION_START_MOVING),
                                                        PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(android.R.drawable.ic_media_play, "Start", pi);
        }

        builder.setWhen(System.currentTimeMillis());

        //noinspection deprecation
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            return builder.build();
        }
        //noinspection deprecation
        return builder.getNotification();
    }



    //
    private class LocationGenerator implements Runnable{

        private List<LatLng> mSourcePoints;
        private int mRouteID;
        private int mSpeedLocation;
        private boolean mRandomSpeed;
        private Pair<LatLng, LatLng> mLastPointPair;
        Random random = new Random();
        private Method mLocationJellyBeanFixMethod;

        private LocationGenerator(final int aRouteID, final int aSpeed, final boolean aRandomSpeed){
            mRouteID = aRouteID;
            mSpeedLocation = aSpeed;
            mRandomSpeed = aRandomSpeed;
            LocationDbHelper ldh = new LocationDbHelper(FakeLocationService.this);
            mSourcePoints = ldh.queryPoints(aRouteID);

            mLastPointPair = Pair.create(mSourcePoints.get(0), mSourcePoints.get(0));

            try{
                mLocationJellyBeanFixMethod = Location.class.getMethod("makeComplete");
            } catch(NoSuchMethodException ignored){
            }
        }

        @Override
        public void run(){
            if(!mMoving){
                return;
            }
            int currentSpeed;

            if(mRandomSpeed){
                currentSpeed = (mMinSpeed + (random.nextInt(mSpeedLocation - mMinSpeed) + 1));
            } else{
                currentSpeed = mSpeedLocation;
            }

            mLastPointPair = MapsHelper.nextLatLng(mLastPointPair, mSourcePoints, currentSpeed);
            LatLng currentPoint = mLastPointPair.second;

            Pair<LatLng, LatLng> nextPair = MapsHelper.nextLatLng(mLastPointPair, mSourcePoints, currentSpeed);
            LatLng nextPoint = nextPair.second;

            Log.v("mobi.droid.fakeroad.gen", "curr=" + currentPoint + " next=" + nextPoint + " speed: " + currentSpeed);


            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(currentPoint.latitude);
            location.setLongitude(currentPoint.longitude);
            location.setAccuracy(0.0f);

            location.setSpeed(currentSpeed);
            if(!currentPoint.equals(nextPoint)){
                location.setBearing(MapsHelper.bearing(currentPoint, nextPoint));
            }

            location.setTime(System.currentTimeMillis());
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
                location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            }

            try{ // trick to initialize all last fields with default values
                if(mLocationJellyBeanFixMethod != null){
                    mLocationJellyBeanFixMethod.invoke(location);
                }
            } catch(Exception ignored){
            }

            String speedInfo = String.format("Speed: %d m/s (%d km/h %d mph)", currentSpeed,
                                             (int) (currentSpeed * 3.6),
                                             (int) (currentSpeed * 2.23));

            Notification notification = createNotification(speedInfo);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(1, notification);

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
