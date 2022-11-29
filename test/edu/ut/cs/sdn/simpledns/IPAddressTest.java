package edu.ut.cs.sdn.simpledns;

import org.junit.Assert;
import org.junit.Test;

public class IPAddressTest {
    @Test
    public void testFromString() {
        String ip = "192.168.1.2/0";
        long value = 3232235778L;
        long actual = IPAddress.fromString(ip);
        Assert.assertEquals(value, actual);
    }

    @Test
    public void testFromLong() {
        long ip = 3232235778L;
        String value = "192.168.1.2";
        String actual = IPAddress.fromLong(ip);
        Assert.assertEquals(value, actual);
    }

    @Test
    public void testFromBits() {
        String ip = "192.168.0.0/16";
        int value = 16;
        int actual = IPAddress.getBits(ip);
        Assert.assertEquals(value, actual);
    }
}
