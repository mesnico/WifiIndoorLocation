package com.unipi.nicola.indoorlocator.fingerprinting;

import android.net.wifi.ScanResult;

/**
 * Created by Nicola on 14/04/2017.
 *
 * Class describing the signature characteristics of each access point.
 */

public class AccessPointInfos implements Comparable<AccessPointInfos>{
    private int signalStrength;
    private String hwAddress;
    private String networkName;

    public AccessPointInfos(ScanResult s) {
        signalStrength = s.level;
        hwAddress = s.BSSID;
        networkName = s.SSID;
    }

    public AccessPointInfos(int signalStrength, String hwAddress, String networkName) {
        this.signalStrength = signalStrength;
        this.hwAddress = hwAddress;
        this.networkName = networkName;
    }

    @Override
    public boolean equals(Object o){
        return hwAddress.equals(((AccessPointInfos)o).hwAddress);
    }

    @Override
    public int compareTo(AccessPointInfos o) {
        return getHwAddress().compareTo(
                o.getHwAddress());
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public String getHwAddress() {
        return hwAddress;
    }

    public String getNetworkName() {
        return networkName;
    }

    @Override
    public String toString(){
        return networkName + ":" + signalStrength + " -- ";
    }
}
