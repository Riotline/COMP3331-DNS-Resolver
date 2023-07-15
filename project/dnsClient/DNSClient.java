package project.dnsClient;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import project.util.RDebug;
import project.util.RQFlag;

public class DNSClient {
    public static void main(String[] args) throws Exception
    {
        // Get command line arguments.
        if (args.length != 3) {
            System.out.println("Required arguments: ip, port, A name");
            return;
        }

        InetAddress resolverIP = InetAddress.getByName(args[0]);
        Integer resolverPort = Integer.parseInt(args[1]);
        String recordName = args[2];

        DatagramSocket clientSocket = new DatagramSocket();

        byte[] queryId = new byte[2];
        // Generation of Random Bytes via the java util Random
        // Relevant method info used from tutorialspoint
        Random random = new Random();
        random.nextBytes(queryId);

        // Query Flags startingf from 0 index
        // QR (1), OPCode (4), AA (1), TC (1), RD (1), RA (1)
        // ZERO (3), rCode (4)
        BitSet queryFlags = new BitSet(16);
        queryFlags.set(RQFlag.QR.getIndex(), false);

        byte[] queryHeader = new byte[12];
        queryHeader = queryId;
        queryHeader[2] = queryFlags.toByteArray()[0];
        queryHeader[3] = queryFlags.toByteArray()[1];

    }
}
