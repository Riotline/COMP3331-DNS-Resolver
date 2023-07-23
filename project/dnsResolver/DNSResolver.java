package project.dnsResolver;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import project.util.RDebug;
import project.util.RDNSPacket;
import project.util.RDebug.DEBUG_LEVEL;

public class DNSResolver {
    private static ArrayList<InetAddress> rootServers = new ArrayList<InetAddress>();
    private static ArrayList<RDNSPacket> currentPackets = new ArrayList<RDNSPacket>();
    public static void main(String[] args) throws Exception
    {
        // Get command line argument.
        if (args.length < 1) {
            System.out.println("Required arguments: port");
            return;
        }
        int port = Integer.parseInt(args[0]);

        // Debugging Initialisation
        RDebug.setDebugLevel(
            args.length > 1 ? RDebug.toDLevel(args[1]) : DEBUG_LEVEL.NONE
        );

        // Roots file parsing. (Currently hardcoded for A records)
        try {
            File namedRootFile = new File("project/dnsResolver/named.root");
            Scanner fileReader = new Scanner(namedRootFile);
            while (fileReader.hasNextLine()) {
                String fileLineData = fileReader.nextLine();
                if (fileLineData.startsWith(";")) continue;

                String[] fileSplit = fileLineData.split("\\s+");
                if (fileSplit[2].equals("A")) 
                    rootServers.add(InetAddress.getByName(fileSplit[3]));
            }
            RDebug.print(DEBUG_LEVEL.DEBUG, 
                "%s", rootServers
            );
            fileReader.close();
        } catch (FileNotFoundException e) {
            RDebug.print(DEBUG_LEVEL.WARNING,
                "Exception: %s", e
            );
        }

        DatagramSocket serverSocket = new DatagramSocket(port);
        RDebug.print(DEBUG_LEVEL.INFO, 
            "Running on port %d", serverSocket.getLocalPort()
        );

        byte[] receiveData = new byte[1024];
        byte[] sendData = new byte[1024];
        // Processing Loop
        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(
                receiveData, receiveData.length
            );
            break;
        }

    }
}
