package com.unipi.nicola.indoorlocator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

/**
 * Created by Nicola on 08/06/2017.
 */

public class FPStoreFragment extends Fragment implements View.OnClickListener, LocationListener{
    private static final String TAG = "FPStoreFragment";
    View rootView;  //to be inflated with this fragment xml
    LocationManager locationManager;
    String locationProvider;
    private static final int PLACE_PICKER_REQUEST = 1;

    EditText lat;
    EditText lon;
    EditText alt;
    Button pickPlace;
    CheckBox gpsOn;
    TextView accuracy;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_fp_store, container, false);

        //initialize the location provider in case gps is needed
        // Get the location manager
        locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        // Define the criteria how to select the locatioin provider
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationProvider = locationManager.getBestProvider(criteria, false);

        lat = (EditText) rootView.findViewById(R.id.latitude);
        lon = (EditText) rootView.findViewById(R.id.longitude);
        alt = (EditText) rootView.findViewById(R.id.altitude);
        pickPlace = (Button) rootView.findViewById(R.id.pick_location);
        pickPlace.setOnClickListener(this);
        gpsOn = (CheckBox) rootView.findViewById(R.id.gps_on);
        gpsOn.setOnClickListener(this);
        accuracy = (TextView) rootView.findViewById(R.id.gps_accuracy);

        return rootView;
    }

    @Override
    public void onStart(){
        super.onStart();
        //the location acquiring restarts only if the checkbox is checked
        if(gpsOn.isChecked()) {
            try {
                locationManager.requestLocationUpdates(locationProvider, 0, 0, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        //stopping the location acquiring is necessary only if the gps checkbox was checked
        if(gpsOn.isChecked()) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(View v){
        if(v.getId() == R.id.pick_location){
            //handles the pick place button starting the place picker activity
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            try {
                startActivityForResult(builder.build(getActivity()), PLACE_PICKER_REQUEST);
            } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            }
        }
        if(v.getId() == R.id.gps_on){
            //handles the case in which the location is provided by gps
            CheckBox gpsOn = (CheckBox) v;
            if(gpsOn.isChecked()){
                //disable the pick location button and disable the altitude edit text (the altitude is provided by gps itself)
                Log.d(TAG,"GPS positioning enabled");
                pickPlace.setEnabled(false);
                alt.setEnabled(false);
                try {
                    locationManager.requestLocationUpdates(locationProvider, 0, 0, this);
                } catch(SecurityException e){
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG,"GPS positioning disabled");
                pickPlace.setEnabled(true);
                alt.setEnabled(true);
                try {
                    locationManager.removeUpdates(this);
                } catch(SecurityException e){
                    e.printStackTrace();
                }
                //clear the accuracy, in manual mode it is not needed
                accuracy.setText("");
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        //the coordinates are updated in real time into the GUI whenever the location changes
        lat.setText(String.valueOf(location.getLatitude()));
        lon.setText(String.valueOf(location.getLongitude()));
        alt.setText(String.valueOf(location.getAltitude()));

        //display the accuracy of the estimated location
        accuracy.setText("Accuracy: "+location.getAccuracy()+"m");
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Place place = PlacePicker.getPlace(getActivity(), data);
                String toastMsg = String.format("Place: %s", place.getName());
                Toast.makeText(getContext(), toastMsg, Toast.LENGTH_LONG).show();

                //set the elements of the GUI with the geo-coordinates
                lat.setText(String.valueOf(place.getLatLng().latitude));
                lon.setText(String.valueOf(place.getLatLng().longitude));
            }
        }
    }
}
