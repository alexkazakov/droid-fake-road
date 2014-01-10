package mobi.droid.fakeroad.kml;

import android.util.Log;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.net.URL;
import java.net.URLConnection;

public class MapService{

    public static final int MODE_ANY = 0;
    public static final int MODE_CAR = 1;
    public static final int MODE_WALKING = 2;
    private static final String TAG = "mobi.MapService";

    public static NavigationDataSet calculateRoute(Double startLat, Double startLng, Double targetLat, Double targetLng,
                                                   int mode){
        return calculateRoute(startLat + "," + startLng, targetLat + "," + targetLng, mode);
    }

    public static NavigationDataSet calculateRoute(String startCoords, String targetCoords, int mode){
        String urlPedestrianMode = "http://maps.google.com/maps?" + "saddr=" + startCoords + "&daddr="
                + targetCoords + "&sll=" + startCoords + "&dirflg=w&hl=en&ie=UTF8&z=14&output=kml";

        Log.d(TAG, "urlPedestrianMode: " + urlPedestrianMode);

        String urlCarMode = "http://maps.google.com/maps?" + "saddr=" + startCoords + "&daddr="
                + targetCoords + "&sll=" + startCoords + "&hl=en&ie=UTF8&z=14&output=kml";

        Log.d(TAG, "urlCarMode: " + urlCarMode);

        NavigationDataSet navSet = null;
        // for mode_any: try pedestrian route calculation first, if it fails, fall back to car route
        if(mode == MODE_ANY || mode == MODE_WALKING){
            navSet = MapService.getNavigationDataSet(urlPedestrianMode);
        }
        if(mode == MODE_ANY && navSet == null || mode == MODE_CAR){
            navSet = MapService.getNavigationDataSet(urlCarMode);
        }
        return navSet;
    }

    /**
     * Retrieve navigation data set from either remote URL or String
     *
     * @param url
     * @return navigation set
     */
    public static NavigationDataSet getNavigationDataSet(String url){
        Log.d(TAG, "urlString -->> " + url);
        NavigationDataSet navigationDataSet = null;
        try{
            final URL aUrl = new URL(url);
            final URLConnection conn = aUrl.openConnection();
            conn.setReadTimeout(15 * 1000);  // timeout for reading the google maps data: 15 secs
            conn.connect();

            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();

            XMLReader xr = sp.getXMLReader();

            NavigationSaxHandler navSax2Handler = new NavigationSaxHandler();
            xr.setContentHandler(navSax2Handler);

            xr.parse(new InputSource(aUrl.openStream()));

            navigationDataSet = navSax2Handler.getParsedData();

            Log.d(TAG, "navigationDataSet: " + navigationDataSet.toString());

        } catch(Exception e){
            navigationDataSet = null;
        }

        return navigationDataSet;
    }

 /*   *//**
     * Does the actual drawing of the route, based on the geo points provided in the nav set
     *
     * @param navSet Navigation set bean that holds the route information, incl. geo pos
     * @param color Color in which to draw the lines
     * @param mMapView01 Map view to draw onto
     *//*
    public static void drawPath(NavigationDataSet navSet, int color, MapFragment mMapView01){

        Log.d(TAG, "map color before: " + color);

        // color correction for dining, make it darker
        if(color == Color.parseColor("#add331")){
            color = Color.parseColor("#6C8715");
        }
        Log.d(TAG, "map color after: " + color);

        Collection overlaysToAddAgain = new ArrayList();
        for(Iterator iter = mMapView01.getMap().iterator(); iter.hasNext(); ){
            Object o = iter.next();
            Log.d(TAG, "overlay type: " + o.getClass().getName());
            if(!RouteOverlay.class.getName().equals(o.getClass().getName())){
                // mMapView01.getOverlays().remove(o);
                overlaysToAddAgain.add(o);
            }
        }
        mMapView01.getOverlays().clear();
        mMapView01.getOverlays().addAll(overlaysToAddAgain);

        String path = navSet.getRoutePlacemark().getCoordinates();
        Log.d(TAG, "path=" + path);
        if(path != null && path.trim().length() > 0){
            String[] pairs = path.trim().split(" ");

            Log.d(TAG, "pairs.length=" + pairs.length);

            String[] lngLat = pairs[0].split(","); // lngLat[0]=longitude lngLat[1]=latitude lngLat[2]=height

            Log.d(TAG, "lnglat =" + lngLat + ", length: " + lngLat.length);

            if(lngLat.length < 3){
                lngLat = pairs[1].split(","); // if first pair is not transferred completely, take seconds pair //TODO
            }

            try{
                GeoPoint startGP = new GeoPoint((int) (Double.parseDouble(lngLat[1]) * 1E6),
                                                (int) (Double.parseDouble(lngLat[0]) * 1E6));
                mMapView01.getOverlays().add(new RouteOverlay(startGP, startGP, RouteOverlay.Mode.PATH));
                GeoPoint gp1;
                GeoPoint gp2 = startGP;

                for(int i = 1; i < pairs.length; i++) // the last one would be crash
                {
                    lngLat = pairs[i].split(",");

                    gp1 = gp2;

                    if(lngLat.length >= 2 && gp1.getLatitudeE6() > 0 && gp1.getLongitudeE6() > 0
                            && gp2.getLatitudeE6() > 0 && gp2.getLongitudeE6() > 0){

                        // for GeoPoint, first:latitude, second:longitude
                        gp2 = new GeoPoint((int) (Double.parseDouble(lngLat[1]) * 1E6),
                                           (int) (Double.parseDouble(lngLat[0]) * 1E6));

                        if(gp2.getLatitudeE6() != 22200000){
                            mMapView01.getOverlays().add(new RouteOverlay(gp1, gp2, RouteOverlay.Mode.PATH, color));
                            Log.d(TAG,
                                  "draw:" + gp1.getLatitudeE6() + "/" + gp1.getLongitudeE6() + " TO " + gp2.getLatitudeE6() + "/" + gp2.getLongitudeE6());
                        }
                    }
                    // Log.d(TAG,"pair:" + pairs[i]);
                }
                //routeOverlays.add(new RouteOverlay(gp2,gp2, 3));
                mMapView01.getOverlays().add(new RouteOverlay(gp2, gp2, RouteOverlay.Mode.END_POINT));
            } catch(NumberFormatException e){
                Log.e(TAG, "Cannot draw route.", e);
            }
        }
        // mMapView01.getOverlays().addAll(routeOverlays); // use the default color
        mMapView01.setEnabled(true);
    }
*/
}