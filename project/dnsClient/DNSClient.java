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
import project.util.RDNSQuery;
import project.util.RDNSQueryBuilder;
import project.util.RDNSRecord;
import project.util.RDNSResponse;

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

        RDNSQueryBuilder newQuery = new RDNSQueryBuilder();
        newQuery.addQuestion(recordName, 1, 1);
        RDebug.print(DEBUG_LEVEL.DEBUG, 
            "%s", 
            newQuery.getQuestionRecords()
        );
        DatagramPacket sendPacket = newQuery.compose(resolverIP, resolverPort);
        RDNSQuery query = RDNSQuery.parse(sendPacket);

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

        if (!(RDNS.byteToBinary(responseId).equals(RDNS.byteToBinary(query.getIdentifier())))) {
            RDebug.print(DEBUG_LEVEL.WARNING, 
                "Response ID (%s) mismatch to Query ID (%s)",
                RDNS.byteToBinary(responseId), RDNS.byteToBinary(query.getIdentifier())
            ); return;
        } else if (!RDNS.isBit(receivePacketBytes[2], 7)) {
            RDebug.print(DEBUG_LEVEL.WARNING, 
                "QR flag returned as a query not as a response."
            ); return;
        }

        if (RDNS.isBit(receivePacketBytes[3], 1)) {
            RDebug.print(DEBUG_LEVEL.NONE, 
                "%s [!] Server returned a server failure error [!]", RDebug.ANSI_RED
            );
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

        RDNSResponse resp = RDNSResponse.parse(receivePacket);

        System.out.printf("~~~ QUESTION RECORDS ~~~\n");
        for (int i = 0; i < resp.getQuestionRecords().size(); i++) {
            RDNSRecord record = resp.getQuestionRecords().get(i);
            System.out.printf("%28s %6s %6d\n",
                record.getName(), 
                RDNS.typeIndexToType(record.getType()), 
                record.getRecordClass()
            );
        }

        RDebug.print(DEBUG_LEVEL.DEBUG, "%s", resp.getAnswerRecords());

        System.out.printf("~~~ ANSWER RECORDS ~~~\n");
        for (int i = 0; i < resp.getAnswerRecords().size(); i++) {
            RDNSRecord record = resp.getAnswerRecords().get(i);
            System.out.printf("%28s %6s %6d %8d %10d %28s\n",
                record.getName(), 
                RDNS.typeIndexToType(record.getType()), 
                record.getRecordClass(),
                record.getTtl(),
                record.getrDLength(),
                RDNS.bytesToIP(record.getrData())
            );
        }

        System.out.printf("~~~ AUTHORITY RECORDS ~~~\n");
        for (int i = 0; i < resp.getAuthorityRecords().size(); i++) {
            RDNSRecord record = resp.getAuthorityRecords().get(i);
            System.out.printf("%28s %6s %6d %8d %10d %28s\n",
                record.getName(), 
                RDNS.typeIndexToType(record.getType()), 
                record.getRecordClass(),
                record.getTtl(),
                record.getrDLength(),
                new String(RDNS.readLabel(
                    receivePacketBytes, 
                    record.getOriginalOffset() + record.getNameTrueLength() + 10
                ).getLabelString())
            );
        }

        System.out.printf("~~~ ADDITIONAL RECORDS ~~~\n");
        for (int i = 0; i < resp.getAdditionalRecords().size(); i++) {
            RDNSRecord record = resp.getAdditionalRecords().get(i);
            String recordType = RDNS.typeIndexToType(record.getType());
            String rdataoutput = new String();
            if (recordType == "A") {
                rdataoutput = RDNS.bytesToIP(record.getrData());
            } else if (recordType == "AAAA") {
                rdataoutput = RDNS.bytesToIPv6(record.getrData());
            } else rdataoutput = record.getrData().toString();
            System.out.printf("%28s %6s %6d %8d %10d %28s\n",
                record.getName(), 
                recordType, 
                record.getRecordClass(),
                record.getTtl(),
                record.getrDLength(),
                rdataoutput
            );
        }
    }
}
