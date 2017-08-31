package com.unipi.nicola.indoorlocator;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import java.io.Serializable;

/**
 * Created by Nicola on 30/08/2017.
 */

public class CalibrationData implements Serializable{
    private String label;
    private Array2DRowRealMatrix calibrationMatrix;

    public CalibrationData(String label, Array2DRowRealMatrix calibrationMatrix) {
        this.label = label;
        this.calibrationMatrix = calibrationMatrix;
    }

    public String getLabel() {
        return label;
    }

    public RealMatrix getCalibrationMatrix() {
        return calibrationMatrix;
    }

    @Override
    public String toString() {
        return label;
    }
}
