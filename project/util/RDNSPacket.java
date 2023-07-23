package project.util;

import java.time.Instant;
import java.util.ArrayList;

public class RDNSPacket {
    private ArrayList<RDNSPacket> newPackets = new ArrayList<RDNSPacket>();
    private String identifier;
    private byte[] data;
    private Instant timestamp = Instant.now();

    public RDNSPacket(byte[] identifier, byte[] data) {
        this.identifier = identifier.toString();
        this.data = data;
    }

    public RDNSPacket(byte[] identifier, byte[] data, RDNSPacket parent) {
        RDNSPacket rPacket = new RDNSPacket(identifier, data);
        newPackets.add(rPacket);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public byte[] getData() {
        return data;
    }

    public String getIdentifier() {
        return identifier;
    }

    public ArrayList<RDNSPacket> getNewPackets() {
        return newPackets;
    }
}
