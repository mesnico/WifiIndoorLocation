package com.unipi.nicola.indoorlocator;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class InertialPedestrianNavigationService extends Service implements SensorEventListener{
    public static final String TAG = "InertialNavService";

    private static final float LAT_TO_METERS = 110.574f * 1000;
    private static final float LONG_TO_METERS = 111.320f * 1000;

    IndoorLocatorApplication app;

    private SensorManager mSensorManager;
    private Sensor rotationSensor;
    private Sensor stepSensor;

    //if true, the next sample of the rotation vector is used to store the new offset
    private boolean storeNewRotationOffset = true;
    //private float[] rotationOffsetVector = new float[4];
    private PointF actualPosition = new PointF(0,0);
    private PointF filteredDirection;
    int stepCounter = 0;

    private Location actualEstimatedLocation;

    //The messenger object that must be passed to the activity in order to contact this service
    private Messenger mMessenger = new Messenger(new IncomingHandler());

    //TODO: modifiable by preference
    //Amount of smoothness for direction filtering
    float beta = 0.5f;
    //Length of a step
    float stepLength = 0.74f; //0.74 meters
    //Time interval after wich the new position is notified
    int updateSteps = 3; //steps needed to have an update on the map

    public InertialPedestrianNavigationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind done");
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //initialize the receiver for listening for new locations
        this.registerReceiver(locationEstimationReadyReceiver, new IntentFilter(
                IndoorLocatorApplication.LOCATION_ESTIMATION_READY));

        //initialize the sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        stepSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if(rotationSensor != null && stepSensor != null){
            //if the devices has both sensors, then register the listeners
            mSensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        app = (IndoorLocatorApplication) getApplication();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(locationEstimationReadyReceiver);
    }

    /**
     * Receives notifications about the new estimations of the position in order to recalibrate the
     * inertial navigation system
     */
    private final transient BroadcastReceiver locationEstimationReadyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //if the previous estimated location equals the new one, then the update is discarded
            if(actualEstimatedLocation != null) {
                String oldId = actualEstimatedLocation.getExtras().getString("id");
                String newId = app.getEstimatedLocation().getExtras().getString("id");
                if (oldId.equals(newId)) {
                    return;
                }
            }
            actualPosition = new PointF(
                    (float) app.getEstimatedLocation().getLongitude(),
                    (float) app.getEstimatedLocation().getLatitude()
            );
            actualEstimatedLocation = new Location(app.getEstimatedLocation());
        }
    };

    /*
     * Handler for requests coming from the activity
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            if(storeNewRotationOffset){
                //TODO: is this needed?
                Log.d(TAG, "new offset stored!");
                storeNewRotationOffset = false;
            }
            float[] rotationMatrix = new float[16];
            float[] rotationAngles = new float[3];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, rotationAngles);
            //Log.d(TAG,"Azimuth:"+rotationAngles[0]+"; Pitch:"+rotationAngles[1]+"; Roll:"+rotationAngles[2]);
            PointF direction = new PointF(-(float)Math.sin(-rotationAngles[0]), (float)Math.cos(-rotationAngles[0]));
            //TODO: calculate the UFD so that phone can be held in any position
            //NOTE: direction must be a normalized vector

            //filter the direction vector
            if(filteredDirection == null){
                filteredDirection = new PointF(direction.x, direction.y);
            } else {
                filteredDirection.x = filteredDirection.x * beta + direction.x * (1 - beta);
                filteredDirection.y = filteredDirection.y * beta + direction.y * (1 - beta);
            }

        }
        if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR){
            stepCounter++;
            //update the position using the current direction. position is needed as lat lon coordinates
            //https://stackoverflow.com/questions/1253499/simple-calculations-for-working-with-lat-lon-km-distance
            if(filteredDirection != null) {
                actualPosition.y += filteredDirection.y * stepLength * (1 / LAT_TO_METERS);
                actualPosition.x += filteredDirection.x * stepLength * (1 / (LONG_TO_METERS * Math.cos(actualPosition.y * Math.PI / 180)));
                if (stepCounter % updateSteps == 0) {
                    //send the new estimated position in a broadcast intent
                    sendInertialPosition();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void sendInertialPosition(){
        app.getPositionsList().add(new PointF(actualPosition.x, actualPosition.y));

        //notifies all the broadcast receivers for this new data
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(IndoorLocatorApplication.NEW_INERTIAL_POSITION_AVAILABLE);
        sendBroadcast(broadcastIntent);
        Log.d(TAG, "New inertial position sent!");
    }
}
