package chat.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * @author jiyoon
 */
public class ChatClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    private static final String EXIT_COMMAND = "quit";
    private static final String PROMPT_MESSAGE = "Enter your message: ";

    public void startChatSession() {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             BufferedReader userInputReader =
                new BufferedReader(
                    new InputStreamReader(System.in));
             PrintWriter messageTransmitter =
                new PrintWriter(socket.getOutputStream(), true);
             BufferedReader messageReceiver =
                new BufferedReader(new InputStreamReader(socket.getInputStream())))
            {
                System.out.println("Successfully connected to chat server.");
    //            handleChatSession(userInputReader, messageTransmitter, messageReceiver);

                // 서버로부터 오는 메시지를 계속 수신하는 스레드
                Thread receiveThread = new Thread(() -> {
                    try {
                        String serverMessage;
                        while ((serverMessage = messageReceiver.readLine()) != null) {
                            System.out.println("[Server] " + serverMessage);
                        }
                    } catch (IOException e) {
                        System.err.println("Server read error: " + e.getMessage());
                    }
                });

            receiveThread.setDaemon(true);
            receiveThread.start();

            System.out.println("input your nickname : ");
            String nickname = userInputReader.readLine();
            if (nickname == null || nickname.isBlank()) {
                System.out.println("nickname is not input");
                return;
            }
            messageTransmitter.println(nickname); // 첫 줄로 닉네임 전송


            String userMessage;
                // 입력 루프(명령어/일반 메시지 모두 서버로 전송)
            while (true) {
                System.out.println(PROMPT_MESSAGE);
//                System.out.print("Enter your message (명령어: /rooms, /join <room>, /leave): ");
                userMessage = userInputReader.readLine();
                if (userMessage == null) break;

                if (userMessage.equalsIgnoreCase(EXIT_COMMAND)) {
                    System.out.println("Chat session ended.");
                    break;
                }

                // 방 생성일 시
                if (userMessage.equals("/createRoom")) {
                    // 방 이름 입력
                    System.out.println("input your room name : ");
                    String roomname = userInputReader.readLine();
                    // 예외 처리(인풋 없음)
                    if (roomname == null || roomname.isBlank()) {
                        System.out.println("roomname is not input");
                        return;
                    }
                    // 서버 전송
                    messageTransmitter.println(userMessage+" "+roomname);
                    continue;
                }

                // 클라이언트 메시지 표시
                messageTransmitter.println(userMessage);

            }
        } catch (IOException e) {
            System.err.println("Socket Connection Error: " + e.getMessage());
        }
    }



//    private void handleChatSession(BufferedReader userInputReader,
//                                   PrintWriter messageTransmitter,
//                                   BufferedReader messageReceiver
//                                   ) throws IOException {
//        String userMessage;
//        while (true) {
//            System.out.println(PROMPT_MESSAGE);
//            userMessage = userInputReader.readLine();
//
//            if (userMessage.equalsIgnoreCase(EXIT_COMMAND)) {
//                System.out.println("Chat session ended.");
//                break;
//            }
//        sendAndReceiverMessage(userMessage, messageTransmitter, messageReceiver);
//        }
//
//    }
//
//    private void sendAndReceiverMessage(String message,
//                                       PrintWriter messageTransmitter,
//                                       BufferedReader messageReceiver
//                                       ) throws IOException {
//
//        messageTransmitter.println(message);
//        String serverResponse = messageReceiver.readLine();
//        System.out.println("Server Response :" + serverResponse);
//    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.startChatSession();
    }

}
