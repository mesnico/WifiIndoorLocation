package com.unipi.nicola.indoorlocator;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprint;

import org.w3c.dom.Text;

/**
 * Created by Nicola on 08/06/2017.
 */

public class FPLocateFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "FPLocateFragment";

    /*
     * The messenger object that must be passed from the activity and that is needed in order for this fragment
     * to communicate with the Fingerprinting Service
     */
    Messenger mService;

    //GUI elements
    private ViewGroup rootView; //to be inflated with this fragment xml
    private ListView fingerprintsList;
    private View apFrame; //to be inflated with xml when a fingerprint is selected

    @Override
    public void setArguments(Bundle b){
        //get the messenger from the activity
        mService = b.getParcelable("mService");
    }

    @Override
    public void onStart() {
        super.onStart();
        //Registers the broadcast receiver to receive notifications about new position estimation available
        getActivity().registerReceiver(locationEstimationAvailable, new IntentFilter(
                IndoorLocatorApplication.LOCATION_ESTIMATION_READY));
    }

    @Override
    public void onPause(){
        super.onPause();
        getActivity().unregisterReceiver(locationEstimationAvailable);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_fp_locate, container, false);

        fingerprintsList = (ListView) rootView.findViewById(R.id.fingerprints_list);
        fingerprintsList.setOnItemClickListener(this);
        //attach the header to the fingerprints list
        ViewGroup header = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.fingerprints_list_header, fingerprintsList, false);
        fingerprintsList.addHeaderView(header);
        return rootView;
    }

    /**
     * Method for handling items click in the fingerprints list view. The access points associated to
     * that particular fingerprint must be shown
     */
    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id){
        Log.d(TAG,"Item clicked!");
        //if an ap list was present, then remove it before creating the new one
        if(apFrame != null){
            rootView.removeView(apFrame);
        }
        WifiFingerprint selectedFP = (WifiFingerprint) l.getItemAtPosition(position);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        apFrame = inflater.inflate(R.layout.access_points_floating_view, rootView, false);

        //if the close button is clicked, then the apframe must be destroyed
        Button closeBtn = (Button)apFrame.findViewById(R.id.close_btn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "AP frame close button clicked");
                rootView.removeView(apFrame);
            }
        });
        //get the text view to be filled with infos from the current selection
        TextView apLocationLabel = (TextView)apFrame.findViewById(R.id.apview_location_label);
        apLocationLabel.setText(selectedFP.getLocationLabel());

        //fill the list view with the access points of the selected fingerprint
        AccessPointsListAdapter adapter = new AccessPointsListAdapter(
                getActivity(),
                selectedFP.getAccessPoints()
        );
        // attach the adapter to the ListView
        ListView aplist = (ListView)apFrame.findViewById(R.id.ap_list_view);
        aplist.setAdapter(adapter);

        rootView.addView(apFrame);
    }

    private void updateFingerprintList() {
        IndoorLocatorApplication app = (IndoorLocatorApplication) getActivity().getApplication();
        FingerprintsListAdapter adapter = new FingerprintsListAdapter(
                getActivity(),
                app.getkBestFingerprints()
        );
        // attach the adapter to a ListView
        fingerprintsList.setAdapter(adapter);

        //fill the best match text view
        TextView bestMatch = (TextView) rootView.findViewById(R.id.best_match);
        if (app.getkBestFingerprints().isEmpty()) {
            bestMatch.setText("NONE");
        } else {
            bestMatch.setText(app.getkBestFingerprints().get(0).getLocationLabel());
        }
    }

    /**
     * Handler for broadcast notification telling a new wifi fingerprint position estimation is available
     */
    private final BroadcastReceiver locationEstimationAvailable = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.d(TAG, "New location estimation is available!");
            updateFingerprintList();
        }
    };
}
