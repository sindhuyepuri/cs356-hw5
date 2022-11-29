package edu.ut.cs.sdn.simpledns;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class CSVReader {
    public static final String DELIMETER = ",";
    // bit mask --> map(ip, loc)
    private Map<Integer, Map<Integer, String>> map;
    private CSVReader() {
        map = new TreeMap<>(); // look at most specific mask first
    }

    private void insert(int bits, int addr, String location) {
        if (!this.map.containsKey(bits))
            this.map.put(bits, new HashMap<>());
        Map<Integer, String> ipMap = this.map.get(bits);
        ipMap.put(addr, location);
    }

    public String get(int addr) {
        for (int mask : this.map.keySet()) {
            addr = (addr >> mask) << mask; // zero out lower bits
            if (this.map.get(mask).containsKey(addr))
                return this.map.get(mask).get(addr);
        }
        return null;
    }

    public static CSVReader createCSVReader(String csvFilename) {
        CSVReader reader = new CSVReader();

        try ( BufferedReader br = new BufferedReader(new FileReader(csvFilename)); ) {
            String line = null;
            while ((line = br.readLine()) != null) {  
                String[] split = line.split(CSVReader.DELIMETER);
                String ip = split[0], location = split[1];
                int address = IPAddress.fromString(ip);
                int bits = IPAddress.getBits(ip);
                reader.insert(bits, address, location);
            }  
        }
        catch (IOException e) {  
            System.out.printf("Error reading in csv file %s: %s\n", csvFilename, e.getMessage());
            return null;
        }

        return null;
    }
}