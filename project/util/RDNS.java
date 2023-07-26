package project.util;

import java.nio.ByteBuffer;
import project.util.RDebug.DEBUG_LEVEL;

public class RDNS {
    public static String byteToBinary(byte[] bytes) {
        String inBinary = "";
        for (int i = 0; i < bytes.length; i++) {
            inBinary += String.format(
                "%8s", 
                Integer.toBinaryString(bytes[i] & 0xFF)
            ).replace(' ', '0');
        }
        return inBinary;
    }

    // Checking specific bits (like bitflags)
    public static Boolean isBit(byte bByte, int pos) {
        return (((bByte >> pos) & 1) == 1);
    }

    // Convert a byte array of size 2 into a Short
    public static short responseToShort(byte[] response, int responseOffset) {
        byte[] responseBytes = new byte[2];
        System.arraycopy(
            response, responseOffset, 
            responseBytes, 0, 
            2
        );
        ByteBuffer responseBuf = ByteBuffer.wrap(responseBytes);
        return responseBuf.getShort();
    }

    // Used for unsigned int as a long
    public static Long responseToUnsignedInt(byte[] response, int responseOffset) {
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

    // Bytes to an string IP representation
    public static String bytesToIP(byte[] bytes) {
        return (bytes[0] & 0xff) + "." + (bytes[1] & 0xff) + "." + (bytes[2] & 0xff) + "." + (bytes[3] & 0xff);
    }

    public static String typeIndexToType(int typeIndex) {
        switch (typeIndex) {
            case 1: return "A";
            case 2: return "NS";
            case 28: return "AAAA";
            default: return "?";
        }
    }

    public static int getQtyOfRecords(byte[] response, int offset) {
        byte[] responseQtyBytes = new byte[2];
        System.arraycopy(
            response, offset, 
            responseQtyBytes, 0, 
            2
        );
        ByteBuffer responseQtyBuf = ByteBuffer.wrap(responseQtyBytes);
        return ((Short) responseQtyBuf.getShort()).intValue();
    }

    // Does the label and message compression reading
    public static LabelRtn readLabel(byte[] bytes, int offset) {
        // Is this label just a pointer.
        // If so, return what is at that pointer
        if (isLabelPtrByte(bytes[offset])) {
            byte[] labelPtrBytes = {bytes[offset], bytes[offset+1]};
            labelPtrBytes[0] = (byte) (labelPtrBytes[0] & 0b00111111);
            ByteBuffer labelPtrBuf = ByteBuffer.wrap(labelPtrBytes);
            Short labelPtr = labelPtrBuf.getShort();
            RDebug.print(DEBUG_LEVEL.DEBUG, "LP: %d", labelPtr);
            LabelRtn lRtn = readLabel(bytes, labelPtr);
            return new LabelRtn(lRtn.getLabelString(), offset+2);
        }

        int currentOffset = offset;
        String labelString = new String();
        while (true) {        
            // Collect all the parts of the label    
            int sizeOfLabel = Byte.toUnsignedInt(bytes[currentOffset++]);
            RDebug.print(DEBUG_LEVEL.DEBUG, "LS: %d", sizeOfLabel);
            for (int i = 0; i < sizeOfLabel; i++) {
                labelString += (char) bytes[currentOffset++];
                RDebug.print(DEBUG_LEVEL.DEBUG, "L (%d): %s", currentOffset-1, labelString);
            }

            // If a label ends with a zero octet, thats it
            // Check if Sequence of Labels ending with Pointer
            // Otherwise, continue
            if (Byte.toUnsignedInt(bytes[currentOffset]) == 0) {
                currentOffset++;
                break;
            } else if (isLabelPtrByte(bytes[currentOffset])) {
                labelString += '.';
                RDebug.print(DEBUG_LEVEL.DEBUG, "LABEL PTR FOUND");
                LabelRtn lRtn = readLabel(bytes, currentOffset);
                currentOffset += 2;
                labelString += lRtn.getLabelString();
                break;
            } else {
                labelString += '.';
            }
            RDebug.print(DEBUG_LEVEL.DEBUG, "Label: %s", labelString);
        }
        return new LabelRtn(labelString, currentOffset);
    }

    // Does the byte point to another location
    public static boolean isLabelPtrByte(byte labelByte) {
        return (((labelByte >> 6) & 0b11) >= 3);
    }
}
