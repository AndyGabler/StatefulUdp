package com.gabler.udpmanager.app;

import com.gabler.udpmanager.client.UdpClient;
import com.gabler.udpmanager.server.UdpServer;

import java.io.IOException;
import java.net.SocketException;
import java.util.Scanner;

/**
 * Run the default UDP client and server. Useful for demo-ing and live testing.
 *
 * @author Andy Gabler
 */
public class Driver {

    /**
     * Basic entry point.
     * @param args Ignored.
     * @throws IOException If something goes wrong with the client or server.
     */
    public static void main(String[] args) throws IOException {
        final Scanner scanner = new Scanner(System.in);
        System.out.println("Enter either \"client\" or \"server\" to choose run mode.");

        final String runMode = scanner.nextLine();
        if (runMode.equalsIgnoreCase("server")) {
            runDefaultServer(scanner);
        } else if (runMode.equalsIgnoreCase("client")) {
            runDefaultClient(scanner);
        } else {
            System.out.println("Unknown run-mode: " + runMode);
        }
    }

    /**
     * Run the default UDP server implementation.
     *
     * @param scanner The user input scanner
     * @throws SocketException If the server cannot be made
     */
    private static void runDefaultServer(Scanner scanner) throws SocketException {
        System.out.println("Enter port number.");
        final int portNumber = Integer.parseInt(scanner.nextLine());

        final UdpServer server = new UdpServer(portNumber);
        server.setConfiguration(new DefaultUdpServerConfig());
        server.start();

        while (scanner.hasNextLine()) {
            final String input = scanner.nextLine();
            System.out.println("broadcasting: " + input);
            server.clientBroadcast(input);

            if (input.equalsIgnoreCase("quit")) {
                server.terminate();
                break;
            }
        }
    }

    /**
     * Run the default UDP client implementation.
     *
     * @param scanner The user input scanner
     * @throws IOException If the client has some kind of issue
     */
    private static void runDefaultClient(Scanner scanner) throws IOException {
        System.out.println("Enter hostname.");
        final String hostName = scanner.nextLine();

        System.out.println("Enter port number.");
        final int portNumber = Integer.parseInt(scanner.nextLine());

        final UdpClient client = new UdpClient(hostName, portNumber);
        client.setConfiguration(new DefaultUdpClientConfig());
        client.start();

        while (scanner.hasNextLine()) {
            final String input = scanner.nextLine();
            System.out.println("sending server message: " + input);
            client.sendMessageToServer(input);

            if (input.equalsIgnoreCase("quit")) {
                client.terminate();
                break;
            }
        }
    }
}
