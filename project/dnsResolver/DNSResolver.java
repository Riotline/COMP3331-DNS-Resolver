import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import project.util.RDebug;

public class DNSResolver {
    public static void main(String[] args) throws Exception
    {
        // Get command line argument.
        if (args.length != 1) {
            System.out.println("Required arguments: port");
            return;
        }
        int port = Integer.parseInt(args[0]);

        ServerSocket mainSocket = new ServerSocket(port);

        // Processing Loop.
        while (true) {
            
        }
    }

    private static String findA(InetAddress name) {
        
    }

    // This calls the normal NStoIP if it isn't in the additional records
    private static String NStoIP(byte[] response, String name) {

    }

    // This does another query for the NS from scratch
    private static String NStoIP(String name) {

    }
}
