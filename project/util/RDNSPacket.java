package project.util;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;

public class RDNSPacket {
    private ArrayList<RDNSPacket> newPackets = new ArrayList<RDNSPacket>();
    private Short identifier;
    private byte[] data;
    private DatagramPacket rawPacket;
    private Instant timestamp = Instant.now();

    public RDNSPacket(DatagramPacket packet, RDNSPacket parent) {
        RDNSPacket rP = new RDNSPacket(packet);
        parent.newPackets.add(rP);
    }

    public RDNSPacket(DatagramPacket packet) {
        this.rawPacket = packet;
        this.data = packet.getData();
        this.identifier = ByteBuffer.wrap(data, 0, 2).getShort();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public byte[] getData() {
        return data;
    }

    public Short getIdentifier() {
        return identifier;
    }

    public ArrayList<RDNSPacket> getNewPackets() {
        return newPackets;
    }
}
