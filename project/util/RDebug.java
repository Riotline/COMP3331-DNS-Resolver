package project.util;

public class RDebug {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String CRLF = "\r\n";
    private static DEBUG_LEVEL debugLevel = DEBUG_LEVEL.NONE;

    public enum DEBUG_LEVEL {
        NONE,
        INFO,
        WARNING,
        DEBUG
    }

    public static void setDebugLevel(DEBUG_LEVEL level) {
        debugLevel = level;
    }

    public static DEBUG_LEVEL toDLevel(String level) {
        switch (level.toLowerCase()) {
            case "none"     :   return DEBUG_LEVEL.NONE;  
            case "info"     :   return DEBUG_LEVEL.INFO;
            case "warning"  :   return DEBUG_LEVEL.WARNING;
            case "debug"    :   return DEBUG_LEVEL.DEBUG;        
            default:            return DEBUG_LEVEL.NONE;  
        }
    }

    public static void printDebug(DEBUG_LEVEL level, String string, Object... args) {
        if (debugLevel == DEBUG_LEVEL.NONE) return;
        else if (debugLevel == DEBUG_LEVEL.DEBUG) {
            string = String.format(string, args);
            System.out.printf(
                "  %s[%s]%s %s\n", 
                ANSI_PURPLE,
                level, 
                ANSI_RESET, 
                string,
                args
            );
        }
    }
}
