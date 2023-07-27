package project.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

import project.util.RDebug.DEBUG_LEVEL;

public class RDNSQueryBuilder {
    private byte[] data;
    private byte[] identifier = new byte[2];
    Random random = new Random();

    private int questionCount = 0;
    private ArrayList<RDNSRecord> questionRecords = new ArrayList<RDNSRecord>();

    public RDNSQueryBuilder() {
        random.nextBytes(identifier);
    }

    public byte[] getIdentifier() {
        return identifier;
    }

    public void setIdentifier(byte[] identifier) {
        this.identifier = identifier;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public ArrayList<RDNSRecord> getQuestionRecords() {
        return questionRecords;
    }

    public void addQuestion(RDNSRecord questionRecord) {
        this.questionRecords.add(questionRecord);
        this.questionCount = this.questionRecords.size();
    }

    public void addQuestion(String name, int type, int recordClass) {
        addQuestion(
            new RDNSRecord(
                name, 
                name.length() + 2, 
                (short) type, 
                (short) recordClass,  
                name.length()
            )
        );
    }

    public DatagramPacket compose(InetAddress ip, int port) {
        byte[] queryFlags = new byte[2];

        // Query header construction
        byte[] queryHeader = new byte[12];
        System.arraycopy(
            identifier, 0, 
            queryHeader, 0, 
            identifier.length
        );
        System.arraycopy(
            queryFlags, 0, 
            queryHeader, identifier.length, 
            queryFlags.length
        );

        int totalQuestionLength = 0;
        
        ByteArrayOutputStream queryQuestions = new ByteArrayOutputStream();

        for (int i = 0; i < questionCount; i++) {
            // Question Count
            System.arraycopy(
                (short) questionCount, 0, 
                queryHeader, 5, 
                2
            );

            // Query question name
            String[] arrRecordName = questionRecords.get(i).getName().split("\\.");
            Integer questionLength = 1 + arrRecordName.length;
            for (int j = 0; i < arrRecordName.length; i++) {
                questionLength += arrRecordName[i].length();
            }

            byte[] queryQuestionName = new byte[questionLength];
            Integer byteIndex = 0;
            for (int j = 0; i < arrRecordName.length; i++) {
                Integer labelLength = arrRecordName[i].length();
                queryQuestionName[byteIndex++] = labelLength.byteValue(); 
                
                for (int k = 0; k < labelLength; k++) {
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

            totalQuestionLength += queryQuestion.length;
            try {
                queryQuestions.write(queryQuestion);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        byte[] query = new byte[queryHeader.length + totalQuestionLength];
        System.arraycopy(
            queryHeader, 0, 
            query, 0, 
            queryHeader.length
        );
        System.arraycopy(
            queryQuestions.toByteArray(), 0, 
            query, queryHeader.length, 
            totalQuestionLength
        );

        return new DatagramPacket(
            query, query.length, ip, port
        );
    }

    
}
