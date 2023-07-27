package project.dnsResolver;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import project.util.RDNS;
import project.util.RDNSPacket;
import project.util.RDNSQuery;
import project.util.RDNSQueryBuilder;
import project.util.RDNSRecord;
import project.util.RDNSResponse;
import project.util.RDNSResponseBuilder;
import project.util.RDebug;
import project.util.RDebug.DEBUG_LEVEL;

public class DNSQuery implements Runnable {
    protected static ArrayList<RDNSQuery> activequeries = new ArrayList<RDNSQuery>();
    protected static ArrayList<DNSQuery> resolvers = new ArrayList<DNSQuery>();

    private long startedTimestamp = System.currentTimeMillis();

    private RDNSQuery mainQuery;
    private byte[] expectedIdentifier;
    private DatagramSocket socket;
    protected SynchronousQueue<DatagramPacket> newPacket = new SynchronousQueue<>();
    private ArrayList<InetAddress> nameServers = DNSResolver.getRootServers();

    public DNSQuery(DatagramSocket socket, RDNSQuery packet) {
        this.mainQuery = packet;
        this.socket = socket;
        resolvers.add(this);
    }

    public byte[] getExpectedIdentifier() {
        return expectedIdentifier;
    }

    @Override
    public void run() {
        RDebug.print(DEBUG_LEVEL.INFO, 
            "New Query Received (Thread: %d): %s", 
            Thread.currentThread().getId(), new String(mainQuery.getData())
        );


        InetAddress nameServerToQuery = nameServers.get(0);
        int nameServerIndex = 0;
        RDNSQueryBuilder currentQuery = new RDNSQueryBuilder();
        for (int i = 0; i < mainQuery.getQuestionCount(); i++) {
            currentQuery.addQuestion(
                mainQuery.getQuestions().get(i).getName(),
                1, 1
            );
        }
        while (true) {
            if ((System.currentTimeMillis() - startedTimestamp) > 1500) {
                sendServerError();
                return;
            }

            expectedIdentifier = currentQuery.getIdentifier();
            RDebug.print(DEBUG_LEVEL.INFO, "ATTEMPT TO: %s", nameServerToQuery);
            DatagramPacket packetToSend = currentQuery.compose(nameServerToQuery, 53);
            try {
                socket.send(packetToSend);
            } catch (IOException e) {
                e.printStackTrace();
            }
            DatagramPacket receivedPacket = null;
                
            try {
                receivedPacket = newPacket.poll(400L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            if (receivedPacket != null) {
                RDNSResponse resp = RDNSResponse.parse(receivedPacket);
                if (resp.getAnswerRecords().size() > 0) {
                    // Send Response
                    // RDebug.print(DEBUG_LEVEL.INFO,
                    //     "FOUND ANSWER RECORD"
                    // );
                    RDNSResponseBuilder newResponse = new RDNSResponseBuilder();
                    newResponse.setIdentifier(mainQuery.getIdentifier());
                    
                    mainQuery.getQuestions().forEach(newResponse::addQuestion);
                    resp.getAnswerRecords().forEach(newResponse::addAnswer);
                    resp.getAuthorityRecords().forEach(newResponse::addAuthority);
                    // resp.getAdditionalRecords().forEach(newResponse::addAdditional);
                    
                    DatagramPacket dP = newResponse.compose(
                        mainQuery.getPacket().getAddress(), 
                        mainQuery.getPacket().getPort()
                    );

                    try {
                        socket.send(dP);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    DNSQuery.resolvers.remove(this);
                    return;
                } else if (resp.getAuthorityRecords().size() > 0) {
                    ArrayList<RDNSRecord> additionalRecords = resp.getAdditionalRecords();
                    ArrayList<InetAddress> addRecords = new ArrayList<>();
                    resp.getAuthorityRecords().forEach((RDNSRecord rec) -> {
                        if (RDNS.typeIndexToType(rec.getType()) == "NS") {
                            if (additionalRecords.size() != 0) {
                                additionalRecords.stream().filter((RDNSRecord r) -> {
                                    return r.getName().equals(RDNS.readLabel(
                                        resp.getData(),
                                        rec.getOriginalOffset() + rec.getNameTrueLength() + 10
                                    ).getLabelString());
                                }).map((RDNSRecord r) -> {
                                    try {
                                        return InetAddress.getByName(
                                            RDNS.bytesToIP(r.getrData()));
                                    } catch (UnknownHostException e) {
                                        e.printStackTrace();
                                        return null;
                                    }
                                }).forEach((InetAddress iA) -> {
                                    addRecords.add(iA);
                                });
                            } 
                            /*
                            else {
                                RDNSQueryBuilder newQuery = new RDNSQueryBuilder();
                                newQuery.addQuestion(new String(rec.getrData()), 1, 1);
                                byte[] nsId = newQuery.getIdentifier();

                                byte[] nsPacketBuf = new byte[1024];
                                DatagramPacket nsPacket = new DatagramPacket(
                                    nsPacketBuf, nsPacketBuf.length);
                                DatagramPacket nsPacketToSend = newQuery.compose(
                                    InetAddress.getLoopbackAddress(), 
                                    DNSResolver.getPort());

                                try {
                                    socket.send(nsPacketToSend);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    expectedIdentifier = nsId;
                                    nsPacket = newPacket.poll(1500L, 
                                        TimeUnit.MILLISECONDS);
                                } catch (InterruptedException e1) {
                                    // TODO Auto-generated catch block
                                    e1.printStackTrace();
                                }

                                if (nsPacket != null) {
                                    RDNSResponse respNs = RDNSResponse.parse(nsPacket);
                                    if (respNs.getAnswerRecords().size() != 0) {
                                        respNs.getAnswerRecords().forEach((RDNSRecord r) -> {
                                            try {
                                                addRecords.add(
                                                    InetAddress.getByAddress(r.getrData())
                                                );
                                            } catch (UnknownHostException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                    }
                                }
                            }*/
                        }
                    });
                    RDebug.print(DEBUG_LEVEL.INFO, 
                        "%s", 
                        nameServers
                    );
                    if (addRecords.size() != 0) {
                        RDebug.print(DEBUG_LEVEL.INFO, "%d", addRecords.size());
                        nameServers.addAll(0, addRecords);
                        nameServerIndex = 0;
                    }
                    nameServerToQuery = nameServers.get(nameServerIndex++);
                }
            } else {
                RDebug.print(DEBUG_LEVEL.DEBUG, "ATTEMPTING NEW NAME SERVER");

                if (nameServerIndex + 1 < nameServers.size())
                    nameServerToQuery = nameServers.get(++nameServerIndex);
                else {
                    sendServerError();
                }
            }
        }
    }

    private void sendServerError() {
        RDNSResponseBuilder newResponse = new RDNSResponseBuilder();
        newResponse.setIdentifier(mainQuery.getIdentifier());
        
        mainQuery.getQuestions().forEach(newResponse::addQuestion);
        // resp.getAdditionalRecords().forEach(newResponse::addAdditional);
        newResponse.enableError();
        DatagramPacket dP = newResponse.compose(
            mainQuery.getPacket().getAddress(), 
            mainQuery.getPacket().getPort()
        );

        try {
            socket.send(dP);
        } catch (IOException e) {
            e.printStackTrace();
        }
        DNSQuery.resolvers.remove(this);
        return;
    }
    
}
