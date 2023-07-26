package project.util;

public class RDNSRecord {
    private String name;
    private int nameTrueLength;
    private Short type;
    private Short recordClass;
    private Long ttl;
    private Short rDLength;
    private byte[] rData;
    private int length;
    private int originalOffset;

    public RDNSRecord(
        String name, 
        int nameTrueLength,
        Short type, 
        Short recordClass, 
        Long ttl,
        Short rdlength,
        byte[] rdata,
        int length,
        int originalOffset
    ) {
        this.name = name;
        this.type = type;
        this.recordClass = recordClass;
        this.ttl = ttl;
        this.rDLength = rdlength;
        this.rData = rdata;
        this.length = length;
        this.originalOffset = originalOffset;
        this.nameTrueLength = nameTrueLength;
    }

    public String getName() {
        return name;
    }

    public Short getType() {
        return type;
    }

    public int getNameTrueLength() {
        return nameTrueLength;
    }

    public Short getRecordClass() {
        return recordClass;
    }

    public Long getTtl() {
        return ttl;
    }

    public Short getrDLength() {
        return rDLength;
    }

    public byte[] getrData() {
        return rData;
    }

    public int getLength() {
        return length;
    }

    public int getOriginalOffset() {
        return originalOffset;
    }
}
