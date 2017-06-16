package com.unipi.nicola.indoorlocator;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TableRow;
import android.widget.TextView;

import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprint;

import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by Nicola on 11/06/2017.
 */

public class FingerprintsListAdapter extends ArrayAdapter<WifiFingerprint> {

    double distanceThreshold;
    int numberOfNearestNeighbors;

    public FingerprintsListAdapter(Context context, List<WifiFingerprint> users) {
        super(context, 0, users);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        distanceThreshold = Double.valueOf(sharedPref.getString(SettingsActivity.PREF_DISTANCE_THRESHOLD_KEY, "10"));
        numberOfNearestNeighbors = Integer.valueOf(sharedPref.getString(SettingsActivity.PREF_NEAREST_NEIGHBORS_NUMBER_KEY, "3"));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        WifiFingerprint fingerprint = getItem(position);
        // check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.fingerprints_list_item, parent, false);
        }
        // lookup view for data population
        TextView locationLabel = (TextView) convertView.findViewById(R.id.location_label);
        TextView fpDistance = (TextView) convertView.findViewById(R.id.fp_distance);
        TextView rank = (TextView) convertView.findViewById(R.id.fp_rank);

        //if this fingerprint is out of range, change the visual appearance
        Log.d("test","------------------"+position+" vs "+numberOfNearestNeighbors+"---------------------"+fingerprint.getDistance()+" vs "+distanceThreshold);
        TableRow row = (TableRow)convertView.findViewById(R.id.fp_row);
        if(position + 1 > numberOfNearestNeighbors || fingerprint.getDistance() > distanceThreshold){
            row.setBackgroundColor(Color.parseColor("#ffcc99"));
        } else {
            row.setBackgroundColor(Color.parseColor("#ffffff"));
        }

        // populate the data into the template view using the data object
        locationLabel.setText(fingerprint.getLocationLabel());
        DecimalFormat df = new DecimalFormat("0.00");
        fpDistance.setText(df.format(fingerprint.getDistance()));
        rank.setText(String.valueOf(position+1));

        return convertView;
    }
}
