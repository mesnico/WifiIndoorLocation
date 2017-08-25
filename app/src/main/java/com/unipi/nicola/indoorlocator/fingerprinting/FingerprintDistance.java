package com.unipi.nicola.indoorlocator.fingerprinting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Nicola on 21/05/2017.
 */

public abstract class FingerprintDistance {
    private static final double ALPHA = 24;
    protected static final int MIN_STRENGTH = -120;

    private List<WifiFingerprint> orderedFingerprints;

    /* utilities functions to normalize distances */
    protected static double positiveRepresentation(double inputSignal){
        return inputSignal - MIN_STRENGTH;
    }

    protected static double exponentialRepresentation(double inputSignal){
        return Math.exp(inputSignal/ALPHA) / Math.exp(-MIN_STRENGTH/ALPHA);
    }

    abstract public double computeDistance(WifiFingerprint thisFp, WifiFingerprint otherFp);

    /**
     * Compares this fingerprint with all the fingerprints in the inputFPList and returns a list sorted
     * by similarity among the query and the list fingerprints. The top ranked k elements are returned
     * @param inputFPList the fingerprints to be compared against
     * @param inputFP the fingerprint that must be compared against inputFPList
     * @param k number of nearest neighbors to retrieve
     * @return the top k ranked fingerprints
     */
    public List<WifiFingerprint> computeNearestFingerprints(List<WifiFingerprint> inputFPList, WifiFingerprint inputFP, int maxReturnedFingerprints){
        //computes the distances from the query to all the fingerprints in the list
        for(WifiFingerprint fp : inputFPList){
            fp.setDistance(computeDistance(fp, inputFP));
        }
        List<WifiFingerprint> sortedFPList = new ArrayList<>(inputFPList);
        Collections.sort(sortedFPList);

        //returns only the k top ranked, if k<fp.size(), otherwise returns all fingerprints in the original list
        orderedFingerprints = sortedFPList.subList(0, Math.min(maxReturnedFingerprints, inputFPList.size()));

        //return a copy to avoid external users to modify the reference to the internal ordered list
        return new ArrayList<>(orderedFingerprints);
    }

    /**
     * Filters the ordered fingerprints list using maximum distance and maximum found neighbors as filters
     * @param maxDistance maximum distance filter
     * @param maxK maximum number of neighbors (it will be the size of the output list)
     * @return a filtered view of the original list
     */

    public List<WifiFingerprint> filterComputedNearestFingerprints(double maxDistance, int maxK){
        List<WifiFingerprint> filtered = new ArrayList<>();
        int k = 0;
        for(WifiFingerprint o : orderedFingerprints){
            if(k >= maxK || o.getDistance() > maxDistance) break;
            filtered.add(o);
            k++;
        }
        return filtered;
    }
}
