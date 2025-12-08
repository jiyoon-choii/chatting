package chat.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.Buffer;

/**
 * @author jiyoon
 */
public class ChatClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    public void startChatSession() {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             BufferedReader userInputReader =
             new BufferedReader(new InputStreamReader(System.in));
             PrintWriter messageTransmitter =
             new PrintWriter(socket.getOutputStream(), true);
             BufferedReader messageReciever =
             new BufferedReader(new InputStreamReader(socket.getInputStream())))
        {
            System.out.println("Successfully connected to chat server.");
            handleChatSession(userInputReader, messageTransmitter, messageReciever);
        } catch (IOException e) {
            System.err.println("Socket Connection Error: " + e.getMessage());
        }
    }

    private static final String EXIT_COMMAND = "quit";
    private static final String PROMPT_MESSAGE = "Enter your message: ";

    private void handleChatSession(BufferedReader userInputReader,
                                   PrintWriter messageTransmitter,
                                   BufferedReader messageReciever
                                   ) throws IOException {
        String userMessage;
        while (true) {
            System.out.println(PROMPT_MESSAGE);
            userMessage = userInputReader.readLine();

            if (userMessage.equalsIgnoreCase(EXIT_COMMAND)) {
                System.out.println("Chat session ended.");
                break;
            }
        }

        sendAndRecieveMessage(userMessage, messageTransmitter, messageReciever);
    }

    private void sendAndRecieveMessage(String message,
                                       PrintWriter messageTransmitter,
                                       BufferedReader messageReciever
                                       ) throws IOException {

        messageTransmitter.println(message);
        String serverResponse = messageReciever.readLine();
        System.out.println("Server Response :" + serverResponse);
    }

}
