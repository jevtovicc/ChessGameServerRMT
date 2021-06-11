package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientThread extends Thread {

    private Socket connSocket;
    private String username;
    private BufferedReader inputFromClient;
    private PrintStream outputToClient;

    public ClientThread(Socket connSocket) {
        this.connSocket = connSocket;
    }

    public String getUsername() { return username; }
    public BufferedReader getInputFromClient() { return inputFromClient; }
    public PrintStream getOutputToClient() { return outputToClient; }

    @Override
    public void run() {

        initializeIO();

        try {

            while (true) {

                String messageFromClient = inputFromClient.readLine();

                if (messageFromClient == null) {
                    break;
                }

                if (messageFromClient.startsWith("Username")) {
                    String username = messageFromClient.split("@")[1];
                    if (validateUsernameUnique(username)) {
                        this.username = username;
                        outputToClient.println("Username@OK");
                    } else {
                        outputToClient.println("Username@NOT_UNIQUE");
                    }
                }

                if (messageFromClient.toLowerCase().startsWith("quit")) {
                    outputToClient.println("Goodbye " + username);
                    break;
                }

            }

            Server.onlinePlayers.remove(this);
            connSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void initializeIO() {
        try {
            inputFromClient = new BufferedReader(new InputStreamReader(connSocket.getInputStream()));
            outputToClient = new PrintStream(connSocket.getOutputStream());
        } catch (IOException ex) {
            System.out.println("Error initializing IO streams");
            ex.printStackTrace();
        }
    }

    private boolean validateUsernameUnique(String username) {
        for (var ct : Server.onlinePlayers) {
            if (username.equals(ct.getUsername())) {
                return false;
            }
        }
        return true;
    }
}
