package se.kth.dogtracker.io;

import java.util.ArrayList;

import se.kth.dogtracker.model.DogTracker;

/**
 * Interface for listening to the retrieval of the list of tracks.
 */
public interface OnTracksRetrievedListener {
    void onTracksRetrieved(ArrayList<DogTracker> tracks);
}

