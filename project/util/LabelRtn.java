package project.util;

public class LabelRtn {
    private String string = new String();
    private int offset = 0;

    public LabelRtn(String string, int offset) {
        this.string = string;
        this.offset = offset;
    }

    public String getLabelString() {
        return this.string;
    }

    public int getOffset() {
        return this.offset;
    }
}
