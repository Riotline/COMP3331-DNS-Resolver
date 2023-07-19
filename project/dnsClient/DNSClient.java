package project.dnsClient;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import project.util.RDebug;
import project.util.RQFlag;
import project.util.RDebug.DEBUG_LEVEL;

public class DNSClient {
    public static void main(String[] args) throws Exception
    {
        // Get command line arguments.
        if (args.length < 3) {
            System.out.println("Required arguments: ip, port, A name");
            return;
        }

        // Arguments for resolver
        InetAddress resolverIP = InetAddress.getByName(args[0]);
        Integer resolverPort = Integer.parseInt(args[1]);
        String recordName = args[2];

        // Debugging Initialisation
        RDebug.setDebugLevel(
            args.length > 3 ? RDebug.toDLevel(args[3]) : DEBUG_LEVEL.NONE
        );
        RDebug.printDebug(
            DEBUG_LEVEL.INFO, 
            "REQ: %s:%d - %s", 
            resolverIP, 
            resolverPort, 
            recordName
        );

        // UDP Socket
        DatagramSocket clientSocket = new DatagramSocket();

        byte[] queryId = new byte[2];
        // Generation of Random Bytes via the java util Random
        // Relevant method info used from tutorialspoint
        Random random = new Random();
        random.nextBytes(queryId);

        // Query Flags startingf from 0 index
        // QR (1), OPCode (4), AA (1), TC (1), RD (1), RA (1)
        // ZERO (3), rCode (4)

        // BitSet queryFlags = new BitSet(16);
        // queryFlags.set(RQFlag.QR.getIndex(), false);

        byte[] queryFlags = new byte[2];

        // RDebug.printDebug(
        //     DEBUG_LEVEL.DEBUG, 
        //     "QF: %s %d", 
        //     queryFlags.toString(),
        //     queryFlags.length()
        // );

        // Query number of questions
        BitSet queryQNo = new BitSet(16);
        queryQNo.set(0, true);

        // Query header construction
        byte[] queryHeader = new byte[12];
        queryHeader[0] = queryId[0];
        queryHeader[1] = queryId[1];
        queryHeader[2] = queryFlags[0];
        queryHeader[3] = queryFlags[1];
        // queryHeader[4] = queryQNo.toByteArray()[0];
        queryHeader[5] = queryQNo.toByteArray()[0];

        // Query question name
        String[] arrRecordName = recordName.split("\\.");
        Integer questionLength = 1 + arrRecordName.length;
        for (int i = 0; i < arrRecordName.length; i++) {
            questionLength += arrRecordName[i].length();
        }

        byte[] queryQuestionName = new byte[questionLength];
        Integer bitIndex = 0;
        for (int i = 0; i < arrRecordName.length; i++) {
            Integer labelLength = arrRecordName[i].length();
            queryQuestionName[bitIndex++] = labelLength.byteValue(); 
            for (int j = 0; j < labelLength; j++) {
                queryQuestionName[bitIndex++] = (byte) arrRecordName[i].charAt(j);
            }
        }

        RDebug.printDebug(
            DEBUG_LEVEL.DEBUG, 
            "QQN: %s", 
            queryQuestionName.length
        );

    }
}
