package project.util;

public class RDebug {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String CRLF = "\r\n";

    public enum DEBUG_LEVEL {
        NONE,
        INFO,
        WARNING,
        DEBUG
    }

    public static void printDebug(DEBUG_LEVEL debug, String string) {
        if (debug == DEBUG_LEVEL.NONE) return;
        System.out.printf(
            "  %s[%s]%s %s\n", 
            ANSI_PURPLE,
            debug, 
            ANSI_RESET, 
            string
        );
    }
}
