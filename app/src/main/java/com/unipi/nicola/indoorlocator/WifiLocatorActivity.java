package com.unipi.nicola.indoorlocator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprint;

public class WifiLocatorActivity extends AppCompatActivity {

    //Messenger for communicating with the fingerprinting service and inertial navigation service
    Messenger mFingerprintingService = null;
    Messenger mInertialNavigationService = null;

    //The fragment actually loaded
    Fragment actualFragment = null;
    //Reference to each individual fragment
    Fragment mapFragment;
    Fragment storeFragment;
    Fragment locateFragment;

    //Services started by this activity
    Intent locatorService;
    Intent inertialNavigationService;

    private final String TAG = "WifiLocatorActivity";


    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate called!");
        setContentView(R.layout.activity_wifi_locator);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //restore the previously saved fragments
        if(savedInstanceState != null){
            mapFragment = getSupportFragmentManager().getFragment(savedInstanceState, "mapFragment");
            storeFragment = getSupportFragmentManager().getFragment(savedInstanceState, "storeFragment");
            locateFragment = getSupportFragmentManager().getFragment(savedInstanceState, "locateFragment");
        }

        //Starts the Fingerprinting Service
        locatorService = new Intent(this, WifiFingerprintingService.class);
        startService(locatorService);
        Log.d(TAG, "Locator service should be started!");

        // Bind to the Fingerprinting Service
        bindService(new Intent(this, WifiFingerprintingService.class), mFingerprintingServiceConnection,
                Context.BIND_AUTO_CREATE);

        //Starts the Inertial Navigation Service only if there are the right sensors
        SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor rotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Sensor accelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(rotationSensor != null && accelerometerSensor != null){
            Log.d(TAG, "Starting Inertial navigation service");

            //Starts the Inertial Navigation Service
            inertialNavigationService = new Intent(this, InertialPedestrianNavigationService.class);
            startService(inertialNavigationService);

            // Bind to the Inertial Navigation Service
            bindService(new Intent(this, InertialPedestrianNavigationService.class), mInertialServiceConnection,
                    Context.BIND_AUTO_CREATE);
        } else {
            buildNoSensorsAlertMessage();
        }

        //If wifi disabled, ask the user if it can be enabled
        final WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!manager.isWifiEnabled()) {
            buildAlertMessageNoWifi();
        }
    }

    private void buildNoSensorsAlertMessage(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("There aren't the right sensors to perform inertial navigation. It will be disabled")
                .setCancelable(false)
                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void buildAlertMessageNoWifi() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Wifi is needed for this application to work. Do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
                        wifiManager.setWifiEnabled(true);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mFingerprintingService != null) {
            unbindService(mFingerprintingServiceConnection);
        }
        if(mInertialNavigationService != null){
            unbindService(mInertialServiceConnection);
        }


        //stop the services
        if(inertialNavigationService != null){
            stopService(inertialNavigationService);
        }
        if(locatorService != null){
            stopService(locatorService);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //save fragments
        if(mapFragment != null) getSupportFragmentManager().putFragment(outState, "mapFragment", mapFragment);
        if(storeFragment != null) getSupportFragmentManager().putFragment(outState, "storeFragment", storeFragment);
        if(locateFragment != null) getSupportFragmentManager().putFragment(outState, "locateFragment", locateFragment);
    }

    /*@Override
    public void onResume(){
        super.onResume();
        if(mBound){
            updatePreferenceValues();
        }
    }*/


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wifi_locator, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            //show the Settings Activity
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent,17930);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Through this callback we can know when the settings activity is being closed, so that we can
    //retrieve the settings and send them to the proper services in order to be updated
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 17930){
            Log.d(TAG, "Settings activity finished!");
            updatePreferenceValues();
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch(position) {
                case 0:
                    if (mapFragment == null) {
                        mapFragment = new FPMapFragment();
                    }
                    actualFragment = mapFragment;
                    break;
                case 1:
                    if (locateFragment == null){
                        locateFragment = new FPLocateFragment();
                    }
                    actualFragment = locateFragment;
                    break;
                case 2:
                    if(storeFragment == null) {
                        storeFragment = new FPStoreFragment();
                    }
                    actualFragment = storeFragment;
                    break;
            }

            tryInjectingMessengersIntoFragments();

            return actualFragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "MAP";
                case 1:
                    return "LOCATE";
                case 2:
                    return "STORE";
            }
            return null;
        }
    }

    //try to inject the service messengers objects into the fragments
    private void tryInjectingMessengersIntoFragments(){
        Bundle b = new Bundle();
        if(mFingerprintingService !=null){
            b.putParcelable("mFingerprintingService", mFingerprintingService);
        }
        if(mInertialNavigationService !=null){
            b.putParcelable("mInertialNavigationService", mInertialNavigationService);
        }

        if(mapFragment!=null){
            mapFragment.setArguments(b);
        }
        if(storeFragment!=null){
            storeFragment.setArguments(b);
        }
        if(locateFragment!=null){
            locateFragment.setArguments(b);
        }
    }

    private void updatePreferenceValues(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Bundle b;

        if(mFingerprintingService != null) {
            //matching preferences. They are sent to the fingerprinting service

            //signal normalization preference
            Boolean normalizeSignal = sharedPref.getBoolean(SettingsActivity.PREF_SIGNAL_NORMALIZATION_KEY, false);
            b = new Bundle();

            b.putBoolean("signal_normalization", normalizeSignal);

            //minimum number of matching APs preference
            int minMatchingAPs = Integer.valueOf(sharedPref.getString(SettingsActivity.PREF_MINIMUM_MATCHING_APS_KEY, "1"));
            b.putInt("min_matching_aps", minMatchingAPs);

            //distance threshold preference
            double distanceThreshold = Double.valueOf(sharedPref.getString(SettingsActivity.PREF_DISTANCE_THRESHOLD_KEY, "10"));
            b.putDouble("distance_threshold", distanceThreshold);

            //number of nearest neighbors preference
            int numberOfNearestNeighbors = Integer.valueOf(sharedPref.getString(SettingsActivity.PREF_NEAREST_NEIGHBORS_NUMBER_KEY, "3"));
            b.putInt("nn_number", numberOfNearestNeighbors);

            //storing preferences
            int storingIterations = Integer.valueOf(sharedPref.getString(SettingsActivity.PREF_STORING_ITERATIONS_KEY, "4"));
            b.putInt("storing_iterations", storingIterations);

            Utils.sendMessage(mFingerprintingService, WifiFingerprintingService.MSG_PARAMETERS_CHANGED, b, null);
        }

        if(mInertialNavigationService != null) {
            //send inertial navigation settings to inertial navigation service
            b = new Bundle();

            float beta = Float.valueOf(sharedPref.getString(SettingsActivity.PREF_BETA, "0.5"));
            b.putFloat("beta", beta);

            float stepLength = Float.valueOf(sharedPref.getString(SettingsActivity.PREF_STEP_LENGTH, "0.74"));
            b.putFloat("step_length", stepLength);

            int updateAfterNumSteps = Integer.valueOf(sharedPref.getString(SettingsActivity.PREF_UPDATE_AFTER_NUM_STEPS, "3"));
            b.putInt("update_after_num_steps", updateAfterNumSteps);

            Utils.sendMessage(mInertialNavigationService, InertialPedestrianNavigationService.MSG_PARAMETERS_CHANGED, b, null);
        }
    }

    /**
     * Static method used to display an access point list carried by a given wifi fingerprint
     */
    public static void showFingerprintApList(Activity context, View popupOriginator, Point offset, WifiFingerprint fingerprint){
        View apPopup;
        final PopupWindow pw;

        LinearLayout parentView = (LinearLayout) context.findViewById(R.id.aps_popup);
        LayoutInflater inflater = LayoutInflater.from(context);
        apPopup = inflater.inflate(R.layout.access_points_popup_layout, parentView, false);

        //get the text view to be filled with infos from the current selection
        TextView apLocationLabel = (TextView)apPopup.findViewById(R.id.apview_location_label);
        apLocationLabel.setText(fingerprint.getLocationLabel());

        //fill the list view with the access points of the selected fingerprint
        AccessPointsListAdapter adapter = new AccessPointsListAdapter(
                context,
                fingerprint.getAccessPoints()
        );
        // attach the adapter to the ListView
        ListView apList = (ListView)apPopup.findViewById(R.id.ap_list_view);
        apList.setAdapter(adapter);

        // create the popup window
        pw = new PopupWindow(apPopup, LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true);

        // display the popup in the position of the popup originator
        int[] location = new int[2];
        popupOriginator.getLocationOnScreen(location);
        pw.showAtLocation(apPopup, Gravity.NO_GRAVITY, location[0]+offset.x, location[1]+offset.y);

        //if the close button is clicked, then the apPopup must be destroyed
        Button closeBtn = (Button)apPopup.findViewById(R.id.close_btn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pw.dismiss();
            }
        });

    }

    /**
     * Class for interacting with the main interface of the fingerprinting service.
     */
    private ServiceConnection mFingerprintingServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service
            mFingerprintingService = new Messenger(service);

            tryInjectingMessengersIntoFragments();

            // sends the values of the preferences to the fingerprinting service
            updatePreferenceValues();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mFingerprintingService = null;
        }
    };

    /**
     * Class for interacting with the main interface of the inertial navigation service.
     */
    private ServiceConnection mInertialServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service
            mInertialNavigationService = new Messenger(service);

            tryInjectingMessengersIntoFragments();

            // sends the values of the preferences to the fingerprinting service
            updatePreferenceValues();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mInertialNavigationService = null;
        }
    };
}
