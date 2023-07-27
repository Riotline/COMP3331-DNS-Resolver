package project.dnsResolver;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import project.util.RDNSPacket;
import project.util.RDNSQuery;
import project.util.RDebug;
import project.util.RDebug.DEBUG_LEVEL;

public class DNSQuery implements Runnable {
    protected static ArrayList<RDNSQuery> queries = new ArrayList<RDNSQuery>();

    private RDNSQuery mainQuery;

    public DNSQuery(DatagramSocket socket, RDNSQuery packet) {
        this.mainQuery = packet;
    }

    @Override
    public void run() {
        RDebug.print(DEBUG_LEVEL.INFO, 
            "New Query Received (Thread: %d): %s", 
            Thread.currentThread().getId(), new String(mainQuery.getData()));
        while(true) {
            continue;
        }
    }
    
}
