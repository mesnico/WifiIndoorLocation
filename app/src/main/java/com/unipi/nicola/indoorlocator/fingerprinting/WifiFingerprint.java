package com.unipi.nicola.indoorlocator.fingerprinting;

import android.location.Location;
import android.os.Bundle;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Created by Nicola on 14/04/2017.
 *
 * This class describes the Wifi Fingerprint: its core is a list of AccessPoints signatures that
 * fully characterize this fingerprint. The Fingerprint is associated a known location.
 */

public class WifiFingerprint implements Comparable<WifiFingerprint>, Serializable{
    private Location location;
    private List<AccessPointInfos> accessPoints;
    private double distance = 0;
    private int id;


    /**
     * Construct a fingerprint with the given access points, the associated location and the method
     * to compute the distance.
     *
     * @param accessPoints list of access points for this fingerprint.
     * @param location location associated to this fingerprint
     * @param locationLabel location label associaterd to this fingerprint (e.g.: "room1")
     */
    public WifiFingerprint(List<AccessPointInfos> accessPoints, Location location, String locationLabel){
        this.location = location;
        if(location!=null){
            this.setLocationLabel(locationLabel);
        }
        this.accessPoints = accessPoints;
        Collections.sort(accessPoints);
    }

    @Override
    public int compareTo(WifiFingerprint o) {
        return (int)((this.distance - o.distance)*10);
    }

    public Location getLocation() {
        return location;
    }

    /**
     * This function returns a string label associated to this location (e.g., the name of a room)
     * @return The label associated to this location
     */
    public String getLocationLabel(){
        Bundle b = location.getExtras();
        return b.getString("label");
    }

    /**
     * This function sets the label associated to this location (e.g., the name of a room)
     * @param label The label to be associated to this location
     */
    public void setLocationLabel(String label){
        Bundle b = new Bundle();
        b.putString("label", label);
        location.setExtras(b);
    }

    public double getDistance(){
        return distance;
    }
    public void setDistance(double distance){
        this.distance = distance;
    }

    public List<AccessPointInfos> getAccessPoints() {
        return accessPoints;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
