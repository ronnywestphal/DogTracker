package se.kth.dogtracker.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import se.kth.dogtracker.R;
import se.kth.dogtracker.model.Dogs;

/**
 * Class for adapting a recycler view to hold previous tracks.
 */
public class ProfilesAdapter extends RecyclerView.Adapter<ProfilesAdapter.ViewHolder> {

    private ArrayList<Dogs.Dog> localDataSet;

    // interface for callbacks when item selected
    public interface IOnButtonClickedCallback { void OnButtonClicked(int position); }
    private final IOnButtonClickedCallback mOnCameraClickedCallback;
    private final IOnButtonClickedCallback mOnSelectClickedCallback;
    private final IOnButtonClickedCallback mOnDeleteClickedCallback;
    private final IOnButtonClickedCallback mOnNameClickedCallback;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final IOnButtonClickedCallback mOnCameraClickedCallback;
        private final IOnButtonClickedCallback mOnSelectClickedCallback;
        private final IOnButtonClickedCallback mOnDeleteClickedCallback;
        private final IOnButtonClickedCallback mOnNameClickedCallback;

        private final TextView uiName;
        private final TextView uiTotalTracks;
        private final TextView uiTotalDistance;
        private final AppCompatImageView profilePicImage;

        public ViewHolder(View view, IOnButtonClickedCallback onCameraClickedCallback, IOnButtonClickedCallback onSelectClickedCallback, IOnButtonClickedCallback onDeleteClickedCallback, IOnButtonClickedCallback onNameClickedCallback) {
            super(view);

            mOnCameraClickedCallback = onCameraClickedCallback;
            mOnSelectClickedCallback = onSelectClickedCallback;
            mOnDeleteClickedCallback = onDeleteClickedCallback;
            mOnNameClickedCallback = onNameClickedCallback;

            uiName = view.findViewById(R.id.profile_name);
            uiTotalTracks = view.findViewById(R.id.profile_tracks);
            uiTotalDistance = view.findViewById(R.id.profile_distance);
            profilePicImage = view.findViewById(R.id.image_profile_picture);

            view.findViewById(R.id.button_profile_camera).setOnClickListener(v -> mOnCameraClickedCallback.OnButtonClicked(getAdapterPosition()));
            view.findViewById(R.id.button_profile_select).setOnClickListener(v -> mOnSelectClickedCallback.OnButtonClicked(getAdapterPosition()));
            view.findViewById(R.id.button_profile_delete).setOnClickListener(v -> mOnDeleteClickedCallback.OnButtonClicked(getAdapterPosition()));
            view.findViewById(R.id.button_profile_name).setOnClickListener(v -> mOnNameClickedCallback.OnButtonClicked(getAdapterPosition()));
        }

        public TextView getUIName() {
            return uiName;
        }
        public TextView getUiTotalTracks() { return uiTotalTracks; }
        public TextView getUiTotalDistance() { return uiTotalDistance; }
        public AppCompatImageView getProfilePicImage() { return profilePicImage; }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet ArrayList<Dogs.Dog> containing the data to populate views to be used
     * by RecyclerView.
     */
    public ProfilesAdapter(ArrayList<Dogs.Dog> dataSet, IOnButtonClickedCallback onCameraClickedCallback, IOnButtonClickedCallback onSelectClickedCallback, IOnButtonClickedCallback onDeleteClickedCallback, IOnButtonClickedCallback onNameClickedCallback) {
        localDataSet = dataSet;

        mOnCameraClickedCallback = onCameraClickedCallback;
        mOnSelectClickedCallback = onSelectClickedCallback;
        mOnDeleteClickedCallback = onDeleteClickedCallback;
        mOnNameClickedCallback = onNameClickedCallback;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateDataSet(ArrayList<Dogs.Dog> updatedProfiles) {
        localDataSet = new ArrayList<>(updatedProfiles);
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.card_profiles, viewGroup, false);
        return new ViewHolder(view, mOnCameraClickedCallback, mOnSelectClickedCallback, mOnDeleteClickedCallback, mOnNameClickedCallback);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        Context context = viewHolder.itemView.getContext();

        Dogs.Dog dataAtPosition = localDataSet.get(position);
        viewHolder.getUIName().setText(dataAtPosition.getName());

        String s = "s";
        if(dataAtPosition.getTotalTracks() != 1)
            s = "s";
        viewHolder.getUiTotalTracks().setText(context.getString(R.string.profile_tracks, dataAtPosition.getTotalTracks(), s));
        viewHolder.getUiTotalDistance().setText(context.getString(R.string.profile_distance, dataAtPosition.getTotalDistance() / 1000));

        if(dataAtPosition.getBitmapPicture() != null) {
            RoundedBitmapDrawable roundDrawable = RoundedBitmapDrawableFactory.create(viewHolder.profilePicImage.getResources(), dataAtPosition.getBitmapPicture());
            roundDrawable.setCircular(true);
            viewHolder.getProfilePicImage().setImageDrawable(roundDrawable);
        }
        else
            viewHolder.getProfilePicImage().setImageResource(R.drawable.empty_profile_picture);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return localDataSet.size();
    }
}

