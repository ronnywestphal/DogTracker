package se.kth.dogtracker.io;

import se.kth.dogtracker.model.Dogs;

/**
 * Interface for listening to the retrieval of the list of dogs.
 */
public interface OnDogsRetrievedListener {
    void onDogsRetrieved(Dogs dogs);
}
