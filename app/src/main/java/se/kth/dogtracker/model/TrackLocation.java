package se.kth.dogtracker.model;

import com.mapbox.geojson.Point;

/**
 * This class represents a single mapbox point on the map together with a
 * timestamp in milliseconds which defines when the mapbox point was saved.
 */
public class TrackLocation {

    private final Point point;
    private final long time;

    public TrackLocation(Point point, long time) {
        this.point = point;
        this.time = time;
    }

    public Point getPoint() { return point; }
    public long getTime() { return time; }
}
