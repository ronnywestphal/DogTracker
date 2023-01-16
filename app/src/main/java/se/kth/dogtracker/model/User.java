package se.kth.dogtracker.model;

import java.util.ArrayList;

import se.kth.dogtracker.io.DatabaseIO;

/**
 * Class representing the user model
 */
public class User {
    private String name, email;
    private ArrayList<DogTracker> dogTrackers;
    private static User instance;
    private Dogs dogs;
    private Dogs.Dog selectedDog;

    private User(String name, String email) {
        this.name = name;
        this.email = email;
        dogTrackers = new ArrayList<>();
        dogs = new Dogs();
        selectedDog = null;
    }

    /**
     * Get the current instance of the User
     *
     * @return the current instance of the User
     */
    public static User getInstance() {
        return instance;
    }


    /**
     * Create a new instance of the User
     *
     * @param name The name of the User
     * @param email The email of the User
     * @return a new instance of the User
     */
    public static User newInstance(String name, String email) {
        instance = new User(name, email);
        return instance;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Add a new track to the User's list of DogTrackers
     * @param dogTracker The DogTracker to be added
     */
    public void addTrack(DogTracker dogTracker) {
        dogTrackers.add(dogTracker);
    }

    /**
     * Update the information of the currently selected dog from the User's list of Dogs
     * This is used to keep the User's list of Dogs in sync with the currently selected Dog
     * The updated dog's information (name, picture, total distance and total tracks) are taken from the selectedDog property
     * and the list of dogs will be saved to the database.
     */
    public void updateSelectedDog() {
        int index = dogs.getDogs().indexOf(selectedDog);
        Dogs.Dog updatedDog = dogs.getDogs().get(index);
        updatedDog.setName(selectedDog.getName());
        updatedDog.setPicture(selectedDog.getPicture());
        updatedDog.setTotalDistance(selectedDog.getTotalDistance());
        updatedDog.setTotalTracks(selectedDog.getTotalTracks());
        dogs.getDogs().set(index, updatedDog);

        // TODO: Move from model
        DatabaseIO.saveDogs(dogs);
    }

    public Dogs getDogs() {
        if (dogs == null) {
            dogs = new Dogs();
        }
        return dogs;
    }

    public Dogs.Dog getSelectedDog() {
        return selectedDog;
    }

    public void setSelectedDog(Dogs.Dog selectedDog) {
        this.selectedDog = selectedDog;
    }

    public void setDogs(Dogs dogs) {
        this.dogs = dogs;
    }

    public ArrayList<DogTracker> getTracks() {
        return (ArrayList<DogTracker>) dogTrackers.clone();
    }
}
