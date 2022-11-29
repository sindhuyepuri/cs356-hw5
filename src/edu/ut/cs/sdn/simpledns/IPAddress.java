package edu.ut.cs.sdn.simpledns;

class IPAddress {
    public static final String SEPARATOR = ".";

    public static int fromString(String ipAddress) {
        String addr = ipAddress.split("/")[0];
        int address = 0;
        for (String part : addr.split(IPAddress.SEPARATOR)) {
            int val = Integer.parseInt(part);
            address = (address << 8) | (val & 0xff);
        }
        return address;
    }

    public static int getBits(String ipAddress) {
        return Integer.parseInt(ipAddress.split("/")[1]);
    }

    public static String fromInt(int ipAddress) {
        String address = "";
        for (int i = 0; i < 4; ++i) {
            String part = Integer.toString(ipAddress & 0xff);
            ipAddress >>= 8;
            address = IPAddress.SEPARATOR + part + address;
        }

        // get rid of the extra separator at the beginning of the string
        return address.substring(IPAddress.SEPARATOR.length());
    }
}