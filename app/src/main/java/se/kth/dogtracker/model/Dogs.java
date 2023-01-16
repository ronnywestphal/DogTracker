package se.kth.dogtracker.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import se.kth.dogtracker.R;

/**
 * Class representing the users dogs.
 */
public class Dogs {

    private ArrayList<Dog> dogs;

    public Dogs() {
        dogs = new ArrayList<>();
    }

    /**
     * Add a new dog.
     */
    public void addDog() {
        dogs.add(new Dog());
    }

    /**
     * Remove a dog.
     * @param index The dogs index in the internal list.
     */
    public void removeDog(int index) {
        dogs.remove(index);
    }

    public ArrayList<Dog> getDogs() {
        return new ArrayList<>(dogs);
    }

    /**
     * Class representing a dog.
     */
    public static class Dog {

        private String name;
        private byte[] picture;
        private double totalDistance;
        private int totalTracks;

        /**
         * Create a new dog object and assign default values.
         */
        public Dog() {
            name = "My Buddy!";
            totalDistance = 0.0;
            totalTracks = 0;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getTotalDistance() {
            return totalDistance;
        }

        public void setTotalDistance(double totalDistance) {
            this.totalDistance = totalDistance;
        }

        public void addTotalDistance(double distance) {
            totalDistance += distance;
        }

        public int getTotalTracks() {
            return totalTracks;
        }

        public void setTotalTracks(int totalTracks) {
            this.totalTracks = totalTracks;
        }

        public void incrementTotalTracks() {
            totalTracks++;
        }

        public byte[] getPicture() {
            return picture;
        }

        /**
         * Set the profile picture for the dog in the form of a bitmap.
         * @param picture A picture in the form of a bitmap.
         */
        public void setBitmapPicture(Bitmap picture) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            picture.compress(Bitmap.CompressFormat.PNG, 100, stream);
            this.picture = stream.toByteArray();
        }

        /**
         * Set the profile picture for the dog in the form of a byte array.
         * @param picture A picture in the form of a byte array.
         */
        public void setPicture(byte[] picture) {
            this.picture = picture;
        }

        /**
         * @return The dog's profile picture in the form of a bitmap picture.
         */
        public Bitmap getBitmapPicture() {
            if (picture == null)
                return null;

            return BitmapFactory.decodeByteArray(picture, 0, picture.length);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Dog dog = (Dog) obj;
            return name.equals(dog.name);
        }
    }
}
