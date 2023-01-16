package se.kth.dogtracker.io;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.kth.dogtracker.model.DogTracker;
import se.kth.dogtracker.model.Dogs;
import se.kth.dogtracker.model.User;

/**
 * This class contains methods for communication with a Firebase database
 */
public class DatabaseIO {

    /**
     * Saves the given DogTracker object to the Firebase Database, using the current user's UID as the parent node.
     * Depending on the state of the DogTracker object, this method will either push a new child node to the "tracks" child of the user or update the last track with the new DogTracker object.
     *
     * @param dogTracker The DogTracker object to be saved to the Firebase Database
     * @param onCompleteListener The OnCompleteListener to be notified when the save operation is complete
     * @throws JSONException If there is an error converting the DogTracker object to a JSON string
     */
    public static void saveTrack(DogTracker dogTracker, OnCompleteListener onCompleteListener) throws JSONException {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser.getUid();
        Log.e("DEBUG", dogTracker.getState().toString());

        DatabaseReference tracksRef = FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("tracks");

        if (dogTracker.getState() == DogTracker.State.HUMAN || dogTracker.getState() == DogTracker.State.START_DOG) {
             Gson gsonPush = new GsonBuilder().create();
             String jsonPush = gsonPush.toJson(dogTracker);
             tracksRef.push().setValue(jsonPush);
        }

        if (dogTracker.getState() == DogTracker.State.DOG || dogTracker.getState() == DogTracker.State.START_HUMAN) {
            tracksRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<String> tracksKeys = new ArrayList<>();
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        tracksKeys.add(childSnapshot.getKey());
                    }

                    if (!tracksKeys.isEmpty()) {
                        String lastKey = tracksKeys.get(tracksKeys.size() - 1);
                        tracksRef.child(lastKey).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Gson gson = new GsonBuilder().create();
                                String json = gson.toJson(dogTracker);
                                Task task = tracksRef.child(lastKey).setValue(json);
                                task.addOnCompleteListener(onCompleteListener);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                // Handle error
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle error
                }
            });
        }
    }

    /**
     * Retrieves the tracks of the currently logged in user from the Firebase Database.
     * Each track is represented as a DogTracker object, and the method returns an ArrayList of these objects.
     *
     * @param listener The OnTracksRetrievedListener to be notified when the retrieval is complete, containing the list of retrieved DogTracker objects
     */
    public static void retrieveTracks(OnTracksRetrievedListener listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser.getUid();

        DatabaseReference tracksRef = FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("tracks");

        final ArrayList<DogTracker> tracks = new ArrayList<>();

        tracksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    String json = childSnapshot.getValue(String.class);
                    Gson gson = new GsonBuilder().create();
                    DogTracker track = gson.fromJson(json, DogTracker.class);
                    tracks.add(track);
                }
                listener.onTracksRetrieved(tracks);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    /**
     * Retrieves the current user from the Firebase Database.
     *
     * @param listener The OnCurrentUserRetrievedListener to be notified when the retrieval is complete, containing the current user's User object
     */
    public static void getCurrentUser(OnCurrentUserRetrievedListener listener) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        DatabaseReference reference = database.getReference("Users").child(userId);

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);
                User user = User.newInstance(name, email);
                listener.onCurrentUserRetrieved(user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * Saves the given Dogs object to the Firebase Database, using the current user's UID as the parent node.
     *
     * @param dogs The Dogs object to be saved to the Firebase Database
     */
    public static void saveDogs(Dogs dogs) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser.getUid();

        DatabaseReference dogsRef = FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("dogs");
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(dogs);
        dogsRef.setValue(json);
    }

    /**
     * Retrieves a list of dogs associated with the current logged-in user from a Firebase database.
     *
     * @param listener A listener that will be notified when the dogs have been retrieved. The listener should implement the
     *                 OnDogsRetrievedListener interface and handle the Dogs object that is passed to the
     *                 OnDogsRetrievedListener.onDogsRetrieved(Dogs) method.
     */
    public static void retrieveDogs(OnDogsRetrievedListener listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser.getUid();

        DatabaseReference dogsRef = FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("dogs");

        dogsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Gson gson = new GsonBuilder().create();
                Dogs dogs = gson.fromJson(dataSnapshot.getValue(String.class), Dogs.class);
                listener.onDogsRetrieved(dogs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * Saves the selected dog to the Firebase Database for the current user.
     *
     * @param dog The Dog object to be saved.
     */
    public static void saveSelectedDog(Dogs.Dog dog) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser.getUid();

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);

        Gson gson = new Gson();
        String dogJson = gson.toJson(dog);
        userRef.child("selectedDog").setValue(dogJson);
    }

    /**
     * Retrieves the selected dog from the Firebase Database for the current user.
     *
     * @param listener The OnSelectedDogRetrievedListener to be called when the dog is retrieved.
     */
    public static void retrieveSelectedDog(OnSelectedDogRetrievedListener listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser.getUid();

        DatabaseReference selectedDogRef = FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("selectedDog");

        selectedDogRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Gson gson = new GsonBuilder().create();
                Dogs.Dog dog = gson.fromJson(dataSnapshot.getValue(String.class), Dogs.Dog.class);
                listener.onDogRetrieved(dog);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * Clears the list of dogs stored in the Firebase Database for the current user.
     */
    public static void clearDogs() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser.getUid();

        DatabaseReference dogsRef = FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("dogs");
        dogsRef.removeValue();
    }

    /**
     * Clears the selected dog from the Firebase Database for the current user.
     */
    public static void clearSelectedDog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser.getUid();

        DatabaseReference selectedDogRef = FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("selectedDog");
        selectedDogRef.removeValue();
    }
}

