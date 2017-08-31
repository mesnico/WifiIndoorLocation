package com.unipi.nicola.indoorlocator;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprintDBAdapter;

public class CalibrationActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener{
    private static final String TAG = "CalibrationActivity";

    //messages
    public static final int MSG_CALIBRATION_COMPLETED = 1;
    public static final int MSG_CALIBRATION_SAVED = 2;

    Messenger mInertialNavigationService = null;
    private Messenger mMessenger = new Messenger(new CalibrationActivity.IncomingHandler());

    Intent inertialNavigationService;

    Button calibrationButton;
    Button saveButton;
    ListView savedCalibrationsList;

    boolean calibrating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        calibrationButton = (Button)findViewById(R.id.calibrate);
        calibrationButton.setOnClickListener(this);

        saveButton = (Button)findViewById(R.id.save_calibration);
        saveButton.setOnClickListener(this);

        savedCalibrationsList = (ListView) findViewById(R.id.saved_calibrations);
        savedCalibrationsList.setOnItemClickListener(this);
        savedCalibrationsList.setOnItemLongClickListener(this);
        updateCalibrationList();

        if(savedInstanceState != null){
            calibrating = savedInstanceState.getBoolean("calibrating");
            handleCalibrating();
        }

        inertialNavigationService = new Intent(this, InertialPedestrianNavigationService.class);
    }

    private void handleCalibrating(){
        calibrationButton.setEnabled(!calibrating);
    }

    private void updateCalibrationList(){
        ArrayAdapter<CalibrationData> calibrationItemsAdapter =
                new ArrayAdapter<>(this, R.layout.calibration_list_item, CalibrationUtils.getCalibrations(this));
        savedCalibrationsList.setAdapter(calibrationItemsAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called");

        if(inertialNavigationService != null && mInertialNavigationService == null) {
            // Bind to the Inertial Navigation Service
            bindService(inertialNavigationService, mInertialServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");

        if(mInertialNavigationService != null){
            unbindService(mInertialServiceConnection);
            mInertialNavigationService = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("calibrating", calibrating);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //send the chosen calibration to the service
        CalibrationData selectedCalibration = (CalibrationData)parent.getItemAtPosition(position);
        Bundle b = new Bundle();
        b.putSerializable("selected_calibration", selectedCalibration);
        Utils.sendMessage(mInertialNavigationService,InertialPedestrianNavigationService.MSG_USE_CALIBRATION, b, null);

        //set the current used calibration label and close this activity
        CalibrationUtils.setCalibrationLabelInUse(this, selectedCalibration.getLabel());

        //terminates this activity
        finish();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final CalibrationData selectedCalibration = (CalibrationData)parent.getItemAtPosition(position);
        //asks the user for deletion of the selected calibration
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to delete calibration \""+selectedCalibration.getLabel()+"\"?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        CalibrationData cd = CalibrationUtils.getCalibrationInUse(CalibrationActivity.this);
                        if(cd != null && selectedCalibration.getLabel().equals(cd.getLabel())){
                            //the calibration I was using is going to be deleted
                            CalibrationUtils.setCalibrationLabelInUse(CalibrationActivity.this, "");
                            Bundle b = new Bundle();
                            //I send a bundle without calibration, so the service is forced to use the default one
                            Utils.sendMessage(mInertialNavigationService, InertialPedestrianNavigationService.MSG_USE_CALIBRATION, b, null);
                        }
                        CalibrationUtils.deleteCalibration(CalibrationActivity.this, selectedCalibration.getLabel());
                        updateCalibrationList();
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

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.calibrate){
            //handle calibrate button
            if(mInertialNavigationService == null){
                //cannot calibrate, cannot reach the inertial service
                Toast.makeText(this,"Calibration failed, cannot reach the inertial background service", Toast.LENGTH_LONG).show();
                return;
            }
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Keep the phone in portrait mode. After the first vibration, put it wherever you prefer. After the second vibration, you'll be ready!")
                    .setCancelable(false)
                    .setPositiveButton("Ok, start calibration", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //send a start calibration message and starts a timer
                            Utils.sendMessage(mInertialNavigationService, InertialPedestrianNavigationService.MSG_START_CALIBRATION, null, null);
                            calibrating = true;
                            handleCalibrating();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        } else if (v.getId() == R.id.save_calibration){
            //handle save calibration button

            //asks the user for the name of the new calibration
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle("New calibration");
            alert.setMessage("Name of the new calibration");

            // Set an EditText view to get user input
            final EditText input = new EditText(this);
            alert.setView(input);

            alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    //sends a save request to the service
                    Bundle b = new Bundle();
                    b.putString("calibration_label", input.getText().toString());
                    Utils.sendMessage(mInertialNavigationService, InertialPedestrianNavigationService.MSG_SAVE_CALIBRATION,b,null);
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });

            alert.show();
        }
    }

    /**
     * Class for interacting with the main interface of the inertial navigation service.
     */
    private ServiceConnection mInertialServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service
            mInertialNavigationService = new Messenger(service);
            //sends an hello message so that the service knows who he is talking to
            Utils.sendMessage(mInertialNavigationService, WifiFingerprintingService.MSG_HELLO_FROM_STORE_FRAGMENT, null, mMessenger);
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mInertialNavigationService = null;
        }
    };

    /**
     * handler for messages coming from InertialPedestrianNavigationService
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            switch (msg.what) {
                case MSG_CALIBRATION_COMPLETED:
                    calibrating = false;
                    handleCalibrating();
                    updateCalibrationList();
                    Toast.makeText(CalibrationActivity.this, "Calibration OK!", Toast.LENGTH_SHORT).show();
                    //empty label because the new calibration still doesn't have a label
                    CalibrationUtils.setCalibrationLabelInUse(CalibrationActivity.this, "");
                    break;
                case MSG_CALIBRATION_SAVED:
                    if(b.getBoolean("success")) {
                        Log.d(TAG, "Calibration saved!");
                        updateCalibrationList();
                    } else {
                        Toast.makeText(CalibrationActivity.this,"Calibration label already in use!", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    }
}
