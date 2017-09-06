package com.unipi.nicola.indoorlocator;

import android.location.Location;

/**
 * Created by Nicola on 06/09/2017.
 */

//set containing all the estimated locations visited so far. It is used in order to print once
//the marker on the map for every distinct estimated location
public class ComparableLocation extends Location {
    public ComparableLocation(Location l) {
        super(l);
    }
    @Override
    public boolean equals(Object o){
        ComparableLocation cl = (ComparableLocation) o;
        return cl.getExtras().getString("id").equals(this.getExtras().getString("id"));
    }
    @Override
    public int hashCode() {
        return this.getExtras().getString("id").hashCode();
    }
}
