package project.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Random;

import project.util.RDebug.DEBUG_LEVEL;

public class RDNSResponseBuilder {
    private byte[] data;
    private byte[] identifier = new byte[2];
    Random random = new Random();

    private int questionCount;
    private int answerCount;
    private int authorityCount;
    private int additionalCount;
    private int totalRecordsCount;
    private boolean error = false;
    private ArrayList<RDNSRecord> questionRecords = new ArrayList<RDNSRecord>();
    private ArrayList<RDNSRecord> answerRecords = new ArrayList<RDNSRecord>();
    private ArrayList<RDNSRecord> authorityRecords = new ArrayList<RDNSRecord>();
    private ArrayList<RDNSRecord> additionalRecords = new ArrayList<RDNSRecord>();

    public RDNSResponseBuilder() {
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

    public int getAnswerCount() {
        return answerCount;
    }
    
    public int getAuthorityCount() {
        return authorityCount;
    }

    public int getAdditionalCount() {
        return additionalCount;
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
    
    public void addAnswer(RDNSRecord answerRecord) {
        this.answerRecords.add(answerRecord);
        this.answerCount = this.answerRecords.size();
    }

    public void addAuthority(RDNSRecord authorityRecord) {
        this.authorityRecords.add(authorityRecord);
        this.authorityCount = this.authorityRecords.size();
    }

    public void addAdditional(RDNSRecord additionalRecord) {
        this.additionalRecords.add(additionalRecord);
        this.additionalCount = this.additionalRecords.size();
    }

    public void enableError() {
        error = true;
    }

    public DatagramPacket compose(InetAddress ip, int port) {
        byte[] respFlags = new byte[2];

        // resp header construction
        byte[] respHeader = new byte[12];
        System.arraycopy(
            identifier, 0, 
            respHeader, 0, 
            identifier.length
        );

        respFlags[0] |= 1 << 7;
        if (error) respFlags[1] |= 1 << 1;
        System.arraycopy(
            respFlags, 0, 
            respHeader, identifier.length, 
            respFlags.length
        );

        int totalQuestionLength = 0;
        int totalAnswerLength = 0;
        int totalAuthorityLength = 0;
        int totalAdditionalLength = 0;

        
        ByteArrayOutputStream respQuestions = new ByteArrayOutputStream();
        ByteArrayOutputStream respAnswers = new ByteArrayOutputStream();
        ByteArrayOutputStream respAuthorities = new ByteArrayOutputStream();
        ByteArrayOutputStream respAdditionals = new ByteArrayOutputStream();

        // Question Count
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.putShort((short) questionCount);
        System.arraycopy(
            buffer.array(), 0, 
            respHeader, 4, 
            2
        );

        // Answer Count
        buffer = ByteBuffer.allocate(2);
        buffer.putShort((short) answerCount);
        System.arraycopy(
            buffer.array(), 0, 
            respHeader, 6, 
            2
        );

        // Auth Count
        buffer = ByteBuffer.allocate(2);
        buffer.putShort((short) authorityCount);
        System.arraycopy(
            buffer.array(), 0, 
            respHeader, 8, 
            2
        );

        // Additional Count
        buffer = ByteBuffer.allocate(2);
        buffer.putShort((short) additionalCount);
        System.arraycopy(
            buffer.array(), 0, 
            respHeader, 10, 
            2
        );

        for (int i = 0; i < questionCount; i++) {
            // resp question name
            RDebug.print(DEBUG_LEVEL.DEBUG, 
                "%s", 
                questionRecords.get(i).getName()
            );
            String[] arrRecordName = questionRecords.get(i).getName().split("\\.");
            Integer questionLength = 1 + arrRecordName.length;
            for (int j = 0; j < arrRecordName.length; j++) {
                questionLength += arrRecordName[j].length();
            }

            byte[] respQuestionName = new byte[questionLength];
            Integer byteIndex = 0;
            for (int j = 0; j < arrRecordName.length; j++) {
                Integer labelLength = arrRecordName[j].length();
                respQuestionName[byteIndex++] = labelLength.byteValue(); 
                
                for (int k = 0; k < labelLength; k++) {
                    respQuestionName[byteIndex++] = (byte) arrRecordName[j].charAt(k);
                }
            }
            respQuestionName[byteIndex] = (byte) '\0';
            
            // resp Question Type and Class
            byte respQType = 1;
            byte respQClass = 1;

            // resp Question Construction
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try {
                byteStream.write(respQuestionName);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            byteStream.write(0);
            byteStream.write(respQType);
            byteStream.write(0);
            byteStream.write(respQClass);

            totalQuestionLength += byteStream.size();
            try {
                respQuestions.write(byteStream.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < answerCount; i++) {
            // Answer question name
            RDebug.print(DEBUG_LEVEL.DEBUG, 
                "%s", 
                answerRecords.get(i).getName()
            );
            RDNSRecord record = answerRecords.get(i);
            String[] arrRecordName = record.getName().split("\\.");
            Integer answerLength = 1 + arrRecordName.length;
            for (int j = 0; j < arrRecordName.length; j++) {
                answerLength += arrRecordName[j].length();
            }

            byte[] respAnswerName = new byte[answerLength];
            Integer byteIndex = 0;
            for (int j = 0; j < arrRecordName.length; j++) {
                Integer labelLength = arrRecordName[j].length();
                respAnswerName[byteIndex++] = labelLength.byteValue(); 
                
                for (int k = 0; k < labelLength; k++) {
                    respAnswerName[byteIndex++] = (byte) arrRecordName[j].charAt(k);
                }
            }
            respAnswerName[byteIndex] = (byte) '\0';
            
            // resp Answer Type and Class
            byte respQType = 1;
            byte respQClass = 1;

            // resp Answer Construction
            ByteArrayOutputStream respAnswer = new ByteArrayOutputStream();
            respAnswer.write(respAnswerName, 0, respAnswerName.length);
            respAnswer.write(0);
            respAnswer.write(respQType);
            respAnswer.write(0);
            respAnswer.write(respQClass);

            ByteBuffer ttlBuffer = ByteBuffer.allocate(4);
            ttlBuffer.putInt(record.getTtl().intValue());
            byte[] ttlBytes = ttlBuffer.array();
            try {
                respAnswer.write(ttlBytes);
            } catch (IOException e1) {
                RDebug.print(DEBUG_LEVEL.WARNING,
                    "%s", e1
                );
            }

            ByteBuffer rdlenBuffer = ByteBuffer.allocate(2);
            rdlenBuffer.putShort(record.getrDLength());
            byte[] rdlenBytes = rdlenBuffer.array();
            try {
                respAnswer.write(rdlenBytes);
            } catch (IOException e1) {
                RDebug.print(DEBUG_LEVEL.WARNING,
                    "%s", e1
                );
            }

            try {
                respAnswer.write(record.getrData());
            } catch (IOException e1) {
                RDebug.print(DEBUG_LEVEL.WARNING,
                    "%s", e1
                );
            }

            totalAnswerLength += respAnswer.size();
            try {
                respAnswers.write(respAnswer.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < authorityCount; i++) {
            // Authority name
            RDebug.print(DEBUG_LEVEL.DEBUG, 
                "%s", 
                authorityRecords.get(i).getName()
            );
            RDNSRecord record = authorityRecords.get(i);

            String[] arrRecordName = record.getName().split("\\.");
            Integer authorityLength = 1 + arrRecordName.length;
            for (int j = 0; j < arrRecordName.length; j++) {
                authorityLength += arrRecordName[j].length();
            }

            byte[] respAuthorityName = new byte[authorityLength];
            Integer byteIndex = 0;
            for (int j = 0; j < arrRecordName.length; j++) {
                Integer labelLength = arrRecordName[j].length();
                respAuthorityName[byteIndex++] = labelLength.byteValue(); 
                
                for (int k = 0; k < labelLength; k++) {
                    respAuthorityName[byteIndex++] = (byte) arrRecordName[j].charAt(k);
                }
            }
            respAuthorityName[byteIndex] = (byte) '\0';
            
            // resp Authority Type and Class
            byte respQType = 1;
            byte respQClass = 1;

            // resp Authority Construction
            ByteArrayOutputStream respAuthority = new ByteArrayOutputStream();
            respAuthority.write(respAuthorityName, 0, respAuthorityName.length);
            respAuthority.write(0);
            respAuthority.write(respQType);
            respAuthority.write(0);
            respAuthority.write(respQClass);

            ByteBuffer ttlBuffer = ByteBuffer.allocate(4);
            ttlBuffer.putInt(record.getTtl().intValue());
            byte[] ttlBytes = ttlBuffer.array();
            try {
                respAuthority.write(ttlBytes);
            } catch (IOException e1) {
                RDebug.print(DEBUG_LEVEL.WARNING,
                    "%s", e1
                );
            }

            ByteBuffer rdlenBuffer = ByteBuffer.allocate(2);
            rdlenBuffer.putShort(record.getrDLength());
            byte[] rdlenBytes = rdlenBuffer.array();
            try {
                respAuthority.write(rdlenBytes);
            } catch (IOException e1) {
                RDebug.print(DEBUG_LEVEL.WARNING,
                    "%s", e1
                );
            }

            try {
                respAuthority.write(record.getrData());
            } catch (IOException e1) {
                RDebug.print(DEBUG_LEVEL.WARNING,
                    "%s", e1
                );
            }

            totalAuthorityLength += respAuthority.size();
            try {
                respAuthorities.write(respAuthority.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < additionalCount; i++) {
            // Additional name
            RDebug.print(DEBUG_LEVEL.DEBUG, 
                "%s", 
                additionalRecords.get(i).getName()
            );
            RDNSRecord record = additionalRecords.get(i);

            String[] arrRecordName = record.getName().split("\\.");
            Integer additionalLength = 1 + arrRecordName.length;
            for (int j = 0; j < arrRecordName.length; j++) {
                additionalLength += arrRecordName[j].length();
            }

            byte[] respAdditionalName = new byte[additionalLength];
            Integer byteIndex = 0;
            for (int j = 0; j < arrRecordName.length; j++) {
                Integer labelLength = arrRecordName[j].length();
                respAdditionalName[byteIndex++] = labelLength.byteValue(); 
                
                for (int k = 0; k < labelLength; k++) {
                    respAdditionalName[byteIndex++] = (byte) arrRecordName[j].charAt(k);
                }
            }
            respAdditionalName[byteIndex] = (byte) '\0';
            
            // resp Additional Type and Class
            byte respQType = 1;
            byte respQClass = 1;

            // resp Additional Construction
            ByteArrayOutputStream respAdditional = new ByteArrayOutputStream();
            respAdditional.write(respAdditionalName, 0, respAdditionalName.length);
            respAdditional.write(0);
            respAdditional.write(respQType);
            respAdditional.write(0);
            respAdditional.write(respQClass);

            ByteBuffer ttlBuffer = ByteBuffer.allocate(4);
            ttlBuffer.putInt(record.getTtl().intValue());
            byte[] ttlBytes = ttlBuffer.array();
            try {
                respAdditional.write(ttlBytes);
            } catch (IOException e1) {
                RDebug.print(DEBUG_LEVEL.WARNING,
                    "%s", e1
                );
            }

            ByteBuffer rdlenBuffer = ByteBuffer.allocate(2);
            rdlenBuffer.putShort(record.getrDLength());
            byte[] rdlenBytes = rdlenBuffer.array();
            try {
                respAdditional.write(rdlenBytes);
            } catch (IOException e1) {
                RDebug.print(DEBUG_LEVEL.WARNING,
                    "%s", e1
                );
            }

            try {
                respAdditional.write(record.getrData());
            } catch (IOException e1) {
                RDebug.print(DEBUG_LEVEL.WARNING,
                    "%s", e1
                );
            }

            totalAdditionalLength += respAdditional.size();
            try {
                respAdditionals.write(respAdditional.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int totalTotal = totalQuestionLength + totalAnswerLength
                            + totalAuthorityLength + totalAdditionalLength;
        byte[] resp = new byte[respHeader.length + totalTotal];
        int totalAccumulative = 0;
        System.arraycopy(
            respHeader, 0, 
            resp, 0, 
            respHeader.length
        );
        totalAccumulative = respHeader.length;
        System.arraycopy(
            respQuestions.toByteArray(), 0, 
            resp, totalAccumulative, 
            totalQuestionLength
        );
        totalAccumulative += totalQuestionLength;
        System.arraycopy(
            respAnswers.toByteArray(), 0, 
            resp, totalAccumulative, 
            totalAnswerLength
        );
        totalAccumulative += totalAnswerLength;
        System.arraycopy(
            respAuthorities.toByteArray(), 0, 
            resp, totalAccumulative, 
            totalAuthorityLength
        );
        totalAccumulative += totalAuthorityLength;
        System.arraycopy(
            respAdditionals.toByteArray(), 0, 
            resp, respHeader.length, 
            totalAdditionalLength
        );

        return new DatagramPacket(
            resp, resp.length, ip, port
        );
    }
}
