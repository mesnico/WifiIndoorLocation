package com.unipi.nicola.indoorlocator;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.text.TextUtils;
import android.util.Log;

import com.unipi.nicola.indoorlocator.fingerprinting.AccessPointInfos;
import com.unipi.nicola.indoorlocator.fingerprinting.FingerprintDistance;
import com.unipi.nicola.indoorlocator.fingerprinting.FingerprintEuclidDistance;
import com.unipi.nicola.indoorlocator.fingerprinting.WifiAPsAggregator;
import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprint;
import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprintDBAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class WifiFingerprintingService extends Service {
    static final String TAG = "FingerprintingService";

    //maximum number of returned fingerprints after ordering
    static final int MAX_FINGERPRINTS = 10;

    static final int MSG_HELLO_FROM_STORE_FRAGMENT =                                1;
    static final int MSG_STORE_FINGERPRINT =                    2;
    static final int MSG_PARAMETERS_CHANGED =                   3;
    static final int MSG_LOCATE_ONOFF =                         4;

    private WifiManager wifi;

    //DB adapter for the SQLite DB storing the fingerprints
    private WifiFingerprintDBAdapter dba;

    //counts how many samples need to be acquired when a storing request is received
    int storingCounter = -1;

    //The messenger object that must be passed to the activity in order to contact this service
    private Messenger mMessenger = new Messenger(new IncomingHandler());

    //The messenger object through which this service can send messages to the fp store fragment
    private Messenger mStoreFragment;

    boolean signalPowerNormalization = false;
    int minMatchingAPs = 1;
    int numberOfNearestNeighbors;
    double maximumDistance;
    boolean active = true;  //determine if the service is performing wifi scanning in order to estimate positions

    int storeIterations = 3;

    WifiAPsAggregator aggregator;
    Location currentLocation;
    String currentLocationLabel;

    public WifiFingerprintingService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "***************** Service created!");
        //register for broadcast receivers (wifi state and new scan available)
        this.registerReceiver(wifiStateChangedReceiver, new IntentFilter(
                WifiManager.WIFI_STATE_CHANGED_ACTION));
        this.registerReceiver(wifiScanAvailableReceiver, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        //TODO: Does startScan() work also if wifi is not enabled? It would be nice
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //if (wifi.isWifiEnabled()) {
            wifi.startScan();
        //}

        dba = new WifiFingerprintDBAdapter(this);
        dba.open(true);

        //TODO: the database at the moment is in-memory, so at the startup the following entries are reentered, as of now. Instead, those values should be permanent and stored using the STORE functionality
        //inserts a fingerprint
        /*List<AccessPointInfos> aps1 = new ArrayList<>();
        aps1.add(new AccessPointInfos(-20, "00:18:4d:cf:ca:26", "net1"));
        aps1.add(new AccessPointInfos(-88, "c0:ff:d4:a2:cf:da", "net2"));
        WifiFingerprint fp1 = new WifiFingerprint(aps1,new Location("WifiLocator"), "piano_sopra");
        dba.insertFingerprint(fp1);
        List<AccessPointInfos> aps2 = new ArrayList<>();
        aps2.add(new AccessPointInfos(-88, "00:18:4d:cf:ca:26", "net1"));
        aps2.add(new AccessPointInfos(-20, "c0:ff:d4:a2:cf:da", "net2"));
        WifiFingerprint fp2 = new WifiFingerprint(aps2,new Location("WifiLocator"), "piano_sotto");
        dba.insertFingerprint(fp2);*/
    }

    /* When an activity binds to this service, the messenger object to communicate with it is returned
     * to the activity.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind done");
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        unregisterReceiver(wifiStateChangedReceiver);
        if(active) {
            unregisterReceiver(wifiScanAvailableReceiver);
        }
        //closes the DB
        dba.close();
    }

    private final transient BroadcastReceiver wifiStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            int extraWifiState = intent.getIntExtra(
                    WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN);

            switch (extraWifiState) {
                case WifiManager.WIFI_STATE_DISABLED:
                case WifiManager.WIFI_STATE_ENABLED:
            }
        }
    };

    private final transient BroadcastReceiver wifiScanAvailableReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.i(TAG,"------------------- New scan finished");

            //creates a list of accesspointinfos from the list of scanresults.
            List<AccessPointInfos> apinfos = new ArrayList<>();
            for(ScanResult s : wifi.getScanResults()){
                apinfos.add(new AccessPointInfos(
                        s.level,
                        s.BSSID,
                        s.SSID
                ));
            }

            if(storingCounter <= storeIterations && storingCounter >= 0){
                /* we are in storing mode, so this beacons must be elaborated and then put into the
                 * database
                 */
                Bundle b = new Bundle();

                //during every iteration, use the aggregator to merge the scans into one
                aggregator.insertMeasurement(apinfos);
                Log.d(TAG, (storeIterations - storingCounter) + " iterations left for storing " + currentLocationLabel);
                Log.d(TAG, "current measurement: "+apinfos.toString());
                storingCounter++;
                b.putInt("current_iteration", storingCounter);
                b.putInt("total_iterations", storeIterations);

                //if storingCounter equals storeIterations, then we accumulated enough scans; the resulting aggregated APs
                //can be stored in the database
                if(storingCounter == storeIterations){
                    WifiFingerprint aggregatedFP = new WifiFingerprint(
                            aggregator.returnAggregatedAPs(), currentLocation, currentLocationLabel);

                    //stores the aggregated value in the db and send the result to the store fragment,
                    //only if fingerprint is not empty, otherwise it is useless
                    if(!aggregatedFP.getAccessPoints().isEmpty()) {
                        dba.insertFingerprint(aggregatedFP);
                        b.putSerializable("aggregated_fp", aggregatedFP);
                    }
                    //if initially, before the storing procedure, the service was inactive, then I deregister again the
                    //wifi scan receiver
                    if(!active) {
                        getBaseContext().unregisterReceiver(wifiScanAvailableReceiver);
                    }
                    Log.d(TAG, "new fingerprint stored! LocationLabel: "+currentLocationLabel+"; APs: "+aggregator.returnAggregatedAPs().toString());
                    storingCounter = -1;
                }

                //send notification to the fp fragment
                Utils.sendMessage(mStoreFragment, FPStoreFragment.MSG_NEXT_STORING_ITERATION, b, null);
            }

            /* in any case, matching mode is enabled (storingCounter = -1): the APs sensed must be matched against
             * fingerprints found in the database
             */
            //the current wifi APs set that must be compared with the retrieved FPs in the DB
            WifiFingerprint currentMeasure = new WifiFingerprint(apinfos, new Location("test"), "Just here");

            //search in DB fingerprints having at least "minMatchingAPs" overlapping BSSID
            List<WifiFingerprint> foundFP = dba.extractCommonFingerprints(currentMeasure, minMatchingAPs);

            FingerprintDistance stdEuclid = new FingerprintEuclidDistance(signalPowerNormalization);
            List<WifiFingerprint> orderedResults = stdEuclid.computeNearestFingerprints(foundFP,currentMeasure, MAX_FINGERPRINTS);

            //computes the centroid using a filtered set of fingerprints, in order to estimate the location
            List<WifiFingerprint> filtered = stdEuclid.filterComputedNearestFingerprints(maximumDistance, numberOfNearestNeighbors);

            IndoorLocatorApplication mApp = (IndoorLocatorApplication) getApplication();
            if(!filtered.isEmpty()) {
                Location l = computeCentroid(filtered);
                mApp.setEstimatedLocation(l);

                //notifies the application so that it can retrieve the new location
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(IndoorLocatorApplication.LOCATION_ESTIMATION_READY);
                sendBroadcast(broadcastIntent);
            }

            mApp.setkBestFingerprints(orderedResults);
            //set also the current sensed fingerprint
            mApp.setCurrentFingerprint(currentMeasure);

            //notifies the application so that it can retrieve the so built list of fingerprints
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(IndoorLocatorApplication.FINGERPRINT_SCAN_AVAILABLE);
            sendBroadcast(broadcastIntent);
            if (wifi.isWifiEnabled()) {
                wifi.startScan();
            }
        }
    };

    private Location computeCentroid(List<WifiFingerprint> fps){
        double centroidLat=0, centroidLon=0, centroidAlt=0;
        String concatenatedIds = "";
        Set<Integer> idSet = new TreeSet<>();
        String concatenatedLocationLabels;
        List<String> locationLabelsList = new ArrayList<>();
        for(WifiFingerprint f : fps){
            Location fpLocation = f.getLocation();
            centroidLat += fpLocation.getLatitude();
            centroidLon += fpLocation.getLongitude();
            centroidAlt += fpLocation.getAltitude();
            //add the identifier to the set so that an unique index for this location can be built
            idSet.add(f.getId());
            //add the location label to the list so that it can be inserted into the location we are building
            locationLabelsList.add(f.getLocationLabel());
        }
        Location l = new Location("wifi_centroid");
        l.setLatitude(centroidLat / fps.size());
        l.setLongitude(centroidLon / fps.size());
        l.setAltitude(centroidAlt / fps.size());

        //calculate the id as concatenation of ordered ids
        for(int i : idSet){
            concatenatedIds += i;
        }
        //compute the location label string concatenation
        concatenatedLocationLabels = TextUtils.join("-", locationLabelsList);
        //put the identifier and the concatenated labels into the location through a string bundle
        Bundle b = new Bundle();
        b.putString("id",concatenatedIds);
        b.putString("location_labels",concatenatedLocationLabels);
        l.setExtras(b);
        return l;
    }

    /*
     * Handler for requests coming from the activity
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            switch (msg.what) {
                case MSG_HELLO_FROM_STORE_FRAGMENT:
                    //store the interlocutor
                    mStoreFragment = msg.replyTo;
                    Log.d(TAG, "Hello message received from store fragment!");
                    break;

                case MSG_STORE_FINGERPRINT:
                    //initialize the counter and the new aggregator
                    storingCounter = 0;
                    aggregator = new WifiAPsAggregator();
                    //receive the current location
                    currentLocation = b.getParcelable("current_location");
                    currentLocationLabel = b.getString("current_location_label");
                    //enable wifi scan even if the user turned off the locating service
                    if(!active){
                        getBaseContext().registerReceiver(wifiScanAvailableReceiver, new IntentFilter(
                                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                    }
                    Log.d(TAG, "Store fingerprint request received!");
                    break;

                case MSG_PARAMETERS_CHANGED:
                    signalPowerNormalization = b.getBoolean("signal_normalization");
                    if(signalPowerNormalization){
                        Log.d(TAG, "Signal power normalization ON");
                    } else {
                        Log.d(TAG, "Signal power normalization OFF");
                    }
                    minMatchingAPs = b.getInt("min_matching_aps");
                    Log.d(TAG, "Minimum number of matching APs: "+minMatchingAPs);

                    numberOfNearestNeighbors = b.getInt("nn_number");
                    Log.d(TAG, "Number of nearest neighbors: "+numberOfNearestNeighbors);

                    maximumDistance = b.getDouble("distance_threshold");
                    Log.d(TAG, "Maximum distance: "+maximumDistance);

                    storeIterations = b.getInt("storing_iterations");
                    Log.d(TAG, "Store iterations: "+storeIterations);
                    break;

                case MSG_LOCATE_ONOFF:
                    //if not active, unregister receiver so that wifi scan stops
                    active = b.getBoolean("locate_onoff");
                    if(active){
                        getBaseContext().registerReceiver(wifiScanAvailableReceiver, new IntentFilter(
                                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                        //TODO: if scan in a future will be timeout driven, then this must be changed
                        wifi.startScan();
                    } else {
                        getBaseContext().unregisterReceiver(wifiScanAvailableReceiver);
                    }
                    break;

                default:
                    super.handleMessage(msg);
            }

        }
    }
}
