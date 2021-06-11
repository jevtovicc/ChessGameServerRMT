package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    public static List<ClientThread> onlinePlayers = new ArrayList<>();

    public static void main(String[] args) {

        final int PORT = 6666;

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);

            while (true) {
                System.out.println("Waiting for connection...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected!");

                ClientThread ct = new ClientThread(clientSocket);
                onlinePlayers.add(ct);
                ct.start();
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

}

