package se.kth.dogtracker.model;

import static com.mapbox.turf.TurfConstants.UNIT_METERS;

import android.util.Log;

import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfMeasurement;

import org.json.JSONException;
import java.util.ArrayList;

import se.kth.dogtracker.io.DatabaseIO;

/**
 * Class representing the model for dog tracking.
 * Holds location points and stats for the track that the user and their dog are currently working on.
 */
public class DogTracker {

    private Track track;
    private boolean paused;

    /**
     * State representing at which point in the tracking life cycle the current track is.
     */
    public enum State { START_HUMAN, HUMAN, START_DOG, DOG }
    private State state;

    private long lastPositionChangedTime;

    /**
     * Create a new dog tracker object and create a new empty track.
     */
    public DogTracker() {
        track = new Track();
        lastPositionChangedTime = 0;
        paused = true;
        state = State.START_HUMAN;
    }

    /**
     * Start the track.
     * Changes the dog tracker's state depending on which state it is currently in.
     * @param startingPoint The latest received mapbox point at which the track should start.
     */
    public void startTrack(Point startingPoint) {
        paused = false;

        if(state == State.START_HUMAN) {
            track = new Track();
            lastPositionChangedTime = 0;
            state = State.HUMAN;
        }
        else if(state == State.START_DOG)
            state = State.DOG;

        addPointToTrack(new TrackLocation(startingPoint, System.currentTimeMillis()));
        Log.e("State", state.toString());
    }

    /**
     * Pause data collection, no data will be lost.
     */
    public void pauseTrack() {
        paused = true;
    }

    /**
     * Resume data collection.
     */
    public void resumeTrack() {
        paused = false;
    }

    /**
     * Stop the tracking.
     * Changes the dog tracker's internal state depending on which state it is currently in.
     * Saves the track to cloud database.
     * @param stoppingPoint The latest received location from mapbox to use as an ending point.
     * @throws JSONException
     */
    public void stopTrack(Point stoppingPoint) throws JSONException {
        addPointToTrack(new TrackLocation(stoppingPoint, System.currentTimeMillis()));
        addSelectedDogToTrack();
        paused = true;

        DatabaseIO.saveTrack(this, task -> {
            if (task.isSuccessful()) {
                Log.e("DEBUG", "Success");
                //track = new Track();
            } else {
                Log.e("DEBUG", "Fail");
            }
        });

        if(state == State.HUMAN) {
            state = State.START_DOG;
        } else if(state == State.DOG) {
            lastPositionChangedTime = 0;
            state = State.START_HUMAN;
            User currentUser = User.getInstance();
            currentUser.addTrack(this);
            currentUser.getSelectedDog().addTotalDistance(track.dogDistance);
            currentUser.getSelectedDog().incrementTotalTracks();
            currentUser.updateSelectedDog();

            // TODO: Move from model
            DatabaseIO.saveDogs(currentUser.getDogs());
            DatabaseIO.saveSelectedDog(currentUser.getSelectedDog());
        }
    }

    /**
     * This method is called whenever mapbox updates the phone's current location.
     *
     * The method performs a series of checks to see if the updated location should be added to the track.
     * This is done to avoid an excessive amount of points being added and slowing down the system.
     *
     * @param point A mapbox Point object which contains the longitude and latitude.
     * @return True if the point was added, false if it was ignored.
     */
    public boolean onPositionChanged(Point point) {
        // add to the track's time
        updateTrackTime();

        // if we are not actively tracking, skip this update
        if(state != State.HUMAN && state != State.DOG) return false;

        // if we have paused the track, skip this update
        if(paused) return false;

        // which track do we want to add to?
        ArrayList<TrackLocation> selectedTrack;
        if(state == State.HUMAN)
            selectedTrack = new ArrayList<>(this.track.trackLocations);
        else
            selectedTrack = new ArrayList<>(this.track.dogLocations);

        // if the updated point is too close in distance (3 m for now), skip this update
        double distance = TurfMeasurement.distance(selectedTrack.get(selectedTrack.size() - 1).getPoint(), point, UNIT_METERS);
        if(distance < 3) return false;

        // finally - if all checks are ok, add the new point to the list
        addPointToTrack(new TrackLocation(point, System.currentTimeMillis()));
        track.calculateStatistics();
        return true;
    }

    private void updateTrackTime() {
        if(!paused && lastPositionChangedTime != 0) {
            long timeDelta = System.currentTimeMillis() - lastPositionChangedTime;
            if(state == State.HUMAN) track.humanTime += timeDelta;
            else if(state == State.DOG) track.dogTime += timeDelta;
        }
        lastPositionChangedTime = System.currentTimeMillis();
    }

    private void addSelectedDogToTrack() {
        User currentUser = User.getInstance();
        track.dog = currentUser.getSelectedDog();
    }

    public Dogs.Dog getDog() {
        return track.dog;
    }

    /**
     * This method should only be called internally or from DatabaseIO.
     *
     * Used for adding one location to the track.
     * @param trackLocation A new TrackLocation object which contains position and time.
     */
    public void addPointToTrack(TrackLocation trackLocation) {
        if(state == State.HUMAN) {
            track.trackLocations.add(trackLocation);
            return;
        }
        track.dogLocations.add(trackLocation);
    }

    /**
     * Used for adding one dumbbell to the track.
     * The method uses the last saved tracking point as the dumbbells location.
     */
    public void addDumbbellToTrack() {
        track.dumbbellLocations.add(track.trackLocations.get(track.trackLocations.size() - 1));
    }

    /**
     * Marks if the next dumbbell in the list was found or missed by the dog.
     * @param found True if the dumbbell was found.
     */
    public void markDumbbellFoundOrMissed(boolean found) {
        if(found) track.dumbbellsFound++;
        else track.dumbbellsMissed++;
    }

    public State getState() { return state; }
    public boolean isPaused() { return paused; }
    public ArrayList<TrackLocation> getTrackLocations() { return new ArrayList<>(track.trackLocations); }
    public ArrayList<TrackLocation> getDumbbellLocations() { return new ArrayList<>(track.dumbbellLocations); }
    public ArrayList<TrackLocation> getDogLocations() { return new ArrayList<>(track.dogLocations); }
    public int getDumbbellsFound() { return track.dumbbellsFound; }
    public int getDumbbellsMissed() { return track.dumbbellsMissed; }

    /**
     * @return The current distance of human OR dog track, depending on the dog tracker's internal state.
     */
    public double getDistance() {
        if(state == State.HUMAN || state == State.START_DOG)
            return track.humanDistance;
        return track.dogDistance;
    }

    /**
     * @return The current time of human OR dog track, depending on the dog tracker's internal state.
     */
    public long getTime() {
        if(state == State.HUMAN || state == State.START_DOG)
            return track.humanTime;
        return track.dogTime;
    }


    /**
     * Get a list of mapbox points to draw a polyline for the track.
     * @return An arraylist of Point objects.
     */
    public ArrayList<Point> getTrackPoints() {
        ArrayList<Point> points = new ArrayList<>();
        for(TrackLocation trackLocation : track.trackLocations)
            points.add(trackLocation.getPoint());

        return points;
    }

    /**
     * Get a list of mapbox points to draw the dog's track on the map.
     * @return An arraylist of Point objects.
     */
    public ArrayList<Point> getDogPoints() {
        ArrayList<Point> points = new ArrayList<>();
        for(TrackLocation trackLocation : track.dogLocations)
            points.add(trackLocation.getPoint());
        return points;
    }

    /**
     * Class representing a complete track.
     * The track contains one list of location points where the human walked, and one where the dog walked.
     */
    public static class Track {
        private Dogs.Dog dog;

        private final ArrayList<TrackLocation> trackLocations;
        private final ArrayList<TrackLocation> dumbbellLocations;
        private final ArrayList<TrackLocation> dogLocations;

        private double humanDistance;
        private long humanTime;

        private double dogDistance;
        private long dogTime;

        private int dumbbellsFound;
        private int dumbbellsMissed;

        /**
         * Create a new track object and initialize all track stats to 0.
         */
        public Track() {
            dog = new Dogs.Dog();
            trackLocations = new ArrayList<>();
            dumbbellLocations = new ArrayList<>();
            dogLocations = new ArrayList<>();
            humanDistance = 0;
            humanTime = 0;
            dogDistance = 0;
            dogTime = 0;
            dumbbellsFound = 0;
            dumbbellsMissed = 0;
        }

        private void calculateStatistics() {
            if(trackLocations.size() > 1) {
                humanDistance = 0;
                for(int i = 0; i < trackLocations.size() - 1; i++)
                    humanDistance += TurfMeasurement.distance(trackLocations.get(i).getPoint(), trackLocations.get(i + 1).getPoint(), UNIT_METERS);
            }

            if(dogLocations.size() > 1) {
                dogDistance = 0;
                for(int i = 0; i < dogLocations.size() - 1; i++)
                    dogDistance += TurfMeasurement.distance(dogLocations.get(i).getPoint(), dogLocations.get(i + 1).getPoint(), UNIT_METERS);
            }
        }
    }
}