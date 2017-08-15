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
import android.util.Log;

import com.unipi.nicola.indoorlocator.fingerprinting.AccessPointInfos;
import com.unipi.nicola.indoorlocator.fingerprinting.FingerprintDistance;
import com.unipi.nicola.indoorlocator.fingerprinting.FingerprintEuclidDistance;
import com.unipi.nicola.indoorlocator.fingerprinting.WifiAPsAggregator;
import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprint;
import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprintDBAdapter;

import java.util.ArrayList;
import java.util.List;

public class WifiFingerprintingService extends Service {
    static final String TAG = "FingerprintingService";

    //maximum number of returned fingerprints after ordering
    static final int MAX_FINGERPRINTS = 10;

    static final int MSG_STORE_FINGERPRINT =                    1;
    static final int MSG_PARAMETERS_CHANGED =                   2;
    static final int MSG_LOCATE_ONOFF =                            3;

    private WifiManager wifi;

    //DB adapter for the SQLite DB storing the fingerprints
    private WifiFingerprintDBAdapter dba;

    //counts how many samples need to be acquired when a storing request is received
    int storingCounter = -1;

    //The messenger object that must be passed to the activity in order to contact this service
    private Messenger mMessenger = new Messenger(new IncomingHandler());

    boolean signalPowerNormalization = false;
    int minMatchingAPs = 1;
    int numberOfNearestNeighbors;
    double maximumDistance;
    boolean active = true;  //determine if the service is performing wifi scanning in order to estimate positions

    WifiAPsAggregator aggregator;
    Location currentLocation;
    String currentLocationLabel;

    public WifiFingerprintingService() {
    }

    @Override
    public void onCreate() {
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
        List<AccessPointInfos> aps1 = new ArrayList<>();
        aps1.add(new AccessPointInfos(-20, "00:18:4d:cf:ca:26", "net1"));
        aps1.add(new AccessPointInfos(-88, "c0:ff:d4:a2:cf:da", "net2"));
        WifiFingerprint fp1 = new WifiFingerprint(aps1,new Location("WifiLocator"), "piano_sopra");
        dba.insertFingerprint(fp1);
        List<AccessPointInfos> aps2 = new ArrayList<>();
        aps2.add(new AccessPointInfos(-88, "00:18:4d:cf:ca:26", "net1"));
        aps2.add(new AccessPointInfos(-20, "c0:ff:d4:a2:cf:da", "net2"));
        WifiFingerprint fp2 = new WifiFingerprint(aps2,new Location("WifiLocator"), "piano_sotto");
        dba.insertFingerprint(fp2);

        //checks if that fingerprint 11:11:11:11:11:11 gives me "stanza1"
        /*List<AccessPointInfos> sensedAps = new ArrayList<>();
        sensedAps.add(new AccessPointInfos(-20, "11:11:11:11:11:11", "net1"));
        List<WifiFingerprint> retFP = dba.extractCommonFingerprints(new WifiFingerprint(sensedAps,null,null), 1);*/
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

            //if storingCounter is 0, then we accumulated enough scans; the resulting aggregated APs
            //can be stored in the database
            if(storingCounter == 0){
                WifiFingerprint aggregatedFP = new WifiFingerprint(
                        aggregator.returnAggregatedAPs(), currentLocation, currentLocationLabel);
                dba.insertFingerprint(aggregatedFP);
                //if initially, before the storing procedure, the service was inactive, then I deregister again the
                //wifi scan receiver
                if(!active) {
                    getBaseContext().unregisterReceiver(wifiScanAvailableReceiver);
                }
                Log.d(TAG, "new fingerprint stored! LocationLabel: "+currentLocationLabel);
                storingCounter = -1;
            }

            if(storingCounter>0){
                /* we are in storing mode, so this beacons must be elaborated and then put into the
                 * database
                 */
                aggregator.insertMeasurement(apinfos);
                Log.d(TAG, storingCounter + " iterations left for storing "+currentLocationLabel);
                storingCounter--;

            } else {
                /* matching mode: the APs sensed must be matched against fingerprints found in the
                 * database
                 */

                //the current wifi APs set that must be compared with the retrieved FPs in the DB
                WifiFingerprint currentMeasure = new WifiFingerprint(apinfos, new Location("test"), "Just here");

                //search in DB fingerprints having at least "minMatchingAPs" overlapping BSSID
                List<WifiFingerprint> foundFP = dba.extractCommonFingerprints(currentMeasure, minMatchingAPs);

                FingerprintDistance stdEuclid = new FingerprintEuclidDistance(signalPowerNormalization);
                List<WifiFingerprint> orderedResults = stdEuclid.computeNearestFingerprints(foundFP,currentMeasure, MAX_FINGERPRINTS);

                //computes the centroid using a filtered set of fingerprints, in order to estimate the location
                List<WifiFingerprint> filtered = stdEuclid.filterComputedNearestFingerprints(maximumDistance, numberOfNearestNeighbors);

                //TODO: computeCentroid() and return the results to the activity
                //Location = computeCentroid(filtered);

                IndoorLocatorApplication mApp = (IndoorLocatorApplication) getApplication();
                mApp.setkBestFingerprints(orderedResults);
                //set also the current sensed fingerprint
                mApp.setCurrentFingerprint(currentMeasure);

                //notifies the application so that it can retrieve the so built list of fingerprints
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(IndoorLocatorApplication.LOCATION_ESTIMATION_READY);
                sendBroadcast(broadcastIntent);
            }
            if (wifi.isWifiEnabled()) {
                wifi.startScan();
            }
        }
    };

    /*
     * Handler for requests coming from the activity
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            switch (msg.what) {
                case MSG_STORE_FINGERPRINT:
                    //initialize the counter and the new aggregator
                    storingCounter = 1;
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
