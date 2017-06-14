package com.unipi.nicola.indoorlocator;

import android.app.Application;

import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprint;

import java.util.List;

/**
 * Created by Nicola on 09/06/2017.
 */

public class IndoorLocatorApplication extends Application {
    public static final String LOCATION_ESTIMATION_READY = "it.unipi.iet.LOCATION_ESTIMATION_READY";
    private List<WifiFingerprint> kBestFingerprints;
    private WifiFingerprint currentFingerprint;

    public List<WifiFingerprint> getkBestFingerprints() {
        return kBestFingerprints;
    }

    public void setkBestFingerprints(List<WifiFingerprint> kBestFingerprints) {
        this.kBestFingerprints = kBestFingerprints;
    }

    public WifiFingerprint getCurrentFingerprint() {
        return currentFingerprint;
    }

    public void setCurrentFingerprint(WifiFingerprint currentFingerprint) {
        this.currentFingerprint = currentFingerprint;
    }
}
