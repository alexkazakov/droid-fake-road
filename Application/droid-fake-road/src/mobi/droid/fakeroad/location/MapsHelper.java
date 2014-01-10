package mobi.droid.fakeroad.location;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MapsHelper{

    /**
     * Calculate the bearing between two points.
     *
     * @param p1
     * @param p2
     * @return
     */
    public static float bearing(LatLng p1, LatLng p2){
        float[] result = new float[2];
        Location.distanceBetween(p1.latitude / 1000000.0, p1.longitude / 1000000.0,
                                 p2.latitude / 1000000.0, p2.longitude / 1000000.0, result);
        return result[1];
    }

    /**
     * Calculate the distance between two points.
     *
     * @param p1
     * @param p2
     * @return
     */
    public static double distance(LatLng p1, LatLng p2){
        float[] result = new float[1];
        Location.distanceBetween(p1.latitude / 1000000.0, p1.longitude / 1000000.0,
                                 p2.latitude / 1000000.0, p2.longitude / 1000000.0, result);
        return result[0];
    }

    /**
     * Calculate the distance between all points in a line. ie: calculate the
     * distance between each successive point and sum them.
     *
     * @param locations
     * @return
     */
    public static double distance(List<LatLng> locations){
        double result = 0;
        for(int i = 0; i < locations.size() - 1; i++){
            result += distance(locations.get(i), locations.get(i + 1));
        }
        return result;
    }

    /**
     * Create a human readable speed and time output using distance and times.
     *
     * @param distanceMeters The length of the path in meters.
     * @param progress The speed at which to move along the path. This value should
     * be linear - this function will alter the value to be
     * non-linear and allow for more fine tuned speed at slower
     * levels, but still allow for very fast speeds.
     * @param elapsetimeTV The textview to show the elapsed time result in
     * @param finishtimeTV
     * @param speedTV
     */
    public static void calcTimes(double distanceMeters, int progress, TextView elapsetimeTV, TextView finishtimeTV,
                                 TextView speedTV){
        if(progress > 100){
            // Linear speed from values 0-100. After 100, use quadratic function
            // to allow for very large speeds.
            progress -= 90;
            progress *= progress;
        }
        speedTV.setText(String.format("%,d km/h", progress));
        double MperSec = progress * 1000.0 / 3600.0;
        double seconds = distanceMeters / MperSec;

        elapsetimeTV.setText(makeTimeString(seconds));
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, (int) seconds);
        finishtimeTV.setText(cal.getTime().toString());
    }

    /**
     * Create a human readable string from seconds.
     */
    private static String makeTimeString(double seconds_d){
        int seconds = (int) seconds_d % 60;
        int minutes = (int) ((seconds_d / (60)) % 60);
        int hours = (int) ((seconds_d / (60 * 60)) % 24);
        int days = (int) ((seconds_d / (60 * 60 * 24)) % 365);
        int years = (int) (seconds_d / (60 * 60 * 24 * 365));

        ArrayList<String> timeArray = new ArrayList<String>();

        if(years > 0){
            timeArray.add(String.valueOf(years) + "y");
        }

        if(days > 0){
            timeArray.add(String.valueOf(days) + "d");
        }

        if(hours > 0){
            timeArray.add(String.valueOf(hours) + "h");
        }

        if(minutes > 0){
            timeArray.add(String.valueOf(minutes) + "min");
        }

        if(seconds > 0){
            timeArray.add(String.valueOf(seconds) + "sec");
        }

        String time = "";
        for(int i = 0; i < timeArray.size(); i++){
            time = time + timeArray.get(i);
            if(i != timeArray.size() - 1){
                time = time + ", ";
            }
        }

        if(time == ""){
            time = "0 sec";
        }

        return time;
    }

    /**
     * Use Google's geocoder to get a location from a name the user has entered.
     * Handles errors internally with toasts and is hard-linked to the activity
     * for a less-generic but less-code solution.
     *
     * @param activity
     * @param searchstring User entered string to search for
     */
    public static void getLocationFromString(final Activity activity, MapView aMapView, final String searchstring){
        Toast.makeText(activity, "Searching...", Toast.LENGTH_SHORT).show();
        new Thread(){

            public void run(){
                Geocoder geocoder = new Geocoder(activity);
                try{
                    final List<Address> addresslist = geocoder.getFromLocationName(searchstring, 1);
                    if(addresslist.isEmpty()){
                        Toast.makeText(activity, "Search does not found anything", Toast.LENGTH_SHORT).show();
                    } else{
                        activity.runOnUiThread(new Runnable(){

                            @Override
                            public void run(){
                                Address address = addresslist.get(0);
                                LatLng p = new LatLng((int) (address.getLatitude() * 1E6),
                                                      (int) (address.getLongitude() * 1E6));
                                //aMapView.getController().animateTo(p); TODO AK
                                //aMapView.getController().setZoom(16);
                            }
                        });
                    }
                } catch(final Exception e){
                    e.printStackTrace();
                    activity.runOnUiThread(new Runnable(){

                        @Override
                        public void run(){
                            Toast.makeText(activity, "Search error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            ;
        }.start();
    }

    /**
     * Uses Google Maps' Javascript API to get a list of directions between two
     * map points.
     *
     * @param start
     * @param end
     * @return An array of points representing a route across roads between the
     * two points.
     * @throws Exception If any error has occurred and no directions were possible.
     */
    public static ArrayList<LatLng> getJavascriptDirections(LatLng start, LatLng end) throws Exception{
        // If params LatLng convert to lat,long string here
        StringBuffer urlString = new StringBuffer();
        urlString.append("http://maps.google.com/maps?f=d&hl=en");
        urlString.append("&saddr=");// from
        urlString.append(start.latitude / 1E6 + "," + start.longitude / 1E6);
        urlString.append("&daddr=");// to
        urlString.append(end.latitude / 1E6 + "," + end.longitude / 1E6);
        urlString.append("&ie=UTF8&0&om=0&output=dragdir"); // DRAGDIR RETURNS
        // JSON
        Log.i("URLString", urlString.toString());

        URL inUrl = new URL(urlString.toString());
        URLConnection yc = inUrl.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
        String inputLine;
        String encoded = "";
        while((inputLine = in.readLine()) != null){
            encoded = encoded.concat(inputLine);
        }
        in.close();
        String polyline = encoded.split("points:")[1].split(",")[0];
        polyline = polyline.replace("\"", "");
        polyline = polyline.replace("\\\\", "\\");
        return decodePolyline(polyline);
    }

    /**
     * Change a list of points in an encoded string into a usable list.
     *
     * @param encoded The encoded string received from google
     * @return a usable list
     */
    public static ArrayList<LatLng> decodePolyline(String encoded){
        ArrayList<LatLng> LatLngs = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while(index < len){
            int b, shift = 0, result = 0;
            do{
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while(b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do{
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while(b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((int) (((double) lat / 1E5) * 1E6), (int) (((double) lng / 1E5) * 1E6));
            LatLngs.add(p);
        }
        return LatLngs;
    }
}
