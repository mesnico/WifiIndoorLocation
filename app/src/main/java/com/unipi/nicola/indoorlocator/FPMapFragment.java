package com.unipi.nicola.indoorlocator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprintingService;
import com.unipi.nicola.indoorlocator.inertial.CalibrationData;
import com.unipi.nicola.indoorlocator.inertial.CalibrationUtils;
import com.unipi.nicola.indoorlocator.inertial.InertialPedestrianNavigationService;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * The fragment carrying the google map.
 */

public class FPMapFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener {
    public static final String TAG = "FPMapFragment";

    //messages
    public static final int MSG_STEP = 1;

    private IndoorLocatorApplication app;
    private GoogleMap gMap = null;
    private View rootView;

    //when true, the current marker is zoomed.
    private boolean updateZoom = true;

    /*
     * The messenger object that must be passed from the activity and that is needed in order for this fragment
     * to communicate with the Fingerprinting Service
     */
    private Messenger mInertialNavigationService;
    private Messenger mFingerprintingService;
    private Messenger mMessenger = new Messenger(new FPMapFragment.IncomingHandler());

    //the current shown marker
    private List<Marker> markers = new ArrayList<>();
    //the current user path
    private Polyline userPath;
    private TextView calibrationLabel;

    private TextView stepsCounter;
    int steps = 0;

    int oldVisitedLocationSize = 0;

    private Context mContext;

    public FPMapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
    }

    @Override
    public void setArguments(Bundle b) {
        //get the messenger from the activity
        mInertialNavigationService = b.getParcelable("mInertialNavigationService");
        mFingerprintingService = b.getParcelable("mFingerprintingService");

        //sends an hello message so that the service knows who he is talking to
        Utils.sendMessage(mInertialNavigationService, InertialPedestrianNavigationService.MSG_HELLO_FROM_MAP_FRAGMENT, null, mMessenger);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if(rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_fp_map, container, false);
        }

        //add listener for reset button
        Button reset = (Button)rootView.findViewById(R.id.reset_path);
        reset.setOnClickListener(this);

        //setup the calibration label
        calibrationLabel = (TextView) rootView.findViewById(R.id.calibration_label);
        calibrationLabel.setOnClickListener(this);
        updateCalibration();

        stepsCounter = (TextView) rootView.findViewById(R.id.steps_counter);

        //restore the state of buttons and text views
        if(savedInstanceState != null){
            handleRealPositioningOn();
            steps = savedInstanceState.getInt("steps");
        }

        updateSteps();
        return rootView;
    }

    private void handleRealPositioningOn(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean realPositioningEnabled = sharedPref.getBoolean("show_real_position", false);

        try {
            //enable or disable map positioning
            if(gMap != null)
                gMap.setMyLocationEnabled(realPositioningEnabled);
        } catch (SecurityException e) {
            Toast.makeText(getContext(), R.string.no_location_permissions, Toast.LENGTH_LONG);
            e.printStackTrace();
        }
    }

    private void updateCalibration(){
        CalibrationData cd = CalibrationUtils.getCalibrationInUse(getContext());
        String currentCalibrationLabel = (cd == null) ? getString(R.string.no_saved_calibration) : cd.getLabel();
        calibrationLabel.setText(currentCalibrationLabel);
    }

    private void updateSteps(){
        if(isAdded()){
            //prevent IllegalStateException calling getString on a potentially detached fragment
            stepsCounter.setText(MessageFormat.format(
                    getString(R.string.steps_count),
                    steps));
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == WifiLocatorActivity.CALIBRATION_ACTIVITY){
            Log.d(TAG, "Calibration activity finished!");
            updateCalibration();
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment)getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onStart(){
        super.onStart();
        //Registers the broadcast receiver to receive notifications about new position estimation available
        getActivity().registerReceiver(locationEstimationAvailable, new IntentFilter(
                IndoorLocatorApplication.LOCATION_ESTIMATION_READY));
        getActivity().registerReceiver(inertialPositionAvailable, new IntentFilter(
                IndoorLocatorApplication.NEW_INERTIAL_POSITION_AVAILABLE));
        app = (IndoorLocatorApplication) getActivity().getApplication();
    }

    @Override
    public void onStop(){
        super.onStop();
        getActivity().unregisterReceiver(locationEstimationAvailable);
        getActivity().unregisterReceiver(inertialPositionAvailable);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called!");

        updateZoom = true;
        updateMarkers();
        displayPath();
        handleRealPositioningOn();
        updateCalibration();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        /*gMap.clear();
        gMap = null;*/
        /*SupportMapFragment f = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (f != null)
            getFragmentManager().beginTransaction().remove(f).commitAllowingStateLoss();*/
        //userPath = null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //capture the google map object and store it in the gMap variable so that it can be used
        //outside this method
        gMap = googleMap;

        updateMarkers();
        displayPath();
        //handles the "my position" button
        handleRealPositioningOn();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.reset_path){
            //the reset path button is clicked

            //reset the view and all related data structures
            Utils.sendMessage(mInertialNavigationService,InertialPedestrianNavigationService.MSG_RESET, null, null);
            Utils.sendMessage(mFingerprintingService, WifiFingerprintingService.MSG_RESET, null, null);
            gMap.clear();
            userPath = null;
            steps = 0;
            oldVisitedLocationSize = 0;
            updateZoom = true;
            updateSteps();
        } else if(v.getId() == R.id.calibration_label){
            //show the Calibration Activity
            Intent intent = new Intent(getContext(), CalibrationActivity.class);
            startActivityForResult(intent, WifiLocatorActivity.CALIBRATION_ACTIVITY);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //save the current number of steps
        outState.putInt("steps", steps);
    }

    private final BroadcastReceiver locationEstimationAvailable = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "new location estimation ready!");
            updateMarkers();
        }
    };

    private void updateMarkers(){
        if(gMap != null && app.getEstimatedLocation()!=null){
            //only if the visited location set changed
            if(app.getVisitedLocationsSet().size() != oldVisitedLocationSize) {
                //resets the current markers
                for (Marker m : markers) {
                    m.remove();
                }
                markers.clear();
                //add the markers
                for (ComparableLocation l : app.getVisitedLocationsSet()) {
                    //add a marker on the map corresponding to the estimated position
                    double lat = l.getLatitude();
                    double lon = l.getLongitude();
                    LatLng newMarkerPos = new LatLng(lat, lon);
                    //set marker position, icon and title
                    MarkerOptions markerOpt = new MarkerOptions();
                    markerOpt.position(newMarkerPos);
                    markerOpt.icon(BitmapDescriptorFactory.fromResource(R.drawable.position_marker));
                    markerOpt.title(l.getExtras().getString("location_labels"));
                    Marker currentMarker = gMap.addMarker(markerOpt);
                    markers.add(currentMarker);
                }
                //update oldVisitedLocationSize
                oldVisitedLocationSize = app.getVisitedLocationsSet().size();
            }

            if(updateZoom) {
                //move the camera to the latest added marker
                LatLng latestMarkerPos = new LatLng(
                        app.getEstimatedLocation().getLatitude(),
                        app.getEstimatedLocation().getLongitude());
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latestMarkerPos, 20));
                updateZoom = false;
            }
        }
    }

    private void displayPath(){
        if(gMap != null && !app.getPositionsList().isEmpty()){
            Log.d(TAG, "***** list contains: "+app.getPositionsList().size()+" positions");
            List<LatLng> pathPoints = new ArrayList<>();
            for(PointF p : app.getPositionsList()){
                pathPoints.add(new LatLng(p.y, p.x));
            }
            if(userPath == null){
                PolylineOptions opt = new PolylineOptions();
                opt.width(5);
                opt.color(Color.BLUE);
                opt.addAll(pathPoints);
                opt.endCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.silver_arrow)));
                userPath = gMap.addPolyline(opt);
            }

            //redraw all the path points
            userPath.setPoints(pathPoints);
        }
    }

    private final BroadcastReceiver inertialPositionAvailable = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "new inertial position ready! "+ app.getPositionsList().get(app.getPositionsList().size()-1).y +"; "+ app.getPositionsList().get(app.getPositionsList().size()-1).x);
            displayPath();
        }
    };

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_STEP:
                    steps++;
                    updateSteps();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
