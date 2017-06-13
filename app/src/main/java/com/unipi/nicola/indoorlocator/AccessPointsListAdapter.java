package com.unipi.nicola.indoorlocator;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.unipi.nicola.indoorlocator.R;
import com.unipi.nicola.indoorlocator.fingerprinting.AccessPointInfos;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by Nicola on 11/06/2017.
 */

public class AccessPointsListAdapter extends ArrayAdapter<AccessPointInfos> {
    public AccessPointsListAdapter(Context context, List<AccessPointInfos> users) {
        super(context, 0, users);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AccessPointInfos apinfos = getItem(position);
        // check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.accesspoints_list_item, parent, false);
        }
        // lookup view for data population
        TextView hwAddr = (TextView) convertView.findViewById(R.id.hw_addr);
        TextView signalStrength = (TextView) convertView.findViewById(R.id.signal_strength);

        // populate the data into the template view using the data object
        hwAddr.setText(apinfos.getHwAddress());
        signalStrength.setText(String.valueOf(apinfos.getSignalStrength()));

        return convertView;
    }
}
