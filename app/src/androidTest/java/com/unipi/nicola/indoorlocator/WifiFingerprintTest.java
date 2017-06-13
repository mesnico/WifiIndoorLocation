package com.unipi.nicola.indoorlocator;

import android.location.Location;
import android.util.Log;

import com.unipi.nicola.indoorlocator.fingerprinting.AccessPointInfos;
import com.unipi.nicola.indoorlocator.fingerprinting.FingerprintDistance;
import com.unipi.nicola.indoorlocator.fingerprinting.FingerprintEuclidDistance;
import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprint;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Nicola on 12/05/2017.
 */
public class WifiFingerprintTest {
    FingerprintDistance stdEuclid = new FingerprintEuclidDistance(true);

    @Test
    public void ComputeDistance1() {
        List<AccessPointInfos> ap1 = new ArrayList<>();
        ap1.add(new AccessPointInfos(-40, "44:44:44:44:44:44", "net1"));
        ap1.add(new AccessPointInfos(-80, "11:11:11:11:11:11", "net2"));
        ap1.add(new AccessPointInfos(-75, "22:22:22:22:22:22", "net3"));
        WifiFingerprint fp1 = new WifiFingerprint(ap1, new Location("WifiLocator"), "stanza1");

        List<AccessPointInfos> ap2 = new ArrayList<>();
        ap2.add(new AccessPointInfos(-40, "44:44:44:44:44:44", "net1"));
        ap2.add(new AccessPointInfos(-80, "11:11:11:11:11:11", "net2"));
        ap2.add(new AccessPointInfos(-75, "22:22:22:22:22:22", "net3"));
        WifiFingerprint fp2 = new WifiFingerprint(ap2, new Location("WifiLocator"), "stanza1");

        double distance = stdEuclid.computeDistance(fp1, fp2);

        assertEquals(0.0, distance, 0.001);
    }

    @Test
    public void ComputeDistance2() {
        List<AccessPointInfos> ap1 = new ArrayList<>();
        ap1.add(new AccessPointInfos(-50, "44:44:44:44:44:44", "net1"));
        ap1.add(new AccessPointInfos(-80, "11:11:11:11:11:11", "net2"));
        ap1.add(new AccessPointInfos(-75, "22:22:22:22:22:22", "net3"));
        WifiFingerprint fp1 = new WifiFingerprint(ap1, new Location("WifiLocator"), "stanza1");

        List<AccessPointInfos> ap2 = new ArrayList<>();
        ap2.add(new AccessPointInfos(-75, "22:22:22:22:22:22", "net3"));
        ap2.add(new AccessPointInfos(-80, "11:11:11:11:11:11", "net2"));
        ap2.add(new AccessPointInfos(-40, "44:44:44:44:44:44", "net1"));
        WifiFingerprint fp2 = new WifiFingerprint(ap2, new Location("WifiLocator"), "stanza1");

        double distance = stdEuclid.computeDistance(fp1, fp2);

        assertEquals(10.0, distance, 0.001);
    }

    @Test
    public void ComputeDistance3() {
        List<AccessPointInfos> ap1 = new ArrayList<>();
        ap1.add(new AccessPointInfos(-50, "44:44:44:44:44:44", "net1"));
        ap1.add(new AccessPointInfos(-80, "11:11:11:11:11:11", "net2"));
        ap1.add(new AccessPointInfos(-75, "22:22:22:22:22:22", "net3"));
        WifiFingerprint fp1 = new WifiFingerprint(ap1, new Location("WifiLocator"), "stanza1");

        List<AccessPointInfos> ap2 = new ArrayList<>();
        ap2.add(new AccessPointInfos(-75, "22:22:22:22:22:22", "net3"));
        ap2.add(new AccessPointInfos(-80, "11:11:11:11:11:11", "net2"));
        ap2.add(new AccessPointInfos(-40, "44:44:44:44:44:44", "net1"));
        ap2.add(new AccessPointInfos(-10, "55:55:55:55:55:55", "net5"));
        WifiFingerprint fp2 = new WifiFingerprint(ap2, new Location("WifiLocator"), "stanza1");

        double distance = stdEuclid.computeDistance(fp1, fp2);
        assertEquals(110.4536, distance, 0.001);
        //assertEquals(10.0, distance, 0.001);
    }

    @Test
    public void ComputeDistance5() {
        List<AccessPointInfos> ap2 = new ArrayList<>();
        ap2.add(new AccessPointInfos(-75, "22:22:22:22:22:22", "net3"));
        ap2.add(new AccessPointInfos(-80, "11:11:11:11:11:11", "net2"));
        ap2.add(new AccessPointInfos(-40, "44:44:44:44:44:44", "net1"));
        WifiFingerprint fp2 = new WifiFingerprint(ap2, new Location("WifiLocator"), "stanza2");

        List<AccessPointInfos> ap3 = new ArrayList<>();
        ap3.add(new AccessPointInfos(-75, "22:22:22:22:22:22", "net3"));
        ap3.add(new AccessPointInfos(-80, "11:11:11:11:11:11", "net2"));
        ap3.add(new AccessPointInfos(-40, "33:33:33:33:33:33", "net1"));
        WifiFingerprint fp3 = new WifiFingerprint(ap3, new Location("WifiLocator"), "stanza3");

        double distance = stdEuclid.computeDistance(fp2, fp3);

        assertEquals(113.137, distance, 0.001);
    }

    @Test
    public void ComputeNearestFingerprints(){
        List<AccessPointInfos> ap1 = new ArrayList<>();
        ap1.add(new AccessPointInfos(-50, "44:44:44:44:44:44", "net1"));
        ap1.add(new AccessPointInfos(-80, "11:11:11:11:11:11", "net2"));
        ap1.add(new AccessPointInfos(-75, "22:22:22:22:22:22", "net3"));
        ap1.add(new AccessPointInfos(-10, "55:55:55:55:55:55", "net5"));
        WifiFingerprint fp1 = new WifiFingerprint(ap1, new Location("WifiLocator"), "stanza1");

        List<AccessPointInfos> ap2 = new ArrayList<>();
        ap2.add(new AccessPointInfos(-75, "22:22:22:22:22:22", "net3"));
        ap2.add(new AccessPointInfos(-80, "11:11:11:11:11:11", "net2"));
        ap2.add(new AccessPointInfos(-40, "44:44:44:44:44:44", "net1"));
        WifiFingerprint fp2 = new WifiFingerprint(ap2, new Location("WifiLocator"), "stanza2");

        List<AccessPointInfos> ap3 = new ArrayList<>();
        ap3.add(new AccessPointInfos(-75, "22:22:22:22:22:22", "net3"));
        ap3.add(new AccessPointInfos(-80, "11:11:11:11:11:11", "net2"));
        ap3.add(new AccessPointInfos(-40, "33:33:33:33:33:33", "net1"));
        WifiFingerprint fp3 = new WifiFingerprint(ap3, new Location("WifiLocator"), "stanza3");

        List<AccessPointInfos> ap4 = new ArrayList<>();
        ap4.add(new AccessPointInfos(-75, "22:22:22:22:22:22", "net3"));
        ap4.add(new AccessPointInfos(-80, "11:11:11:11:11:11", "net2"));
        WifiFingerprint fp4 = new WifiFingerprint(ap4, new Location("WifiLocator"), "stanza4");

        List<WifiFingerprint> tosubmit = new ArrayList<>();
        tosubmit.add(fp1);
        tosubmit.add(fp2);
        tosubmit.add(fp3);
        tosubmit.add(fp4);
        List<WifiFingerprint> result = stdEuclid.computeNearestFingerprints(tosubmit,fp2,10);
        for(WifiFingerprint f : result){
            Log.d("************ ",f.getLocationLabel()+"; "+f.getDistance());
        }
    }
}