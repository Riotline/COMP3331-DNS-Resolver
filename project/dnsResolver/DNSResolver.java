import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import project.util.RDebug;

public class DNSResolver {
    public static void main(String[] args) throws Exception
    {
        // Get command line argument.
        if (args.length != 1) {
            System.out.println("Required arguments: port");
            return;
        }
        int port = Integer.parseInt(args[0]);

        ServerSocket mainSocket = new ServerSocket(port);

        // Processing Loop.
        while (true) {
            
        }
    }
}
