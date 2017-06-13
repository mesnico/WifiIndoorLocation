package com.unipi.nicola.indoorlocator.fingerprinting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Nicola on 21/05/2017.
 */

public abstract class FingerprintDistance {
    private static final double ALPHA = 24;
    protected static final int MIN_STRENGTH = -120;

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
    public List<WifiFingerprint> computeNearestFingerprints(List<WifiFingerprint> inputFPList, WifiFingerprint inputFP, int k){
        //computes the distances from the query to all the fingerprints in the list
        for(WifiFingerprint fp : inputFPList){
            fp.setDistance(computeDistance(fp, inputFP));
        }
        List<WifiFingerprint> sortedFPList = new ArrayList<>(inputFPList);
        Collections.sort(sortedFPList);

        //returns only the k top ranked, if k<fp.size(), otherwise returns all fingerprints in the original list
        return sortedFPList.subList(0, Math.min(k, inputFPList.size()));
    }
}
