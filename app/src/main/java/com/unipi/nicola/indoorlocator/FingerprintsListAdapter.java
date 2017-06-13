package com.unipi.nicola.indoorlocator;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprint;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by Nicola on 11/06/2017.
 */

public class FingerprintsListAdapter extends ArrayAdapter<WifiFingerprint> {
    public FingerprintsListAdapter(Context context, List<WifiFingerprint> users) {
        super(context, 0, users);
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

        // populate the data into the template view using the data object
        locationLabel.setText(fingerprint.getLocationLabel());
        DecimalFormat df = new DecimalFormat("0.00");
        fpDistance.setText(df.format(fingerprint.getDistance()));
        rank.setText(String.valueOf(position+1));

        return convertView;
    }
}
