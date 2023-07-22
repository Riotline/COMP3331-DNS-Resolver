package project.dnsClient;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import project.util.RDebug;
import project.util.RQFlag;
import project.util.RDebug.DEBUG_LEVEL;

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
        RDebug.print(DEBUG_LEVEL.DEBUG, "Q (b): %s", byteToBinary(query));

        DatagramPacket sendPacket = new DatagramPacket(
            query, query.length, resolverIP, resolverPort
        );
        clientSocket.send(sendPacket);

        byte[] receiveData = new byte[256];
        DatagramPacket receivePacket = new DatagramPacket(
            receiveData, receiveData.length
        );
        clientSocket.receive(receivePacket);
        byte[] receivePacketBytes = receivePacket.getData();

        RDebug.print(DEBUG_LEVEL.DEBUG, 
            "REPLY: %s", new String(receivePacketBytes)
        );

        String replyInBinary = byteToBinary(receivePacketBytes);
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

        if (!(byteToBinary(responseId).equals(byteToBinary(queryId)))) {
            RDebug.print(DEBUG_LEVEL.WARNING, 
                "Response ID (%s) mismatch to Query ID (%s)",
                byteToBinary(responseId), byteToBinary(queryId)
            ); return;
        } else if (!isBit(receivePacketBytes[2], 7)) {
            RDebug.print(DEBUG_LEVEL.WARNING, 
                "QR flag returned as a query not as a response."
            ); return;
        }

        // For getting count of answers for the use in looping
        byte[] responseAnswerQtyBytes = new byte[2];
        System.arraycopy(
            receivePacketBytes, 6, 
            responseAnswerQtyBytes, 0, 
            2
        );
        ByteBuffer responseAnswerQtyBuf = ByteBuffer.wrap(responseAnswerQtyBytes);
        Integer responseAnswerQty = (
            (Short) responseAnswerQtyBuf.getShort()
        ).intValue();

        if (responseAnswerQty > 0) {
            System.out.printf("%6s %6s %8s %10s %16s\n",
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
            byteToBinary(questionLabelOutput.toByteArray())
        );

        // Question TYPE and CLASS
        responseOffset += 4;

        for (int i = 0; i < responseAnswerQty; i++) {
            // Resource Record Name
            ByteArrayOutputStream responseLabelOutput = new ByteArrayOutputStream();
            while (true) {
                Integer labelLength = Byte.toUnsignedInt(receivePacketBytes[responseOffset++]);
                if (labelLength == 0) break;
                if (labelLength >= 192) {
                    responseLabelOutput.write(questionLabelOutput.toByteArray());
                    responseOffset += 1;
                    break;
                }
                for (int j = 0; j < labelLength; j++) {
                    responseLabelOutput.write(receivePacketBytes[responseOffset++]);
                }
                if (Byte.toUnsignedInt(receivePacketBytes[responseOffset]) != 0) {
                    responseLabelOutput.write('.');
                }
            }
            RDebug.print(DEBUG_LEVEL.DEBUG, 
                "RLO: %s (%d)", 
                responseLabelOutput.toString(), responseAnswerQty
            );

            // Response TYPE
            Short responseType  = responseToShort(receivePacketBytes, responseOffset);
            responseOffset += 2;

            // Response CLASS
            Short responseClass = responseToShort(receivePacketBytes, responseOffset);
            responseOffset += 2;

            // Response TTL
            Long responseTTL    = responseToLong(receivePacketBytes, responseOffset);
            responseOffset += 4;

            // Response RDLength
            Short responseRDLen = responseToShort(receivePacketBytes, responseOffset);
            responseOffset += 2;

            RDebug.print(DEBUG_LEVEL.DEBUG, 
                "RData Length: %d",
                responseRDLen
            );

            // Response RData
            byte[] responseRDataBytes = new byte[responseRDLen];
            System.arraycopy(
                receivePacketBytes, responseOffset, 
                responseRDataBytes, 0, 
                responseRDLen
            );
            String responseRData = bytesToIP(responseRDataBytes);
            responseOffset += responseRDLen;
            System.out.printf("%6d %6d %8d %10d %16s\n", 
                responseType, 
                responseClass,
                responseTTL,
                responseRDLen,
                responseRData
            );
        }
    }

    private static String byteToBinary(byte[] bytes) {
        String inBinary = "";
        for (int i = 0; i < bytes.length; i++) {
            inBinary += String.format(
                "%8s", 
                Integer.toBinaryString(bytes[i] & 0xFF)
            ).replace(' ', '0');
        }
        return inBinary;
    }

    private static Boolean isBit(byte bByte, int pos) {
        return (((bByte >> pos) & 1) == 1);
    }

    private static short responseToShort(byte[] response, int responseOffset) {
        byte[] responseBytes = new byte[2];
        System.arraycopy(
            response, responseOffset, 
            responseBytes, 0, 
            2
        );
        ByteBuffer responseBuf = ByteBuffer.wrap(responseBytes);
        return responseBuf.getShort();
    }

    private static Long responseToLong(byte[] response, int responseOffset) {
        byte[] responseBytes = new byte[4];
        System.arraycopy(
            response, responseOffset, 
            responseBytes, 0, 
            4
        );
        ByteBuffer responseBuf = ByteBuffer.wrap(responseBytes);

        RDebug.print(DEBUG_LEVEL.DEBUG, 
            "TTL (b): %s", byteToBinary(responseBytes)
        );
        return (Long) (responseBuf.getInt() & 0xFFFFFFFFL);
    }

    private static String bytesToIP(byte[] bytes) {
        return (bytes[0] & 0xff) + "." + (bytes[1] & 0xff) + "." + (bytes[2] & 0xff) + "." + (bytes[3] & 0xff);
    }
}
