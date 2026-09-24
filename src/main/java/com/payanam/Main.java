package com.payanam;

import com.payanam.server.PayanamServer;

public class Main {
    public static void main(String[] args) {
        int[] preferredPorts = { 8085, 8090, 8080, 8888, 9090 };
        if (args.length > 0) {
            try {
                preferredPorts = new int[] { Integer.parseInt(args[0]), 8085, 8090, 8080 };
            } catch (NumberFormatException ignored) {}
        }

        PayanamServer server = null;
        int activePort = -1;

        for (int p : preferredPorts) {
            try {
                server = new PayanamServer(p);
                server.start();
                activePort = p;
                break;
            } catch (java.net.BindException be) {
                System.out.println("[Notice] Port " + p + " is occupied, trying alternative port...");
            } catch (Exception e) {
                System.err.println("Fatal: Could not start Payanam server: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        if (server == null || activePort == -1) {
            System.err.println("[Error] Could not find an available port to bind Payanam server.");
            System.exit(1);
        }

        final PayanamServer runningServer = server;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nStopping Payanam server...");
            runningServer.stop();
            System.out.println("Payanam server stopped safely.");
        }));
    }
}
