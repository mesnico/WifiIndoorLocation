package com.unipi.nicola.indoorlocator.fingerprinting;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Nicola on 21/05/2017.
 */

/* TO TEST */
public class FingerprintEuclidDistance extends FingerprintDistance {
    private boolean exponential;

    public FingerprintEuclidDistance(boolean exponential) {
        this.exponential = exponential;
    }

    /**
     * Computes the distance among two different fingerprints, using the Euclidean metric
     * @return the distance between the fingerprints
     */
    @Override
    public double computeDistance(WifiFingerprint thisFp, WifiFingerprint otherFp){
        int thisFpSize = thisFp.getAccessPoints().size();
        int otherFpSize = otherFp.getAccessPoints().size();

        AccessPointInfos[] thisFpArr = thisFp.getAccessPoints().toArray(new AccessPointInfos[thisFpSize]);
        AccessPointInfos[] otherFpArr = otherFp.getAccessPoints().toArray(new AccessPointInfos[otherFpSize]);

        double sum = 0;
        int thisIndex = 0;
        int otherIndex = 0;
        /* Optimized comparison among two ordered lists, in order to compute the euclidean distance.
           The APs that are not common to both lists are assigned a signal equal to MIN_SIGNAL_LEVEL dB.
         */
        while(thisIndex < thisFpSize && otherIndex < otherFpSize) {
            AccessPointInfos thisAp = thisFpArr[thisIndex];
            AccessPointInfos otherAp = otherFpArr[otherIndex];

            int compareResult = thisAp.compareTo(otherAp);
            if (compareResult == 0) {
                // only compare the level of the same wifi address
                sum += Math.pow(
                        normalize(thisAp.getSignalStrength()) - normalize(otherAp.getSignalStrength()),
                        2
                );

                thisIndex++;
                otherIndex++;
            } else if (compareResult < 0) {
                sum += Math.pow(
                        normalize(thisAp.getSignalStrength()) - normalize(MIN_STRENGTH),
                        2
                );
                thisIndex++;

            } else {
                sum += Math.pow(
                        normalize(otherAp.getSignalStrength()) - normalize(MIN_STRENGTH),
                        2
                );
                otherIndex++;
            }
        }

        //we must finish scanning the list not still completed
        AccessPointInfos[] toCompleteArr;
        int toCompleteIndex;
        if(thisIndex < thisFpSize){
             toCompleteArr = thisFpArr;
             toCompleteIndex = thisIndex;
        } else {
            toCompleteArr = otherFpArr;
            toCompleteIndex = otherIndex;
        }

        while(toCompleteIndex < toCompleteArr.length) {
            AccessPointInfos a = toCompleteArr[toCompleteIndex];
            sum += Math.pow(
                    normalize(a.getSignalStrength()) - normalize(MIN_STRENGTH),
                    2
            );
            toCompleteIndex++;
        }

        return Math.sqrt(sum);
    }

    private double normalize(double signalStrength){
        double ret = positiveRepresentation(signalStrength);
        return (exponential) ? exponentialRepresentation(ret) : ret;
    }
}
