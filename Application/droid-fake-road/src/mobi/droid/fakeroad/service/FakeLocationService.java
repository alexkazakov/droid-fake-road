package mobi.droid.fakeroad.service;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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

        mMoving = false;
        mGenerator.stop();
        mHandler.removeCallbacks(mGenerator);

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

        mGenerator = new LocationGenerator(mRouteID, mRandomSpeed);
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
        private boolean mRandomSpeed;
        private Pair<LatLng, LatLng> mCurrentPoints;
        Random random = new Random();
        private Method mLocationJellyBeanFixMethod;
        private final LocationDbHelper mDbHelper;
        private final Cursor mRouteCursor;
        private LatLng mFinalPoint;

        private LocationGenerator(final int aRouteID, final boolean aRandomSpeed){
            mRandomSpeed = aRandomSpeed;
            mDbHelper = new LocationDbHelper(FakeLocationService.this);
            mRouteCursor = mDbHelper.routeCursor(aRouteID);

            if(mRouteCursor == null || !mRouteCursor.moveToLast()){
                Toast.makeText(FakeLocationService.this, "No route data is available: " + aRouteID,
                               Toast.LENGTH_LONG).show();
                stopMoving();
                return;
            }
            mFinalPoint = mDbHelper.readLatLng(mRouteCursor);

            mRouteCursor.moveToFirst();
            LatLng startLatLng = mDbHelper.readLatLng(mRouteCursor);
            mCurrentPoints = Pair.create(startLatLng, startLatLng);

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
            int speed;
            if(mRandomSpeed){
                speed = (mMinSpeed + (random.nextInt(mSpeed - mMinSpeed) + 1));
            } else{
                speed = mSpeed;
            }

            mCurrentPoints = MapsHelper.nextLatLng(mCurrentPoints,
                                                   mRouteCursor,
                                                   mFinalPoint,
                                                   mDbHelper,
                                                   speed);
            LatLng currentPoint = mCurrentPoints.second;
            // save current point position
            int position = mRouteCursor.getPosition();

            Pair<LatLng, LatLng> nextPair = MapsHelper.nextLatLng(mCurrentPoints,
                                                                  mRouteCursor,
                                                                  mFinalPoint,
                                                                  mDbHelper,
                                                                  speed);
            LatLng nextPoint = nextPair.second;
            Log.v("mobi.droid.fakeroad.gen", "curr=" + currentPoint + " next=" + nextPoint + " speed: " + speed);
            // restore current point position
            mRouteCursor.moveToPosition(position);

            Location location = fillinLocation(currentPoint, speed, nextPoint);

            String speedInfo = String.format("Speed: %d m/s (%d km/h %d mph)", speed,
                                             (int) (speed * 3.6),
                                             (int) (speed * 2.23));

            Notification notification = createNotification(speedInfo);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(1, notification);

            if(!publishLocation(location) || currentPoint.equals(nextPoint)){
                stopMoving();
            } else{
                mHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
            }

        }

        private boolean publishLocation(final Location aLocation){
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            try{
                lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, aLocation);
            } catch(Exception e){
                e.printStackTrace();
                Toast.makeText(FakeLocationService.this, "Stopped movement: " + e.getMessage(),
                               Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        }

        private Location fillinLocation(final LatLng aCurrentPoint, final int aSpeed, final LatLng aNextPoint){
            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(aCurrentPoint.latitude);
            location.setLongitude(aCurrentPoint.longitude);
            location.setAccuracy(0.0f);

            location.setSpeed(aSpeed);
            if(!aCurrentPoint.equals(aNextPoint)){
                location.setBearing(MapsHelper.bearing(aCurrentPoint, aNextPoint));
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
            return location;
        }

        public void stop(){
            if(mRouteCursor != null){
                mRouteCursor.close();
            }
            try{
                mDbHelper.close();
            } catch(Exception ignored){
            }
        }
    }
}
