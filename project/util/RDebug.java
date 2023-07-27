package project.util;

public class RDebug {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_RED = "\u001B[31m";
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

    private static int DLevelToInt(DEBUG_LEVEL dLevel) {
        switch (dLevel) {
            case INFO       :   return 1;
            case WARNING    :   return 2;
            case DEBUG      :   return 3;        
            default         :   return 0;  
        }
    }

    public static void print(DEBUG_LEVEL level, String string, Object... args) {
        if (DLevelToInt(debugLevel) < DLevelToInt(level)) return;
        else {
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
