package mobi.droid.fakeroad.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import com.google.android.gms.maps.model.LatLng;
import mobi.droid.fakeroad.Actions;
import mobi.droid.fakeroad.location.MapsHelper;

import java.util.LinkedList;

public class FakeLocationService extends Service{

    public static final String EXTRA_POINTS = "points";
    public static final String EXTRA_SPEED = "speed";
    public static final String EXTRA_TIME = "time";
    public static final int LOCATION_UPDATE_INTERVAL = 1000;
    private Handler mHandler = new Handler();
    private boolean mMoving;
    private LocationGenerator mGenerator;

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
        mHandler.removeCallbacks(mGenerator);
        mMoving = false;
        stopForeground(true);
        stopSelf();
    }

    private void startMoving(final Intent aIntent){
        mMoving = true;

        LatLng[] sourcePoints = (LatLng[]) aIntent.getParcelableArrayExtra(EXTRA_POINTS);
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

        mGenerator = new LocationGenerator(pointsList);
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

    private class LocationGenerator implements Runnable{

        private LinkedList<LatLng> mList;
        private LatLng mPreviousLocation;

        private LocationGenerator(final LinkedList<LatLng> aList){
            mList = aList;
        }

        @Override
        public void run(){
            if(!mMoving){
                return;
            }
            LatLng location = mList.poll();
            if(location == null){
                stopMoving();
                return;
            } else{
                LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                Location loc = new Location(LocationManager.GPS_PROVIDER);
                loc.setLatitude(location.latitude);
                loc.setLongitude(location.latitude);
                if(mPreviousLocation != null){
                    loc.setBearing(MapsHelper.bearing(mPreviousLocation, location));
                }
                try{
                    lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc);
                } catch(Exception e){
                    e.printStackTrace(); // TODO AK handle errors
                    stopMoving();
                    return;
                }
            }
            mPreviousLocation = location;
            mHandler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
        }
    }
}
