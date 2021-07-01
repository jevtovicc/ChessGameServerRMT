package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ClientThread extends Thread {

    private final Socket connSocket;
    private String username;
    private String opponentUsername;
    private BufferedReader inputFromClient;
    private PrintStream outputToClient;
    private boolean isInGame;

    public ClientThread(Socket connSocket) {
        this.connSocket = connSocket;
    }

    public String getUsername() { return username; }

    @Override
    public void run() {

        initializeIO();

        try {

            while (true) {

                String messageFromClient = inputFromClient.readLine();

                if (messageFromClient == null) break;

                else if (messageFromClient.startsWith("quit")) {
                    outputToClient.println("Goodbye");
                    break;
                }

                else if (messageFromClient.startsWith("Username")) {
                    String username = messageFromClient.split("@")[1];
                    if (validateUsernameUnique(username)) {
                        this.username = username;
                        outputToClient.println("Username@OK");
                        // Notify others that new player connected
                        Server.onlinePlayers.stream()
                                .filter(x -> x != this && !x.isInGame)
                                .forEach(x -> x.outputToClient.println("NewOnlinePlayer@" + username));
                    } else {
                        outputToClient.println("Username@NOT_UNIQUE;" + username);
                    }
                }

                else if (messageFromClient.startsWith("OnlinePlayers")) {
                    outputToClient.println("OnlinePlayers@" + getAvailablePlayersUsernames());
                }

                else if (messageFromClient.startsWith("GameRequest")) {
                    String receiver = messageFromClient.split("@")[1];
                    ClientThread ct = findByUsername(receiver);
                    ct.outputToClient.println("GameRequest@" + username);
                }

                else if (messageFromClient.startsWith("InvitationAccept")) {
                    opponentUsername = messageFromClient.split("@")[1];
                    ClientThread sender = findByUsername(opponentUsername);
                    sender.outputToClient.println("InvitationAccept@" + username);
                    sender.isInGame = true;
                    sender.opponentUsername = username;
                    isInGame = true;
                    /* remove these two players from available players list */
                    Server.onlinePlayers.stream()
                            .filter(x -> x != sender && x != this)
                            .forEach(x -> {
                                x.outputToClient.println("PlayerDisconnected@" + opponentUsername);
                                x.outputToClient.println("PlayerDisconnected@" + username);
                            });
                }

                else if (messageFromClient.startsWith("InvitationReject")) {
                    String sender = messageFromClient.split("@")[1];
                    ClientThread ct = findByUsername(sender);
                    ct.outputToClient.println("InvitationReject@" + username);
                }

                else if (messageFromClient.startsWith("MoveMade")) {
                    /* parts format: opponentUsername,srcCol, srcRow,destCol,destRow */
                    String[] parts = messageFromClient.split("@")[1].split(",");
                    String opponentUsername = parts[0];
                    String srcCol = parts[1];
                    String srcRow = parts[2];
                    String destCol = parts[3];
                    String destRow = parts[4];

                    ClientThread ct = findByUsername(opponentUsername);
                    ct.outputToClient.println("MoveMade@" + srcCol + "," + srcRow + "," + destCol + "," + destRow);
                }

                else if (messageFromClient.startsWith("GameOver")) {
                    ClientThread winner = findByUsername(opponentUsername);
                    winner.outputToClient.println("GameWon");
                    winner.isInGame = false;
                    winner.opponentUsername = null;
                    outputToClient.println("GameLost");
                    isInGame = false;
                    opponentUsername = null;

                    /* add two players to available players list, so others can connect with them */
                    Server.onlinePlayers.stream()
                            .filter(x -> x != winner && x != this)
                            .forEach(x -> {
                                x.outputToClient.println("NewOnlinePlayer@" + winner.username);
                                x.outputToClient.println("NewOnlinePlayer@" + username);
                            });
                }

            }

            Server.onlinePlayers.remove(this);

            /* if terminated while in game, notify opponent that player disconnected*/
            if (isInGame) {
                ClientThread opponent = findByUsername(opponentUsername);
                opponent.outputToClient.println("OpponentDisconnected");
                opponent.isInGame = false;
                opponent.opponentUsername = null;
                /* add winner to available players list, so others can connect with him */
                Server.onlinePlayers.stream()
                        .filter(x -> x != opponent)
                        .forEach(x ->  x.outputToClient.println("NewOnlinePlayer@" + opponentUsername));
            }


            /* notify others that player disconnected */
            Server.onlinePlayers.forEach(x -> x.outputToClient.println("PlayerDisconnected@" + username));
            connSocket.close();
            System.out.println("Client disconnected and connection closed");
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
        return Server.onlinePlayers
                .stream()
                .noneMatch(ct -> username.equals(ct.getUsername()));
    }

    private String getAvailablePlayersUsernames() {
        return Server.onlinePlayers
            .stream()
            .filter(x -> x != this && !x.isInGame)
            .map(x -> x.username)
            .collect(Collectors.joining(","));
    }

    private ClientThread findByUsername(String username) {
        return Server.onlinePlayers.stream()
                .filter(x -> username.equals(x.username))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Player " + username + " not found"));
    }
}
