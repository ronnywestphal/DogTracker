package se.kth.dogtracker.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import se.kth.dogtracker.R;
import se.kth.dogtracker.model.DogTracker;

/**
 * Class for adapting a recycler view to hold previous tracks.
 */
public class TracksAdapter extends RecyclerView.Adapter<TracksAdapter.ViewHolder> {

    private final ArrayList<DogTracker> localDataSet;

    // interface for callbacks when item selected
    public interface IOnItemSelectedCallBack { void onItemClicked(int position); }
    private final IOnItemSelectedCallBack mOnItemSelectedCallback;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final IOnItemSelectedCallBack mOnItemSelectedCallBack;
        private final TextView uiName;
        private final TextView uiStats;

        public ViewHolder(View view, IOnItemSelectedCallBack onItemSelectedCallBack) {
            super(view);
            itemView.setOnClickListener(this);
            mOnItemSelectedCallBack = onItemSelectedCallBack;

            uiName = view.findViewById(R.id.track_name);
            uiStats = view.findViewById(R.id.track_stats);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            mOnItemSelectedCallBack.onItemClicked(position);
        }

        public TextView getUIName() {
            return uiName;
        }
        public TextView getUIStats() { return uiStats; }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet ArrayList<DogTracker> containing the data to populate views to be used
     * by RecyclerView.
     */
    public TracksAdapter(ArrayList<DogTracker> dataSet, IOnItemSelectedCallBack onItemSelectedCallBack) {
        localDataSet = dataSet;
        mOnItemSelectedCallback = onItemSelectedCallBack;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.card_tracks, viewGroup, false);
        return new ViewHolder(view, mOnItemSelectedCallback);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        Context context = viewHolder.itemView.getContext();

        DogTracker dataAtPosition = localDataSet.get(position);

        String formattedDate = "";
        if(dataAtPosition.getTrackLocations().size() > 0) {
            Date date = new Date(dataAtPosition.getTrackLocations().get(0).getTime());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            formattedDate = sdf.format(date);
        }
        viewHolder.getUIName().setText(context.getString(R.string.track_name, formattedDate, dataAtPosition.getDog().getName()));

        long minutes = dataAtPosition.getTime() / 60000;
        long seconds = (dataAtPosition.getTime() % 60000) / 1000;
        viewHolder.getUIStats().setText(context.getString(R.string.track_stats, dataAtPosition.getDistance(), minutes, seconds, dataAtPosition.getDumbbellsFound(), dataAtPosition.getDumbbellsFound() + dataAtPosition.getDumbbellsMissed()));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return localDataSet.size();
    }
}

