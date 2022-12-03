package edu.ut.cs.sdn.simpledns;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class EC2Resolver {
    public static final String DELIMETER = ",";
    // bit mask --> map(ip, loc)
    private Map<Integer, Map<Long, String>> map;
    private EC2Resolver() {
        map = new TreeMap<>(); // look at most specific mask first
    }

    private void insert(int prefix, long addr, String location) {
        int bits = IPAddress.NUM_BITS - prefix;
        if (!this.map.containsKey(bits))
            this.map.put(bits, new HashMap<>());
        Map<Long, String> ipMap = this.map.get(bits);
        addr = (addr >> bits) << bits; // zero out lower bits
        ipMap.put(addr, location + "-" + IPAddress.fromLong(addr));
    }

    public String get(String addr) {
        return this.get(IPAddress.fromString(addr));
    }

    public String get(long addr) {
        for (int mask : this.map.keySet()) {
            addr = (addr >> mask) << mask; // zero out lower bits
            if (this.map.get(mask).containsKey(addr))
                return this.map.get(mask).get(addr);
        }
        return null;
    }

    public static EC2Resolver createEC2Resolver(String csvFilename) {
        EC2Resolver reader = new EC2Resolver();

        try ( BufferedReader br = new BufferedReader(new FileReader(csvFilename)); ) {
            String line = null;
            while ((line = br.readLine()) != null) {  
                String[] split = line.split(EC2Resolver.DELIMETER);
                String ip = split[0], location = split[1];
                long address = IPAddress.fromString(ip);
                int bits = IPAddress.getBits(ip);
                reader.insert(bits, address, location);
            }  
        }
        catch (IOException e) {  
            System.out.printf("Error reading in csv file %s: %s\n", csvFilename, e.getMessage());
            return null;
        }

        return reader;
    }
}