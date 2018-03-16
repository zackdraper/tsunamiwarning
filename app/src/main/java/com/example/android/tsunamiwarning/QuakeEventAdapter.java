package com.example.android.tsunamiwarning;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by zackdraper on 07/02/18.
 */

public class QuakeEventAdapter extends RecyclerView.Adapter<QuakeEventAdapter.QuakeViewHolder> {

    private static final String TAG = QuakeEventAdapter.class.getSimpleName();

    final private ListItemClickListener mOnClickListener;

    private static int viewHolderCount;

    private ArrayList<String[]> mQuakeList;

    public interface ListItemClickListener {
        void onListItemClick(int clickedItemIndex, String message);
    }

    public QuakeEventAdapter(ArrayList<String[]> quakeEventsArr, ListItemClickListener listener) {
        mQuakeList = quakeEventsArr;
        mOnClickListener = listener;
        viewHolderCount = 0;
    }

    public int MagWarningColor(Context context, float magnitude) {

        if (magnitude > 7.0) {
            return ContextCompat.getColor(context, R.color.warningRed);
        }

        if (magnitude < 5.5) {
            return ContextCompat.getColor(context, R.color.warningGreen);
        } else {
            return ContextCompat.getColor(context, R.color.warningYellow);
        }

    }

    @Override
    public QuakeViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.quake_event_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        QuakeViewHolder viewHolder = new QuakeViewHolder(view);

        //viewHolder.listEventMagView.setText(mQuakeList.get(viewHolderCount)[1]);

        viewHolderCount++;

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(QuakeViewHolder holder, int position) {
        Context context = holder.itemView.getContext();

        SharedPreferences lastKnownLocationP = PreferenceManager.getDefaultSharedPreferences(context);
        String lastKnownLocation = lastKnownLocationP.getString("lastKnownLocation", null);

        Log.d(TAG, "#" + position+" : "+lastKnownLocation);
        holder.bind(position,lastKnownLocation);

        float magnitude = Float.parseFloat(mQuakeList.get(position)[0]);

        int backgroundColorForViewHolder = MagWarningColor(context, magnitude);

        //Log.d("ADebugTag", "Value: " + magnitude + " : " +backgroundColorForViewHolder);

        holder.itemView.setBackgroundColor(backgroundColorForViewHolder);

    }

    @Override
    public int getItemCount() {
        return mQuakeList.size();
    }

    class QuakeViewHolder extends RecyclerView.ViewHolder
        implements OnClickListener {

        TextView listEventMagView;
        TextView listEventTimeView;
        TextView listEventDiscripView;
        TextView listEventDistanceView;


        public QuakeViewHolder(View itemView) {
            super(itemView);

            listEventMagView = itemView.findViewById(R.id.tv_event_mag);
            listEventTimeView = itemView.findViewById(R.id.tv_event_time);
            listEventDiscripView = itemView.findViewById(R.id.tv_event_description);
            listEventDistanceView = itemView.findViewById(R.id.tv_event_dist);

            itemView.setOnClickListener(this);

        }

        void bind(int listIndex,String lastKnownLocation) {
            //listEventMagView.setText(String.valueOf(listIndex));
            listEventMagView.setText(mQuakeList.get(listIndex)[0]);
            listEventTimeView.setText(mQuakeList.get(listIndex)[1]);
            listEventDiscripView.setText(mQuakeList.get(listIndex)[2]);

            String latitude = mQuakeList.get(listIndex)[4];
            String longitude = mQuakeList.get(listIndex)[5];

            Location location_event = new Location("point A");
            location_event.setLatitude(Double.parseDouble(latitude));
            location_event.setLongitude(Double.parseDouble(longitude));

            //mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            //Task location_phone_task = mFusedLocationClient.getLastLocation();
            //Location location_phone = location_phone_task.g;

            Double distanceInMeters = 0.0;
            try {
                //Double distanceInMeters = location_event.distanceTo(lastKnownLocation);
            } catch (Exception e) {
                e.printStackTrace();
                //Float distanceInMeters = 1;
            }
            String distance = String.format(Locale.getDefault()
                    ,"%.0f km",distanceInMeters/1000 );

            listEventDistanceView.setText(distance);
        }

        @Override
        public void onClick(View v) {
            int clickedPosition = getAdapterPosition();

            mOnClickListener.onListItemClick(clickedPosition, mQuakeList.get(clickedPosition)[3]);

        }
    }
}
