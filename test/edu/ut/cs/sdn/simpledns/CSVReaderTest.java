package edu.ut.cs.sdn.simpledns;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class CSVReaderTest {
    static final String TEST_FILE = "test.csv";
    private static void createFile(String[] lines) throws IOException {
        File out = new File(CSVReaderTest.TEST_FILE);
        if (!out.createNewFile()) return;
        out.deleteOnExit();

        try ( FileWriter fw = new FileWriter(out) ) {
            Arrays.stream(lines).iterator().forEachRemaining((line) -> {
                try {
                    fw.write(line + '\n');
                } catch (IOException ignored) {
                }
            });
        }
    }

    @BeforeClass
    public static void createTest() {
        try {
            CSVReaderTest.createFile(new String[]{"1.2.3.4/19,A", "2.2.3.255/8,B", "2.2.255.255/16,C"});
        } catch (IOException ignored) {
            Assert.fail();
        }
    }

    @Test
    public void testCSVParse() {
        EC2Resolver reader = EC2Resolver.createEC2Resolver(CSVReaderTest.TEST_FILE);
        Assert.assertNotNull("reader should not be null", reader);
    }

    @Test
    public void testBasicLookup() {
        EC2Resolver reader = EC2Resolver.createEC2Resolver(CSVReaderTest.TEST_FILE);
        Assert.assertNotNull("reader should not be null", reader);
        String read = reader.get("1.2.3.4");
        String expected = "A";
        Assert.assertEquals(expected, read);
        read = reader.get("1.2.3.0");
        Assert.assertEquals(expected, read);
        read = reader.get("1.2.3.255");
        Assert.assertEquals(expected, read);
    }

    @Test
    public void testLongestMatch() {
        EC2Resolver reader = EC2Resolver.createEC2Resolver(CSVReaderTest.TEST_FILE);
        Assert.assertNotNull("reader should not be null", reader);
        String read = reader.get("2.2.3.0");
        String expected = "B";
        Assert.assertEquals(expected, read);
        read = reader.get("2.2.2.0");
        expected = "C";
        Assert.assertEquals(expected, read);
    }
}
