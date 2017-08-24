package com.unipi.nicola.indoorlocator;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Bundle;
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
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprint;
import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprintDBAdapter;

/**
 * Created by Nicola on 08/06/2017.
 */

public class FPLocateFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private static final String TAG = "FPLocateFragment";
    IndoorLocatorApplication app;
    /*
     * The messenger object that must be passed from the activity and that is needed in order for this fragment
     * to communicate with the Fingerprinting Service
     */
    Messenger mFingerprintingService;

    //GUI elements
    private ViewGroup rootView; //to be inflated with this fragment xml
    private ListView fingerprintsList;

    @Override
    public void setArguments(Bundle b){
        //get the messenger from the activity
        mFingerprintingService = b.getParcelable("mFingerprintingService");
    }

    @Override
    public void onStart() {
        super.onStart();
        //Registers the broadcast receiver to receive notifications about new position estimation available
        getActivity().registerReceiver(fingerprintScanAvailable, new IntentFilter(
                IndoorLocatorApplication.FINGERPRINT_SCAN_AVAILABLE));

        app = (IndoorLocatorApplication) getActivity().getApplication();
    }

    @Override
    public void onStop(){
        super.onStop();
        getActivity().unregisterReceiver(fingerprintScanAvailable);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_fp_locate, container, false);

        fingerprintsList = (ListView) rootView.findViewById(R.id.fingerprints_list);
        fingerprintsList.setOnItemClickListener(this);
        fingerprintsList.setOnItemLongClickListener(this);
        //attach the header to the fingerprints list
        ViewGroup header = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.fingerprints_list_header, fingerprintsList, false);
        fingerprintsList.addHeaderView(header);

        Button showCurrentAps = (Button) rootView.findViewById(R.id.current_aps);
        showCurrentAps.setOnClickListener(this);

        Switch locationServiceOn = (Switch) rootView.findViewById(R.id.locate_service_on);
        locationServiceOn.setOnCheckedChangeListener(this);

        return rootView;
    }

    /**
     * Method for handling items click in the fingerprints list view. The access points associated to
     * that particular fingerprint must be shown
     */
    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id){
        Log.d(TAG,"Item clicked!");
        //the list header should not be clicked
        if(v.getId()==R.id.fp_list_header) return;

        WifiFingerprint selectedFP = (WifiFingerprint) l.getItemAtPosition(position);
        WifiLocatorActivity.showFingerprintApList(getActivity(), v, new Point(0,80), selectedFP);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG,"Item long clicked!");
        if(view.getId()==R.id.fp_list_header) return true;

        final WifiFingerprint selectedFP = (WifiFingerprint) parent.getItemAtPosition(position);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Do you want to delete fingerprint \""+selectedFP.getLocationLabel()+"\"?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        WifiFingerprintDBAdapter dba = new WifiFingerprintDBAdapter(getContext());
                        dba.open(true);
                        dba.deleteFingerprintById(selectedFP.getId());
                        Log.d(TAG,"Removed fingerprint with id "+selectedFP.getId());
                        dba.close();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();

        return true;
    }

    /**
     * Method for handling long clicks in the fingerprint list view. That fingerprint is asked to
     * be deleted
     * @param v
     */


    @Override
    public void onClick(View v){
        //if the current APs button is pressed, then the access point floating view must be shown
        if(v.getId() == R.id.current_aps){
            if(app.getCurrentFingerprint() != null) {
                WifiLocatorActivity.showFingerprintApList(getActivity(), v, new Point(0, 80), app.getCurrentFingerprint());
            } else {
                //no current aps detected
                String toastMsg = "No APs detected recently";
                Toast.makeText(getContext(), toastMsg, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
        if(isChecked){
            Log.d(TAG, "Location service ON!");
        } else {
            Log.d(TAG, "Location service OFF!");
        }

        Bundle b = new Bundle();
        b.putBoolean("locate_onoff", isChecked);

        Utils.sendMessage(mFingerprintingService, WifiFingerprintingService.MSG_LOCATE_ONOFF, b, null);
    }

    private void updateFingerprintList() {
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
    private final BroadcastReceiver fingerprintScanAvailable = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.d(TAG, "New wifi fingerprint scan is available!");
            updateFingerprintList();
        }
    };
}
