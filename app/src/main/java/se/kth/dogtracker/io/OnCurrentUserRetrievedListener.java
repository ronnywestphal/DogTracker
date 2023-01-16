package se.kth.dogtracker.io;

import se.kth.dogtracker.model.User;

/**
 * Interface for listening to the retrieval of the current user.
 */
public interface OnCurrentUserRetrievedListener {
    void onCurrentUserRetrieved(User user);
}