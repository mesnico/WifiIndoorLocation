package com.unipi.nicola.indoorlocator.fingerprinting;

import java.util.Iterator;

/**
 * Created by Nicola on 21/05/2017.
 */

/* TO TEST - ACTUALLY NOT WORKING */


public class FingerprintAsymmDistance extends FingerprintDistance {
    /* coefficient used for evaluation of the cardinality of
    *  the intersection among the sets of access points for the two fingerprints */
    private double alpha = 0;
    private static int MIN_SIGNAL_LEVEL = -120;

    public FingerprintAsymmDistance(double alpha) {
        this.alpha = alpha;
    }

    /**
     * Computes the distance among two different fingerprints. Only the common APs are counted for the
     * calculus of the euclidean distance. The difference in the sets theirself is handled by the alpha
     * parameter (if the 2 sets are the same and alpha==1, then the distance is virtually brought to 0)
     * --> see the paper "Indoor localization based on IAP" to know how distance is calculated:
     * all the APs of the otherFP are compared with the APs of thisFP. If thisFP doesn't carry an AP
     * present in otherAP, then -120db are considered as virtual AP signal strength for the absent AP.
     * @return the distance between the fingerprints
     */
    @Override
    public double computeDistance(WifiFingerprint thisFp, WifiFingerprint otherFp){
        Iterator<AccessPointInfos> thisFPListIterator = thisFp.getAccessPoints().iterator();
        Iterator<AccessPointInfos> otherFPListIterator = otherFp.getAccessPoints().iterator();

        int sum = 0;
        int numberOfMatchingAPs = 0;
        boolean lastIt = false;

        AccessPointInfos thisAP = thisFPListIterator.next();
        AccessPointInfos otherAP = otherFPListIterator.next();

        /* Optimized comparison among two ordered lists, in order to compute the euclidean distance.
           The APs that are not common to both lists are assigned a signal equal to MIN_SIGNAL_LEVEL dB.
         */
        while(otherFPListIterator.hasNext() || lastIt){
            int compareResult = thisAP.compareTo(otherAP);
            if (compareResult == 0) {
                // only compare the level of the same wifi address
                numberOfMatchingAPs ++;
                sum += Math.pow(thisAP.getSignalStrength() - otherAP.getSignalStrength(), 2);

                if(lastIt) break;
                thisAP = thisFPListIterator.next();
                otherAP = otherFPListIterator.next();
            } else if (compareResult < 0) {
                /* The APs in the FP DB that are not showing in the unknown FP are not taken into
                  consideration, if we follow the paper */
                // sum += Math.pow(thisAP.getSignalStrength() - MIN_SIGNAL_LEVEL, 2);
                if(lastIt) break;
                thisAP = thisFPListIterator.next();

            } else {
                sum += Math.pow(otherAP.getSignalStrength() - MIN_SIGNAL_LEVEL, 2);
                if(lastIt) break;
                otherAP = otherFPListIterator.next();
            }
            if(!thisFPListIterator.hasNext() || !otherFPListIterator.hasNext()){
                lastIt = true;
            }
        }

        // I have to take into consideration the remaining APs in the "other" list
        while(otherFPListIterator.hasNext()){
            otherAP = otherFPListIterator.next();
            sum += Math.pow(otherAP.getSignalStrength() - MIN_SIGNAL_LEVEL, 2);
        }

        double euclidean = Math.sqrt(sum);

        /* Calculate the distance*/
        int numberOfAPs = otherFp.getAccessPoints().size();
        double distance = (euclidean * (1 - alpha * numberOfMatchingAPs / numberOfAPs));
        return distance;
    }
}
