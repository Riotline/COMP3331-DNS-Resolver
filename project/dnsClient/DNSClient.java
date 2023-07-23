package project.dnsClient;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.file.*;

import project.util.LabelRtn;
import project.util.RDebug;
import project.util.RQFlag;
import project.util.RDebug.DEBUG_LEVEL;
import project.util.RDNS;

public class DNSClient {
    public static void main(String[] args) throws Exception
    {
        // Get command line arguments.
        if (args.length < 3) {
            System.out.println("Required arguments: resolver ip, resolver port, name");
            return;
        }

        // Arguments for resolver
        // Cant Use Below cus it is doing the job already
        if (!args[0].matches("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$")) {
            System.out.println("Invalid resolver IP.");
            return;
        }
        InetAddress resolverIP = InetAddress.getByName(args[0]);
        Integer resolverPort = Integer.parseInt(args[1]);
        String recordName = args[2];

        // Debugging Initialisation
        RDebug.setDebugLevel(
            args.length > 3 ? RDebug.toDLevel(args[3]) : DEBUG_LEVEL.NONE
        );
        RDebug.print(
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

        // Query number of questions
        BitSet queryQNo = new BitSet(16);
        queryQNo.set(0, true);

        // Query header construction
        byte[] queryHeader = new byte[12];
        System.arraycopy(
            queryId, 0, 
            queryHeader, 0, 
            queryId.length
        );
        System.arraycopy(
            queryFlags, 0, 
            queryHeader, queryId.length, 
            queryFlags.length
        );
        queryHeader[5] = queryQNo.toByteArray()[0];

        // Query question name
        String[] arrRecordName = recordName.split("\\.");
        Integer questionLength = 1 + arrRecordName.length;
        for (int i = 0; i < arrRecordName.length; i++) {
            questionLength += arrRecordName[i].length();
        }

        byte[] queryQuestionName = new byte[questionLength];
        Integer byteIndex = 0;
        for (int i = 0; i < arrRecordName.length; i++) {
            Integer labelLength = arrRecordName[i].length();
            queryQuestionName[byteIndex++] = labelLength.byteValue(); 
            
            for (int j = 0; j < labelLength; j++) {
                queryQuestionName[byteIndex++] = (byte) arrRecordName[i].charAt(j);
            }
        }
        queryQuestionName[byteIndex] = (byte) '\0';

        // Query Question Type and Class
        byte queryQType = 1;
        byte queryQClass = 1;

        // Query Question Construction
        byte[] queryQuestion = new byte[questionLength + 4];
        System.arraycopy(
            queryQuestionName, 0, 
            queryQuestion, 0, 
            queryQuestionName.length
        );
        
        queryQuestion[queryQuestionName.length] = (byte) '\0';
        queryQuestion[queryQuestionName.length + 1] = queryQType;
        queryQuestion[queryQuestionName.length + 2] = (byte) '\0';
        queryQuestion[queryQuestionName.length + 3] = queryQClass;

        byte[] query = new byte[queryHeader.length + queryQuestion.length];
        System.arraycopy(
            queryHeader, 0, 
            query, 0, 
            queryHeader.length
        );
        System.arraycopy(
            queryQuestion, 0, 
            query, queryHeader.length, 
            queryQuestion.length
        );

        RDebug.print(DEBUG_LEVEL.DEBUG, "Q: %s", new String(query));
        RDebug.print(DEBUG_LEVEL.DEBUG, "Q (b): %s", RDNS.byteToBinary(query));

        DatagramPacket sendPacket = new DatagramPacket(
            query, query.length, resolverIP, resolverPort
        );
        clientSocket.send(sendPacket);

        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(
            receiveData, receiveData.length
        );
        clientSocket.receive(receivePacket);
        byte[] receivePacketBytes = receivePacket.getData();

        RDebug.print(DEBUG_LEVEL.DEBUG, 
            "REPLY: %s", new String(receivePacketBytes)
        );

        String replyInBinary = RDNS.byteToBinary(receivePacketBytes);
        RDebug.print(DEBUG_LEVEL.DEBUG, 
            "REPLY (b) (L:%d): %s", receiveData.length, replyInBinary
        );

        // Response
        byte[] responseId = new byte[2];
        System.arraycopy(
            receivePacketBytes, 0, 
            responseId, 0, 
            2
        );

        if (!(RDNS.byteToBinary(responseId).equals(RDNS.byteToBinary(queryId)))) {
            RDebug.print(DEBUG_LEVEL.WARNING, 
                "Response ID (%s) mismatch to Query ID (%s)",
                RDNS.byteToBinary(responseId), RDNS.byteToBinary(queryId)
            ); return;
        } else if (!RDNS.isBit(receivePacketBytes[2], 7)) {
            RDebug.print(DEBUG_LEVEL.WARNING, 
                "QR flag returned as a query not as a response."
            ); return;
        }

        // For getting count of answers for the use in looping
        Integer responseAnswerQty = RDNS.getQtyOfRecords(
            receivePacketBytes, 6
        );

        Integer responseAuthorityQty = RDNS.getQtyOfRecords(
            receivePacketBytes, 8
        );

        Integer responseAdditionalQty = RDNS.getQtyOfRecords(
            receivePacketBytes, 10
        );

        Integer responseTotalRecords = 
                    responseAnswerQty 
                    + responseAuthorityQty
                    + responseAdditionalQty;

        RDebug.print(DEBUG_LEVEL.INFO, 
            "Total Record Count: %d.", responseTotalRecords
        );

        if (responseTotalRecords > 0) {
            System.out.printf("%28s %6s %6s %8s %10s %28s\n",
                "Name",
                "Type",
                "Class",
                "TTL",
                "RDLength",
                "RData"
            );
        }

        // Question Record Name
        Integer responseOffset = 12;
        ByteArrayOutputStream questionLabelOutput = new ByteArrayOutputStream();
        while (true) {
            Integer labelLength = Byte.toUnsignedInt(receivePacketBytes[responseOffset++]);
            if (labelLength == 0) break;
            for (int j = 0; j < labelLength; j++) {
                questionLabelOutput.write(receivePacketBytes[responseOffset++]);
            }
            if (Byte.toUnsignedInt(receivePacketBytes[responseOffset]) != 0) {
                questionLabelOutput.write('.');
            }
        }

        RDebug.print(DEBUG_LEVEL.DEBUG, 
            "QLO: %s\nQLO (b): %s", 
            questionLabelOutput.toString(), 
            RDNS.byteToBinary(questionLabelOutput.toByteArray())
        );

        // Question TYPE and CLASS
        responseOffset += 4;

        for (int i = 0; i < responseTotalRecords; i++) {
            // Resource Record Name
            LabelRtn labelRtn = RDNS.readLabel(receivePacketBytes, responseOffset);
            responseOffset = labelRtn.getOffset();
            String fullLabel = labelRtn.getLabelString();

            RDebug.print(DEBUG_LEVEL.DEBUG, 
                "RLO: %s (%d)", 
                fullLabel, responseTotalRecords
            );

            // Response TYPE
            Short responseType  = RDNS.responseToShort(receivePacketBytes, responseOffset);
            responseOffset += 2;

            // Response CLASS
            Short responseClass = RDNS.responseToShort(receivePacketBytes, responseOffset);
            responseOffset += 2;

            // Response TTL
            Long responseTTL    = RDNS.responseToUnsignedInt(receivePacketBytes, responseOffset);
            responseOffset += 4;

            // Response RDLength
            Short responseRDLen = RDNS.responseToShort(receivePacketBytes, responseOffset);
            responseOffset += 2;

            RDebug.print(DEBUG_LEVEL.DEBUG, 
                "Resp: %d %d %d %d",
                responseType,
                responseClass,
                responseTTL,
                responseRDLen
            );

            // Response RData
            byte[] responseRDataBytes = new byte[responseRDLen];
            System.arraycopy(
                receivePacketBytes, responseOffset, 
                responseRDataBytes, 0, 
                responseRDLen
            );

            String responseRData = new String();
            if (RDNS.typeIndexToType(responseType) != "NS") {
                responseRData = RDNS.bytesToIP(responseRDataBytes);
            } else {
                LabelRtn lRtn = RDNS.readLabel(receivePacketBytes, responseOffset);
                responseRData = new String(lRtn.getLabelString());
            }
            responseOffset += responseRDLen;
            System.out.printf("%28s %6s %6d %8d %10d %28s\n",
                fullLabel, 
                RDNS.typeIndexToType(responseType), 
                responseClass,
                responseTTL,
                responseRDLen,
                responseRData
            );
        }
    }
}
