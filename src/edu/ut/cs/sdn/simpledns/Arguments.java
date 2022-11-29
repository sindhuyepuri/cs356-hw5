package edu.ut.cs.sdn.simpledns;

public class Arguments {
    public static final String R = "-r";
    public static final String E = "-e";
    public final String rootServerIp;
    public final String ec2Csv;

    public static Arguments parseArguments(String[] args) {
        String rootServerIp = null, ec2Csv = null;
        for (int i = 0; i < args.length - 1; ++i) {
            switch (args[i]) {
                case R:
                    if (rootServerIp != null) {
                        System.err.print("<root-server-ip> is being defined twice\n");
                        usage();
                        return null;
                    }
                    rootServerIp = args[++i];
                    break;
                case E:
                    if (ec2Csv != null) {
                        System.err.print("<ec2-csv> is being defined twice\n");
                        usage();
                        return null;
                    }
                    ec2Csv = args[++i];
                    break;
                default:
                    System.err.printf("unrecognized argument %s\n", args[i]);
                    Arguments.usage();
                    return null;
            }
        }
        
        if (rootServerIp == null || ec2Csv == null) {
            System.err.print("missing some arguments\n");
            usage();
            return null;
        }
        return new Arguments(rootServerIp, ec2Csv);
    }

    private static void usage() {
        System.err.print("Usage: java -jar SimpleDNS.jar -r <root-server-ip> -e <ec2-csv>\n");
    }

    private Arguments(String rootServerIp, String ec2Csv) {
        this.rootServerIp = rootServerIp;
        this.ec2Csv = ec2Csv;
    }
}