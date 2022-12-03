package edu.ut.cs.sdn.simpledns;

class IPAddress {
    public static final int NUM_BITS = 32;

    public static long fromString(String ipAddress) {
        String addr = ipAddress.split("/")[0];
        long address = 0L;
        for (String part : addr.split("\\.")) {
            long val = Long.parseLong(part);
            address = (address << 8) | (val & 0xff);
        }
        return address;
    }

    public static int getBits(String ipAddress) {
        return Integer.parseInt(ipAddress.split("/")[1]);
    }

    public static String fromLong(long ipAddress) {
        StringBuilder address = new StringBuilder();
        for (int i = 0; i < 4; ++i) {
            String part = Long.toString(ipAddress & 0xff);
            ipAddress >>= 8;
            address.insert(0, "." + part);
        }

        // get rid of the extra separator at the beginning of the string
        return address.substring(1);
    }
}