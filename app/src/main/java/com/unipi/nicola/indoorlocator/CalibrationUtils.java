package com.unipi.nicola.indoorlocator;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Nicola on 30/08/2017.
 */

public class CalibrationUtils {
    /*static class RealMatrixInstanceCreator implements InstanceCreator<RealMatrix> {
        public RealMatrix createInstance(Type type) {
            return MatrixUtils.createRealMatrix(3,3);
        }
    }*/

    static final Gson gson = new GsonBuilder()/*.registerTypeAdapter(RealMatrix.class, new RealMatrixInstanceCreator())*/.create();

    public static final String TAG = "CalibrationUtils";

    /**
     * save the calibration, if there no exist other calibrations with the same label
     * @param context
     * @param label
     * @param calibration
     * @return true if save succeeded, false if another calibration with that label already existed
     */
    public static boolean saveCalibration(Context context, String label, Array2DRowRealMatrix calibration){
        if(existsCalibration(context, label) != null) {
            Log.d(TAG, "Calibration "+label+ "is already existing");
            return false;
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();

        //retrieve the current calibrations list
        List<CalibrationData> calibrations = getCalibrations(context);

        //add the new calibration and reserialize
        calibrations.add(new CalibrationData(label, calibration));
        String serializedObject = gson.toJson(calibrations);
        editor.putString("calibration",serializedObject);
        editor.apply();

        Log.d(TAG, "New calibration added! Now size is: "+calibrations.size());
        return true;
    }

    public static List<CalibrationData> getCalibrations(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        List<CalibrationData> calibrations;
        String serializedCalibration = sharedPref.getString("calibration",null);
        if(serializedCalibration == null) {
            calibrations = new ArrayList<>();
        } else {
            //list of calibration data type
            Type type = new TypeToken<ArrayList<CalibrationData>>() {}.getType();
            calibrations = gson.fromJson(serializedCalibration, type);
        }
        return calibrations;
    }

    public static void deleteCalibration(Context context, final String label){
        //use the label as key

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();

        //retrieve the current calibrations list
        List<CalibrationData> calibrations = getCalibrations(context);

        //remove the given calibration and reserialize
        Iterator<CalibrationData> iterator = calibrations.iterator();
        while(iterator.hasNext()) {
            CalibrationData cd = iterator.next();
            if(cd.getLabel().equals(label)) {
                iterator.remove();
            }
        }
        String serializedObject = gson.toJson(calibrations);
        editor.putString("calibration",serializedObject);
        editor.apply();

        Log.d(TAG, "New calibration added! Now size is: "+calibrations.size());
    }

    public static CalibrationData existsCalibration(Context context, String label){
        List<CalibrationData> calibrations = getCalibrations(context);
        for(CalibrationData c : calibrations){
            if(c.getLabel().equals(label)){
                return c;
            }
        }
        return null;
    }

    public static void setCalibrationLabelInUse(Context context, String label){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("calibration_in_use", label);
        editor.apply();
    }

    public static CalibrationData getCalibrationInUse(Context context){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String label = sharedPref.getString("calibration_in_use", null);

        if(label == null)
            return null;

        return existsCalibration(context, label);
    }
}
