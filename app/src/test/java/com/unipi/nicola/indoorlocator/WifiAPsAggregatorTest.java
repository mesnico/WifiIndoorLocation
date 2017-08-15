package com.unipi.nicola.indoorlocator;

import com.unipi.nicola.indoorlocator.fingerprinting.AccessPointInfos;
import com.unipi.nicola.indoorlocator.fingerprinting.WifiAPsAggregator;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Nicola on 11/08/2017.
 */
public class WifiAPsAggregatorTest {
    WifiAPsAggregator aggregator = new WifiAPsAggregator();

    @Before
    public void insertFingerprint() throws Exception {

        List<AccessPointInfos> list1 = new ArrayList<>();
        AccessPointInfos api1 = new AccessPointInfos(30,"33:33:33:33:33:33","Rete1"); list1.add(api1);
        AccessPointInfos api2 = new AccessPointInfos(40,"44:44:44:44:44:44","Rete2"); list1.add(api2);
        AccessPointInfos api3 = new AccessPointInfos(30,"CC:CC:CC:CC:CC:CC","Rete3"); list1.add(api3);
        aggregator.insertMeasurement(list1);

        List<AccessPointInfos> list2 = new ArrayList<>();
        AccessPointInfos api4 = new AccessPointInfos(20,"33:33:33:33:33:33","Rete1"); list2.add(api4);
        AccessPointInfos api6 = new AccessPointInfos(10,"CC:CC:CC:CC:CC:CC","Rete3"); list2.add(api6);
        aggregator.insertMeasurement(list2);


    }

    @Test
    public void returnAggregatedAPs() throws Exception {
        List<AccessPointInfos> aggregation = aggregator.returnAggregatedAPs();
        assertEquals(2, aggregation.size());
        for(AccessPointInfos u : aggregation) {
            System.out.println(u.getNetworkName());
        }
        assertEquals(20, aggregation.get(0).getSignalStrength());
        assertEquals(25, aggregation.get(1).getSignalStrength());
    }

}