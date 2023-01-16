package se.kth.dogtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

import se.kth.dogtracker.io.DatabaseIO;
import se.kth.dogtracker.model.User;
import se.kth.dogtracker.view.ProfilesAdapter;

/**
 * This class is responsible for handling the UI and logic for displaying and managing the dog profiles of a user.
 */
public class ProfilesActivity extends AppCompatActivity {

    private User currentUser;

    private ProfilesAdapter profilesAdapter;
    private int selectedProfile = 0;
    private int selectedIndex = 0;

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles);

        currentUser = User.getInstance();

        // set up custom action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.title_activity_dogs));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // set up recycler view for dog profiles
        RecyclerView profiles = findViewById(R.id.recycler_view_profiles);
        profiles.setLayoutManager(new LinearLayoutManager(this));
        if (currentUser.getDogs() != null) {
            profilesAdapter = new ProfilesAdapter(currentUser.getDogs().getDogs(), this::onCameraClicked, this::onSelectClicked, this::onDeleteClicked, this::onNameClicked);
        } else {
            profilesAdapter = new ProfilesAdapter(new ArrayList<>(), this::onCameraClicked, this::onSelectClicked, this::onDeleteClicked, this::onNameClicked);
        }
        profiles.setAdapter(profilesAdapter);

        // set up buttons
        findViewById(R.id.button_profiles_add).setOnClickListener(v -> {
            if (currentUser != null && currentUser.getDogs() != null) {
                currentUser.getDogs().addDog();
                if (currentUser.getSelectedDog() == null) {
                    currentUser.setSelectedDog(currentUser.getDogs().getDogs().get(0));
                    DatabaseIO.saveSelectedDog(currentUser.getSelectedDog());
                }
                DatabaseIO.saveDogs(currentUser.getDogs());
                profilesAdapter.updateDataSet(currentUser.getDogs().getDogs());
            }
        });
    }

    private void onCameraClicked(int dogIndex) {
        selectedProfile = dogIndex;
        dispatchTakePictureIntent();
    }

    private void onSelectClicked(int dogIndex) {
        selectedIndex = dogIndex;
        currentUser.setSelectedDog(currentUser.getDogs().getDogs().get(dogIndex));
        DatabaseIO.saveSelectedDog(currentUser.getSelectedDog());
        Toast.makeText(this, currentUser.getDogs().getDogs().get(dogIndex).getName() + " selected", Toast.LENGTH_SHORT).show();

    }

    private void onDeleteClicked(int dogIndex) {
        Toast.makeText(this, currentUser.getDogs().getDogs().get(dogIndex).getName() + " deleted", Toast.LENGTH_SHORT).show();
        currentUser.getDogs().removeDog(dogIndex);

        if (currentUser.getDogs().getDogs().size() != 0) {
            currentUser.setSelectedDog(currentUser.getDogs().getDogs().get(0));
            Toast.makeText(this, currentUser.getDogs().getDogs().get(0).getName() + " selected", Toast.LENGTH_SHORT).show();
            DatabaseIO.saveDogs(currentUser.getDogs());
            DatabaseIO.saveSelectedDog(currentUser.getSelectedDog());
        } else {
            DatabaseIO.clearDogs();
            DatabaseIO.clearSelectedDog();
        }

        profilesAdapter.updateDataSet(currentUser.getDogs().getDogs());
    }

    private void onNameClicked(int dogIndex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Name");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            String name = input.getText().toString().trim();
            currentUser.getDogs().getDogs().get(dogIndex).setName(name);
            DatabaseIO.saveDogs(currentUser.getDogs());
            if (currentUser.getDogs().getDogs().get(selectedIndex) == currentUser.getDogs().getDogs().get(dogIndex)) {
                currentUser.setSelectedDog(currentUser.getDogs().getDogs().get(dogIndex));
                DatabaseIO.saveSelectedDog(currentUser.getSelectedDog());
            }
            profilesAdapter.updateDataSet(currentUser.getDogs().getDogs());
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // this event will enable the back function to the button on press
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void dispatchTakePictureIntent() {
        Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(camera_intent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Match the request 'pic id with requestCode
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");

            Matrix rotationMatrix = new Matrix();
            if(photo.getWidth() >= photo.getHeight())
                rotationMatrix.setRotate(90);
            Bitmap rotatedPhoto = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), rotationMatrix, true);

            // we have to crop the photo to a square, otherwise it gets stretched when we make a round drawable
            Bitmap croppedPhoto = rotatedPhoto;
            if(rotatedPhoto.getWidth() > rotatedPhoto.getHeight()) {
                int width = rotatedPhoto.getWidth();
                int height = rotatedPhoto.getHeight();
                int crop = (width - height) / 2;
                croppedPhoto = Bitmap.createBitmap(rotatedPhoto, crop, 0, height, height);
            }
            else if(rotatedPhoto.getWidth() < rotatedPhoto.getHeight()){
                int width = rotatedPhoto.getWidth();
                int height = rotatedPhoto.getHeight();
                int crop = (height - width) / 2;
                croppedPhoto = Bitmap.createBitmap(rotatedPhoto, 0, crop, width, width);
            }

            currentUser.getDogs().getDogs().get(selectedProfile).setBitmapPicture(croppedPhoto);
            profilesAdapter.updateDataSet(currentUser.getDogs().getDogs());
            DatabaseIO.saveDogs(currentUser.getDogs());
            if (currentUser.getDogs().getDogs().get(selectedProfile) == currentUser.getDogs().getDogs().get(selectedIndex)) {
                currentUser.setSelectedDog(currentUser.getDogs().getDogs().get(selectedIndex));
                DatabaseIO.saveSelectedDog(currentUser.getSelectedDog());
            }
        }
    }
}