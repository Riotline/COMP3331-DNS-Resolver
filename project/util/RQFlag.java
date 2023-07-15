package project.util;

/**
 * Query Flags for DNS Request
 */
public enum RQFlag {
    QR(0),
    AA(5),
    TC(6),
    RD(7),
    RA(8);

    private final Integer index;

    private RQFlag(Integer index) {
        this.index = index;
    }

    public Integer getIndex() {
        return index;
    }
}
