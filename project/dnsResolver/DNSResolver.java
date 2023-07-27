package project.dnsResolver;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import project.util.RDebug;
import project.util.RDNS;
import project.util.RDNSPacket;
import project.util.RDNSQuery;
import project.util.RDebug.DEBUG_LEVEL;

public class DNSResolver {
    private static ArrayList<InetAddress> rootServers = new ArrayList<InetAddress>();
    protected static ArrayList<RDNSPacket> currentPackets = new ArrayList<RDNSPacket>();
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

        Thread receivingDaemon = new Thread(new ReceivingThread(serverSocket));
        receivingDaemon.setDaemon(true);
        receivingDaemon.start();
        // Processing Loop
        RDebug.print(DEBUG_LEVEL.DEBUG,
            "Main Thread ID: %d",
            Thread.currentThread().getId()
        );
        while (true) {
            continue;
        }

    }
}

class ReceivingThread implements Runnable {
    private DatagramSocket serverSocket;

    public ReceivingThread(DatagramSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        RDebug.print(DEBUG_LEVEL.DEBUG,
            "Packet Receiver Thread ID: %d",
            Thread.currentThread().getId()
        );
        byte[] receiveData = new byte[1024];
        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(
                receiveData, receiveData.length);
            try {
                serverSocket.receive(receivePacket);
            } catch (IOException e) {
                RDebug.print(DEBUG_LEVEL.DEBUG, 
                    "%s", e.getMessage());
                continue;
            }
            ByteBuffer packetId = ByteBuffer.wrap(
                receivePacket.getData(), 0, 2);
            RDebug.print(DEBUG_LEVEL.DEBUG, 
                "%d", packetId.getShort()
            );

            if (!RDNS.isBit(receivePacket.getData()[2], 7)) {
                RDNSQuery query = RDNSQuery.parse(receivePacket);
                if (!DNSQuery.queries
                            .stream()
                            .map((RDNSQuery q) -> {
                                return q.getIdentifier();})
                            .anyMatch((byte[] q) -> {
                                return Arrays.equals(q, query.getIdentifier());
                })) {
                    DNSQuery.queries.add(query);
                    DNSQuery resolverQuery = new DNSQuery(serverSocket, query);
                    Thread threadQuery = new Thread(resolverQuery);
                    threadQuery.setDaemon(true);
                    threadQuery.start();
                }
                RDebug.print(DEBUG_LEVEL.DEBUG,
                    "%s", DNSQuery.queries
                );
            }

            RDebug.print(DEBUG_LEVEL.DEBUG,
                "TC: %s", Thread.activeCount()
            );
        }
    }
    
}