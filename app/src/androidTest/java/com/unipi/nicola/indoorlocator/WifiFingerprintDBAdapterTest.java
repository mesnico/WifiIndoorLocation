package com.unipi.nicola.indoorlocator;

import android.content.Context;
import android.location.Location;
import android.support.test.InstrumentationRegistry;

import com.unipi.nicola.indoorlocator.fingerprinting.AccessPointInfos;
import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprint;
import com.unipi.nicola.indoorlocator.fingerprinting.WifiFingerprintDBAdapter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Nicola on 08/05/2017.
 */
public class WifiFingerprintDBAdapterTest {
    private WifiFingerprintDBAdapter dba;
    @Before
    public void setUp() throws Exception {
        Context c = InstrumentationRegistry.getContext();
        dba = new WifiFingerprintDBAdapter(c);
        dba.open(true);
    }

    @After
    public void tearDown() throws Exception {
        //dba.close();
    }

    @Test
    public void isOpen() throws Exception {
        assertTrue(dba.isOpen());
    }

    @Test
    public void insertFingerprint1() throws Exception {
        //inserts a fingerprint
        List<AccessPointInfos> aps1 = new ArrayList<>();
        aps1.add(new AccessPointInfos(-40, "44:44:44:44:44:44", "net1"));
        aps1.add(new AccessPointInfos(-80, "11:11:11:11:11:11", "net2"));
        aps1.add(new AccessPointInfos(-75, "22:22:22:22:22:22", "net3"));
        WifiFingerprint fp1 = new WifiFingerprint(aps1,new Location("WifiLocator"), "stanza1");
        dba.insertFingerprint(fp1);
        List<AccessPointInfos> aps2 = new ArrayList<>();
        aps2.add(new AccessPointInfos(-80, "44:44:44:44:44:44", "net4"));
        aps2.add(new AccessPointInfos(-75, "22:22:22:22:22:22", "net5"));
        WifiFingerprint fp2 = new WifiFingerprint(aps2,new Location("WifiLocator"), "stanza2");
        dba.insertFingerprint(fp2);

        //checks if that fingerprint 11:11:11:11:11:11 gives me "stanza1"
        List<AccessPointInfos> sensedAps = new ArrayList<>();
        sensedAps.add(new AccessPointInfos(-20, "11:11:11:11:11:11", "net1"));
        List<WifiFingerprint> retFP = dba.extractCommonFingerprints(new WifiFingerprint(sensedAps,null,null), 1);

        assertEquals(1,retFP.size());
        assertEquals("stanza1", retFP.get(0).getLocationLabel());

        //checks if that fingerprint 11:11:11:11:11:11 and 22:22:22:22:22:22 give me "stanza1" if lowerbound is 2
        sensedAps = new ArrayList<>();
        sensedAps.add(new AccessPointInfos(-20, "11:11:11:11:11:11", "net1"));
        sensedAps.add(new AccessPointInfos(-20, "22:22:22:22:22:22", "net1"));
        retFP = dba.extractCommonFingerprints(new WifiFingerprint(sensedAps,null,null), 2);

        assertEquals(1,retFP.size());
        assertEquals("stanza1", retFP.get(0).getLocationLabel());


    }

}