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

    private static final String KEY_ID = "key1";

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
        server.addClientKey(KEY_ID, staticKey());
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
        client.setClientKey(KEY_ID, staticKey());
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

    /**
     * Static key. Meant to test encryption, not real security.
     *
     * @return Key for encryption
     */
    private static byte[] staticKey() {
        return new byte[]{0xb, 0x2d, 0x13, 0x3, 0x02, 0x22, 0x73, 0x23, 0x4a, 0x71, 0x56, 0x60, 0x67, 0x0a, 0x1f, 0x65};
    }
}
