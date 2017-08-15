package com.unipi.nicola.indoorlocator.fingerprinting;

import com.unipi.nicola.indoorlocator.fingerprinting.AccessPointInfos;
import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Nicola on 11/08/2017.
 */

public class WifiAPsAggregator {
    private static class Aggregation{
        private int counter;
        private int signalStrength;
        private String SSID;

        public Aggregation(int signalStrength, String SSID) {
            this.signalStrength = signalStrength;
            this.SSID = SSID;
            counter = 1;
        }

        public String getSSID() {
            return SSID;
        }

        public int getCounter() {
            return counter;
        }

        public void increment() {
            this.counter++;
        }

        public int getSignalStrength() {
            return signalStrength;
        }

        public void setSignalStrength(int signalStrength) {
            this.signalStrength = signalStrength;
        }
    }
    Map<String,Aggregation> partialAggregate;
    int globalCounter = 0;  //counts how many times "insertMeasurement()" was called

    public WifiAPsAggregator(){
        partialAggregate = new HashMap<>();
    }

    public void insertMeasurement(List<AccessPointInfos> apis){
        globalCounter++;
        for(AccessPointInfos i : apis){
            String hwAddress = i.getHwAddress();
            if(partialAggregate.containsKey(hwAddress)){
                Aggregation currentEntry = partialAggregate.get(hwAddress);
                //sum the signal strengths in order to calculate the mean
                currentEntry.setSignalStrength(currentEntry.getSignalStrength() + i.getSignalStrength());
                //increment the counter for this access point
                currentEntry.increment();
            } else {
                //create a new entry in the hash map
                partialAggregate.put(i.getHwAddress(),new Aggregation(i.getSignalStrength(),i.getNetworkName()));
            }
        }
    }

    /* returns the aggregation, that is, in this case, the access points that have a number of occurrencies
     * equal to globalCounter, with associated signal stregth equal to the mean of the signal strengths
     */
    public List<AccessPointInfos> returnAggregatedAPs(){
        List<AccessPointInfos> apis = new LinkedList<>();
        for (Map.Entry<String, Aggregation> entry : partialAggregate.entrySet()) {
            if(entry.getValue().getCounter() == globalCounter){
                //this way we discard all access point that are not stable (they disappear at least one time)
                apis.add(new AccessPointInfos(
                        entry.getValue().getSignalStrength() / entry.getValue().getCounter(),
                        entry.getKey(),
                        entry.getValue().getSSID()));
            }
        }
        return apis;
    }
}
