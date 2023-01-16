package se.kth.dogtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import se.kth.dogtracker.model.User;
import se.kth.dogtracker.view.TracksAdapter;

/**
 * This class sets up the layout and functionality for the activity that displays previous tracks for the user.
 */
public class TracksActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);

        // set up custom action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.title_activity_tracks));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        User user = User.getInstance();

        // set up recycler view for previous tracks
        RecyclerView tracks = findViewById(R.id.recycler_view_tracks);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        tracks.setLayoutManager(layoutManager);
        tracks.setAdapter(new TracksAdapter(user.getTracks(), this::onTrackClicked));
    }

    private void onTrackClicked(int position) {
        Intent intent = new Intent(this, TrackStatsActivity.class);
        intent.putExtra("position", position);
        startActivity(intent);
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
}