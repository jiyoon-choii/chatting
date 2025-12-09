package chat.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jiyoon
 */
public class ChatServer {

    private static final int SERVER_PORT = 12345;
    private static final String SERVER_START_MESSAGE = "Chat server is running on port: ";
    private static final String CLIENT_CONNECTED_MESSAGE = "New client connection established from: ";

    // 모든 클라이언트에게 브로드캐스트하기 위한 출력 스트림 목록(스레드 안전하게)
    private final Set<PrintWriter> clientWriters = ConcurrentHashMap.newKeySet();

    public void startChatServer() {
        try (ServerSocket serverSocketConnection = new ServerSocket(SERVER_PORT)) {
            System.out.println(SERVER_START_MESSAGE + SERVER_PORT);

            while (true) {
                // 새로운 접속을 계속 수락
                Socket clientConnection = serverSocketConnection.accept();
                System.out.println(CLIENT_CONNECTED_MESSAGE + clientConnection.getInetAddress());

                // 클라이언트 처리를 별도 스레드에서 수행
                new Thread(() -> handleClient(clientConnection)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientConnection) {
        try (
            BufferedReader messageReciever =
                    new BufferedReader(
                            new InputStreamReader(clientConnection.getInputStream()));
            PrintWriter messageTransmitter = new PrintWriter(clientConnection.getOutputStream(), true)
        ) {
            // 현재 클라이언트의 writer를 등록
            clientWriters.add(messageTransmitter);

            String receiveMessage;
            while ((receiveMessage = messageReciever.readLine()) != null) {
                System.out.println("Message from client: " + receiveMessage);
                broadcast("Message received: " + receiveMessage);
            }
        } catch (IOException e) {
            System.err.println("Error handling client connection: " + e.getMessage());
        } finally {
            // 연결 종료 시 writer 제거 및 소켓 담기
            try {
                clientConnection.close();
            } catch (IOException ignored) {}
        }
    }

    private void broadcast(String message) {
        // concurrent set 이므로 반복 중 일부가 실패해도 전체 영향 최소화
        for (PrintWriter writer : clientWriters) {
            try {
                writer.println(message);
            } catch (Exception e) {
                //문제가 생긴 writter는 이후 제거되는 게 안전함
            }
        }
    }

//    public void startChatServer() {
//        try (ServerSocket serverSocketConnection = new ServerSocket(SERVER_PORT)) {
//            System.out.println(SERVER_START_MESSAGE + SERVER_PORT);
//
//            while (true) {
//                try (
//                    Socket clientConnection = serverSocketConnection.accept();
//                    BufferedReader messageReceiver = new BufferedReader(
//                        new InputStreamReader(clientConnection.getInputStream()));
//                        PrintWriter messageTransmitter = new PrintWriter(clientConnection.getOutputStream(), true))
//                {
//                    System.out.println(CLIENT_CONNECTED_MESSAGE + clientConnection.getInetAddress());
//                    String receiveMessage;
//                    while ((receiveMessage = messageReceiver.readLine()) != null) {
//                        System.out.println("Message from client: " + receiveMessage);
//                        String responseMessage = "Message received: " + receiveMessage;
//                        messageTransmitter.println(responseMessage);
//                    }
//                } catch (IOException e) {
//                    System.err.println("Error handling client connetion: "+ e.getMessage());
//                }
//            }
//        } catch (IOException e) {
//            System.err.println("Server error: " + e.getMessage());
//        }
//    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.startChatServer();
    }

}
