package project.dnsResolver;

import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import project.util.RDNSPacket;

public class DNSQuery implements Runnable {
    protected static ArrayList<DNSQuery> queries = new ArrayList<DNSQuery>();

    private DatagramPacket packet;

    public DNSQuery(ServerSocket socket, DatagramPacket packet) {
        this.packet = packet;
    }

    @Override
    public void run() {
        
    }
    
}
