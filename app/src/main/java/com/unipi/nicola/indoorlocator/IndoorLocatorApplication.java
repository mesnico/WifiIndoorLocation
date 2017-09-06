package com.unipi.nicola.indoorlocator;

import android.app.Application;
import android.graphics.PointF;
import android.location.Location;

import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Nicola on 09/06/2017.
 */

public class IndoorLocatorApplication extends Application {
    public static final String LOCATION_ESTIMATION_READY = "it.unipi.iet.LOCATION_ESTIMATION_READY";
    public static final String FINGERPRINT_SCAN_AVAILABLE = "it.unipi.iet.FINGERPRINT_SCAN_AVAILABLE";
    public static final String NEW_INERTIAL_POSITION_AVAILABLE = "it.unipi.iet.NEW_INERTIAL_POSITION_AVAILABLE";
    private List<WifiFingerprint> kBestFingerprints;
    private WifiFingerprint currentFingerprint;
    private Location estimatedLocation;
    private List<PointF> positionsList = new ArrayList<>();
    private Set<ComparableLocation> visitedLocationsSet = new HashSet<>();

    public Set<ComparableLocation> getVisitedLocationsSet() {
        return visitedLocationsSet;
    }

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

    public Location getEstimatedLocation() {
        return estimatedLocation;
    }

    public void setEstimatedLocation(Location estimatedLocation) {
        this.estimatedLocation = estimatedLocation;
    }

    public List<PointF> getPositionsList() {
        return positionsList;
    }

    public void setPositionsList(List<PointF> positionsList) {
        this.positionsList = positionsList;
    }
}
