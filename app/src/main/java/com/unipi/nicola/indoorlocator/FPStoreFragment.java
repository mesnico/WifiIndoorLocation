package com.unipi.nicola.indoorlocator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
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

    /*
     * The messenger object that must be passed from the activity and that is needed in order for this fragment
     * to communicate with the Fingerprinting Service
     */
    Messenger mService;

    EditText lat;
    EditText lon;
    EditText alt;
    Button pickPlace;
    Button store;
    CheckBox gpsOn;
    TextView accuracy;

    Location location; //the location to be filled both by GPS or place picker

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
        alt.addTextChangedListener(altitudeTextWatcher);
        pickPlace = (Button) rootView.findViewById(R.id.pick_location);
        pickPlace.setOnClickListener(this);
        gpsOn = (CheckBox) rootView.findViewById(R.id.gps_on);
        gpsOn.setOnClickListener(this);
        accuracy = (TextView) rootView.findViewById(R.id.gps_accuracy);
        store = (Button) rootView.findViewById(R.id.store);
        store.setOnClickListener(this);

        possiblyEnableStoreButton();

        return rootView;
    }

    private final TextWatcher altitudeTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            possiblyEnableStoreButton();
            Log.d(TAG, "altitude changed!");
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

    @Override
    public void setArguments(Bundle b){
        //get the messenger from the activity
        mService = b.getParcelable("mService");
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
                //but first, if the location is off, ask the user if it can be turned on
                final LocationManager manager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    buildAlertMessageNoGps();
                }

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
        if(v.getId() == R.id.store){
            //handles the storing of the current wifi measurement
            Log.d(TAG, "Store button pressed!");

            //refresh the altitude since it could be modified manually by the user
            location.setAltitude(Double.valueOf(alt.getText().toString()));
            String locationLabel = ((EditText) rootView.findViewById(R.id.location_label)).getText().toString();
            Bundle b = new Bundle();
            b.putString("current_location_label", locationLabel);
            b.putParcelable("current_location", location);

            //send request to the service
            Message msg = Message.obtain(null, WifiFingerprintingService.MSG_STORE_FINGERPRINT);
            msg.setData(b);
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void buildAlertMessageNoGps(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        gpsOn.setChecked(false);
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void possiblyEnableStoreButton(){
        //The store button can be enabled only if latitude, longitude and altitude have been set
        if(lat.length()!=0 && lon.length()!=0 && alt.length()!=0){
            store.setEnabled(true);
        } else {
            store.setEnabled(false);
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

        possiblyEnableStoreButton();

        this.location = location;
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

                location = new Location("place picker");
                location.setLatitude(place.getLatLng().latitude);
                location.setLongitude(place.getLatLng().longitude);

                possiblyEnableStoreButton();
            }
        }
    }
}
