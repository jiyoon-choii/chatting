package chat.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author jiyoon
 */
public class ChatServer {

    private static final int SERVER_PORT = 12345;
    private static final String SERVER_START_MESSAGE = "Chat server is running on port: ";
    private static final String CLIENT_CONNECTED_MESSAGE = "New client connection established from: ";

    public void startChatServer() {
        try (ServerSocket serverSocketConnection = new ServerSocket(SERVER_PORT)) {
            System.out.println(SERVER_START_MESSAGE + SERVER_PORT);

            while (true) {
                try (Socket clientConnection = serverSocketConnection.accept();
                     BufferedReader messageReceiver = new BufferedReader(
                             new InputStreamReader(clientConnection.getInputStream()));
                     PrintWriter messageTransmitter = new PrintWriter(clientConnection.getOutputStream(), true))
                {
                    System.out.println(CLIENT_CONNECTED_MESSAGE + clientConnection.getInetAddress());
                    String recieveMessage;
                    while ((recieveMessage = messageReceiver.readLine()) != null) {
                        System.out.println("Message from client: " + recieveMessage);
                        String responseMessage = "Message received: " + recieveMessage;
                        messageTransmitter.println(responseMessage);
                    }
                } catch (IOException e) {
                    System.err.println("Error handling client connetion: "+ e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.startChatServer();
    }

}
