package se.kth.dogtracker.io;

import se.kth.dogtracker.model.Dogs;

/**
 * Interface for listening to the retrieval of the selected dog.
 */
public interface OnSelectedDogRetrievedListener {
    void onDogRetrieved(Dogs.Dog dog);
}
