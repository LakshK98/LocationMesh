package com.locmesh.locationmesh.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.locmesh.locationmesh.R;

import java.util.ArrayList;

/**
 * Created by lakshkotian on 15/03/19.
 */

public class MainRecyclerAdapter extends RecyclerView.Adapter<MainRecyclerAdapter.MainViewHolder>{
    private ArrayList<String[]> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MainViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView nameTextView,latTextView,longTextView;
        public MainViewHolder(View v) {
            super(v);
            nameTextView = v.findViewById(R.id.name_text_view);
            latTextView = v.findViewById(R.id.lat_text_view);
            longTextView = v.findViewById(R.id.long_text_view);

        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public  MainRecyclerAdapter(ArrayList<String[]>  myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MainRecyclerAdapter.MainViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.main_recycler, parent, false);

        MainViewHolder vh = new MainViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MainViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        String[] values=mDataset.get(position);

        holder.nameTextView.setText(values[0]);
        holder.latTextView.setText(values[1]);
        holder.longTextView.setText(values[2]);


    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
