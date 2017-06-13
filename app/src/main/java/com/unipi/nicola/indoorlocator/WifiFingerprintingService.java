package com.unipi.nicola.indoorlocator;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.unipi.nicola.indoorlocator.fingerprinting.AccessPointInfos;
import com.unipi.nicola.indoorlocator.fingerprinting.FingerprintDistance;
import com.unipi.nicola.indoorlocator.fingerprinting.FingerprintEuclidDistance;
import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprint;
import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprintDBAdapter;

import java.util.ArrayList;
import java.util.List;

public class WifiFingerprintingService extends Service {
    static final String TAG = "FingerprintingService";
    static final int MSG_STORE_FINGERPRINT =                1;
    static final int MSG_SIGNAL_POWER_NORMALIZATION_ON =    2;
    static final int MSG_SIGNAL_POWER_NORMALIZATION_OFF =   3;

    private WifiManager wifi;

    //DB adapter for the SQLite DB storing the fingerprints
    private WifiFingerprintDBAdapter dba;

    //counts how many samples need to be acquired when a storing request is received
    int storingCounter = 0;

    //The messenger object that must be passed to the activity in order to contact this service
    private Messenger mMessenger = new Messenger(new IncomingHandler());

    boolean signalPowerNormalization = false;

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
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled()) {
            wifi.startScan();
        }

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
            if(storingCounter>0){
                /* we are in storing mode, so this beacons must be elaborated and then put into the
                 * database
                 */
            } else {
                /* matching mode: the APs sensed must be matched against fingerprints found in the
                 * database
                 */

                //creates a list of accesspointinfos from the list of scanresults.
                List<AccessPointInfos> apinfos = new ArrayList<>();
                for(ScanResult s : wifi.getScanResults()){
                    apinfos.add(new AccessPointInfos(
                            s.level,
                            s.BSSID,
                            s.SSID
                    ));
                }

                //the current wifi APs set that must be compared with the retrieved FPs in the DB
                WifiFingerprint currentMeasure = new WifiFingerprint(apinfos, new Location("test"), "testRoom");

                //search in DB fingerprints having at least 1 overlapping BSSID
                List<WifiFingerprint> foundFP = dba.extractCommonFingerprints(currentMeasure, 1);

                FingerprintDistance stdEuclid = new FingerprintEuclidDistance(signalPowerNormalization);

                //TODO: the 5 should be a variable parameter set by the user
                List<WifiFingerprint> orderedResults = stdEuclid.computeNearestFingerprints(foundFP,currentMeasure, 5);

                IndoorLocatorApplication mApp = (IndoorLocatorApplication) getApplication();
                mApp.setkBestFingerprints(orderedResults);


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
            switch (msg.what) {
                case MSG_STORE_FINGERPRINT:
                    storingCounter = 1;
                    Log.d(TAG, "Store fingerprint request received!");
                    break;

                case MSG_SIGNAL_POWER_NORMALIZATION_ON:
                    Log.d(TAG, "Signal power normalization turn-ON request received!");
                    signalPowerNormalization = true;
                    /*Bundle bundle = msg.getData();
                    String hello = (String) bundle.get("hello");*/
                    break;

                case MSG_SIGNAL_POWER_NORMALIZATION_OFF:
                    Log.d(TAG, "Signal power normalization turn-OFF request received!");
                    signalPowerNormalization = false;
                    break;

                default:
                    super.handleMessage(msg);
            }

        }
    }
}
