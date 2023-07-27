package project.util;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import project.util.RDebug.DEBUG_LEVEL;

public class RDNSResponse {
    private byte[] data;
    private Short identifier;
    private DatagramPacket packet;
    
    private int questionCount;
    private int answerCount;
    private int authorityCount;
    private int additionalCount;
    private int totalRecordsCount;
    private ArrayList<RDNSRecord> questionRecords = new ArrayList<RDNSRecord>();
    private ArrayList<RDNSRecord> answerRecords = new ArrayList<RDNSRecord>();
    private ArrayList<RDNSRecord> authorityRecords = new ArrayList<RDNSRecord>();
    private ArrayList<RDNSRecord> additionalRecords = new ArrayList<RDNSRecord>();

    private RDNSResponse(DatagramPacket packet) {
        this.packet = packet;
    }

    public byte[] getData() {
        return data;
    }

    public Short getIdentifier() {
        return identifier;
    }

    public DatagramPacket getPacket() {
        return packet;
    }

    public int getTotalRecordsCount() {
        return totalRecordsCount;
    }

    public ArrayList<RDNSRecord> getQuestionRecords() {
        return questionRecords;
    }

    public ArrayList<RDNSRecord> getAnswerRecords() {
        return answerRecords;
    }

    public ArrayList<RDNSRecord> getAuthorityRecords() {
        return authorityRecords;
    }

    public ArrayList<RDNSRecord> getAdditionalRecords() {
        return additionalRecords;
    }

    public static RDNSResponse parse(DatagramPacket packet) {
        RDNSResponse resp = new RDNSResponse(packet);
        resp.data = packet.getData();

        // Response Parsing
        byte[] dnsIdBytes = new byte[2];
        System.arraycopy(
            resp.data, 0, 
            dnsIdBytes, 0, 
            2
        );
        resp.identifier = ByteBuffer.wrap(
            resp.data, 0, 2
        ).getShort();

        if (!RDNS.isBit(resp.data[2], 7)) {
            RDebug.print(DEBUG_LEVEL.WARNING, 
                "QR flag returned as a query not as a response."
            );
            throw new RuntimeException(
                "Packet identified as a query could not be parsed as a response"
            );
        }

        // For getting count of records
        resp.questionCount = RDNS.getQtyOfRecords(resp.data, 4);
        resp.answerCount = RDNS.getQtyOfRecords(resp.data, 6);
        resp.authorityCount = RDNS.getQtyOfRecords(resp.data, 8);
        resp.additionalCount = RDNS.getQtyOfRecords(resp.data, 10);

        resp.totalRecordsCount = resp.questionCount 
                                + resp.answerCount 
                                + resp.authorityCount 
                                + resp.additionalCount;

        // if (responseTotalRecords > 0) {
        //     System.out.printf("%28s %6s %6s %8s %10s %28s\n",
        //         "Name",
        //         "Type",
        //         "Class",
        //         "TTL",
        //         "RDLength",
        //         "RData"
        //     );
        // }

        // Question Record Name
        Integer responseOffset = 12;
        for (int i = 0; i < resp.totalRecordsCount; i++) {
            boolean questionRecord = i < resp.questionCount;
            RDNSRecord record = recordParse(resp, questionRecord, responseOffset);
            responseOffset += record.getLength();
            if (questionRecord) 
                resp.questionRecords.add(record);
            else if (i < resp.questionCount + resp.answerCount) 
                resp.answerRecords.add(record);
            else if (i < resp.questionCount + resp.answerCount + resp.authorityCount) 
                resp.authorityRecords.add(record);
            else 
                resp.additionalRecords.add(record);
        }

        return resp;
    } 

    private static RDNSRecord recordParse(
        RDNSResponse resp, boolean questionRecord, int offset
    ) {
        int currentOffset = offset;
        
        // Resource Record Name
        LabelRtn labelRtn = RDNS.readLabel(resp.data, currentOffset);
        currentOffset = labelRtn.getOffset();
        String fullLabel = labelRtn.getLabelString();

        // Response TYPE
        Short responseType  = RDNS.responseToShort(resp.data, currentOffset);
        currentOffset += 2;

        // Response CLASS
        Short responseClass = RDNS.responseToShort(resp.data, currentOffset);
        currentOffset += 2;        

        Long responseTTL;
        Short responseRDLen;
        byte[] responseRDataBytes = null;
        if (!questionRecord) {
            // Response TTL
            responseTTL    = RDNS.responseToUnsignedInt(resp.data, currentOffset);
            currentOffset += 4;

            // Response RDLength
            responseRDLen = RDNS.responseToShort(resp.data, currentOffset);
            currentOffset += 2;
            
            // Response RData
            responseRDataBytes = new byte[responseRDLen];
            System.arraycopy(
                resp.data, currentOffset, 
                responseRDataBytes, 0, 
                responseRDLen
            );
            currentOffset += responseRDLen;
        } else {
            responseTTL = -1L;
            responseRDLen = -1;
        }

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
