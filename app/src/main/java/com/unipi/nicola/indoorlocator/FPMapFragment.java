package com.unipi.nicola.indoorlocator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.google.android.gms.maps.model.RoundCap;

import java.util.ArrayList;
import java.util.List;

/**
 * The fragment carrying the google map.
 */
public class FPMapFragment extends Fragment implements OnMapReadyCallback  {
    public static final String TAG = "FPMapFragment";
    private IndoorLocatorApplication app;
    private GoogleMap gMap = null;
    private View rootView;

    //the current shown marker
    private Marker currentMarker;
    //the current user path
    private Polyline userPath;

    public FPMapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if(rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_fp_map, container, false);
        }
        return rootView;
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
    public void onDestroyView() {
        super.onDestroyView();
        gMap = null;
        /*SupportMapFragment f = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (f != null)
            getFragmentManager().beginTransaction().remove(f).commitAllowingStateLoss();*/
        userPath = null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //capture the google map object and store it in the gMap variable so that it can be used
        //outside this method
        gMap = googleMap;
    }

    private final BroadcastReceiver locationEstimationAvailable = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "new location estimation ready!");
            if(gMap != null){
                //delete the previous marker, if one
                if(currentMarker != null) {
                    currentMarker.remove();
                }
                //add a marker on the map corresponding to the estimated position
                double lat = app.getEstimatedLocation().getLatitude();
                double lon = app.getEstimatedLocation().getLongitude();
                LatLng newMarkerPos = new LatLng(lat, lon);
                currentMarker = gMap.addMarker(new MarkerOptions().position(newMarkerPos));
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newMarkerPos,20));
            }
        }
    };

    private final BroadcastReceiver inertialPositionAvailable = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "new inertial position ready! "+ app.getPositionsList().get(app.getPositionsList().size()-1).y +"; "+ app.getPositionsList().get(app.getPositionsList().size()-1).x);
            if(gMap != null){
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
    };
}
