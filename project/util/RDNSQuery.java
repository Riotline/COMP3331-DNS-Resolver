package project.util;

import java.net.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import project.util.RDNSResponse;
import project.util.RDebug.DEBUG_LEVEL;

public class RDNSQuery {
    private byte[] data;
    private byte[] identifier = new byte[2];
    private DatagramPacket packet;

    public byte[] getData() {
        return data;
    }

    public byte[] getIdentifier() {
        return identifier;
    }

    public DatagramPacket getPacket() {
        return packet;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    private int questionCount;
    private ArrayList<RDNSRecord> questionRecords = new ArrayList<RDNSRecord>();

    public ArrayList<RDNSRecord> getQuestions() {
        return questionRecords;
    }

    private RDNSQuery(DatagramPacket packet) {
        this.packet = packet;
    }

    public static RDNSQuery parse(DatagramPacket packet) {
        RDNSQuery query = new RDNSQuery(packet);
        query.data = packet.getData();

        // Query Parsing
        System.arraycopy(
            query.data, 0, 
            query.identifier, 0, 
            2
        );

        if (RDNS.isBit(query.data[2], 7)) {
            RDebug.print(DEBUG_LEVEL.WARNING, 
                "QR flag returned as a response not as a query."
            );
            throw new RuntimeException(
                "Packet identified as a response could not be parsed as a query"
            );
        }

        // For getting count of records
        query.questionCount = RDNS.getQtyOfRecords(query.data, 4);

        // Question Record Name
        Integer responseOffset = 12;
        for (int i = 0; i < query.questionCount; i++) {
            boolean questionRecord = i < query.questionCount;
            RDNSRecord record = recordParse(query, questionRecord, responseOffset);
            responseOffset += record.getLength();
            query.questionRecords.add(record);
        }

        return query;
    }

    private static RDNSRecord recordParse(
        RDNSQuery query, boolean questionRecord, int offset
    ) {
        int currentOffset = offset;
        
        // Resource Record Name
        LabelRtn labelRtn = RDNS.readLabel(query.data, currentOffset);
        currentOffset = labelRtn.getOffset();
        String fullLabel = labelRtn.getLabelString();

        // Response TYPE
        Short responseType  = RDNS.responseToShort(query.data, currentOffset);
        currentOffset += 2;

        // Response CLASS
        Short responseClass = RDNS.responseToShort(query.data, currentOffset);
        currentOffset += 2;        

        Long responseTTL;
        Short responseRDLen;
        byte[] responseRDataBytes = null;
        responseTTL = -1L;
        responseRDLen = -1;

        RDebug.print(DEBUG_LEVEL.DEBUG, 
            "Resp (%d->%d): %d %d %d %d",
            offset,
            currentOffset,
            responseType,
            responseClass,
            responseTTL,
            responseRDLen
        );

        RDNSRecord record = new RDNSRecord(
            fullLabel,
            labelRtn.getOffset() - offset, 
            responseType, 
            responseClass, 
            responseTTL, 
            responseRDLen, 
            responseRDataBytes,
            currentOffset - offset,
            offset
        );

        return record;
    }
}
