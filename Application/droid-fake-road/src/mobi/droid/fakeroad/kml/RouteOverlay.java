package mobi.droid.fakeroad.kml;

public class RouteOverlay { //extends Overlay

/*    private static final int RADIUS = 6;
    public static final int LINE_ALPHA = 120;
    public static final int LINE_WIDTH = 5;

    private GeoPoint mPoint1;
    private GeoPoint mPoint2;

    private Mode mMode = Mode.START_POINT;

    private int mDefaultColor;

    public RouteOverlay(GeoPoint aPoint1, GeoPoint aPoint2, Mode aMode){ // GeoPoint is a int. (6E)
        this.mPoint1 = aPoint1;
        this.mPoint2 = aPoint2;
        this.mMode = aMode;
        mDefaultColor = Color.BLACK;
    }

    public RouteOverlay(GeoPoint aPoint1, GeoPoint aPoint2, Mode aMode, int aDefaultColor){
        this.mPoint1 = aPoint1;
        this.mPoint2 = aPoint2;
        this.mMode = aMode;
        this.mDefaultColor = aDefaultColor;
    }

    public Mode getMode(){
        return mMode;
    }

    @Override
    public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when){
        Projection projection = mapView.getProjection();
        if(!shadow){
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            Point point = new Point();
            projection.toPixels(mPoint1, point);
            switch(mMode){
                case START_POINT:{
                    paint.setColor(mDefaultColor);
                    RectF oval = new RectF(point.x - RADIUS, point.y - RADIUS,
                                           point.x + RADIUS, point.y + RADIUS);
                    // start point
                    canvas.drawOval(oval, paint);
                }
                break;
                case PATH:{
                    if(mDefaultColor == 999){
                        paint.setColor(Color.RED);
                    } else{
                        paint.setColor(mDefaultColor);
                    }
                    Point point2 = new Point();
                    projection.toPixels(mPoint2, point2);
                    paint.setStrokeWidth(LINE_WIDTH);
                    paint.setAlpha(LINE_ALPHA);
                    canvas.drawLine(point.x, point.y, point2.x, point2.y, paint);
                }
                break;
                case END_POINT:{
                    paint.setColor(mDefaultColor);

                    Point point2 = new Point();
                    projection.toPixels(mPoint2, point2);
                    paint.setStrokeWidth(LINE_WIDTH);
                    paint.setAlpha(LINE_ALPHA);
                    canvas.drawLine(point.x, point.y, point2.x, point2.y, paint);
                    RectF oval = new RectF(point2.x - RADIUS, point2.y - RADIUS,
                                           point2.x + RADIUS, point2.y + RADIUS);
                    // end point
                    paint.setAlpha(255);
                    canvas.drawOval(oval, paint);
                }
                break;
            }
        }
        return super.draw(canvas, mapView, shadow, when);
    }

    public static enum Mode{
        START_POINT,
        PATH,
        END_POINT
    }*/
}