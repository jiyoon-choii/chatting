package chat.chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * @author jiyoon
 */
public class ChatServer {

    private static final int SERVER_PORT = 12345;
    private static final String SERVER_START_MESSAGE = "Chat server is running on port: ";
    private static final String CLIENT_CONNECTED_MESSAGE = "New client connection established from: ";
    private static final String DEFAULT_ROOM = "lobby"; //기본 채팅방
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

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
        ClientSession session = null;

        try (
            BufferedReader messageReciever =
                    new BufferedReader(
                            new InputStreamReader(clientConnection.getInputStream()));
            PrintWriter messageTransmitter = new PrintWriter(clientConnection.getOutputStream(), true)
        ) {

            // 첫 줄은 닉네임으로 받기
            String nickname = messageReciever.readLine();
            if (nickname == null || nickname.isBlank()) {
                messageTransmitter.println("[Server] nickname required. closing...");
//                clientWriters.add(messageTransmitter);
                return;
            }

            // CientSession 생성
            session = new ClientSession(nickname, clientConnection, messageTransmitter);

            //방 없으면 로비에서 시작하도록
            rooms.putIfAbsent(DEFAULT_ROOM, new Room(DEFAULT_ROOM));
            Room lobby = rooms.get(DEFAULT_ROOM);
            lobby.join(session);
            lobby.broadcastToAll("[IN] " + nickname + " join " + DEFAULT_ROOM);

            // 현재 클라이언트의 writer를 등록
            clientWriters.add(messageTransmitter);

            String line;
            while ((line = messageReciever.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("/")) { // 명령어일 경우
                    try {
                        handleCommand(session, line, messageTransmitter);
                    } catch (Exception e) {
                        messageTransmitter.println("[Sever] internal error while processing command");
                        e.printStackTrace();
                    }
                } else {
                    Room current = session.getCurrentRoom();
                    if (current == null) {
                        messageTransmitter.println("[Server] you are not in any room");
                    } else {
                        System.out.println("Message from client: " + line);
//                        broadcast("Message received from [" + nickname + "] : " + line, messageTransmitter);
                        current.broadcastToOthers("[" + session.getNickname() + "] " + line, session);
                    }

                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client connection: " + e.getMessage());
        } finally {
            // 연결 종료 시 writer 제거 및 소켓 담기
            if (session != null) {
                Room current = session.getCurrentRoom();
                if (current != null) {
                    current.leave(session);
                }
                clientWriters.remove(session.getOut());
            }
            cleanupEmptyRooms();
            try {
                clientConnection.close();
            } catch (IOException ignored) {}
        }
    }



    private void broadcast(String message, PrintWriter exclude) {
        // concurrent set 이므로 반복 중 일부가 실패해도 전체 영향 최소화
        for (PrintWriter writer : clientWriters) {
            if (writer == exclude) continue; // 발신자 제외
            try {
                writer.println(message);
            } catch (Exception e) {
                //문제가 생긴 writter는 이후 제거되는 게 안전함
            }
        }
    }

    // 명령어 처리
    private void handleCommand(ClientSession session, String cmd, PrintWriter out) {
        String[] parts = cmd.split("\\s+", 2);
        String keyword = parts[0];
        String content = (parts.length > 1) ? parts[1].trim() : null; //방 이름 추출

        switch (keyword) {
            // 방 정보
            case "/rooms" : {

                // 방 정보 표시
                // 방 없음
                if (rooms.isEmpty()) {
                    out.println("[Server] no room exists");
                    break;
                } else {
                // 방 목록
                    StringBuilder sb = new StringBuilder("[Server] room list: ");
                    rooms.keySet().forEach(name -> sb.append(name).append(" "));
                    out.println(sb.toString().trim());
                }
                break;
            }

            // 참여 정보(입장)
            case "/join": {
                if (parts.length < 2 || parts[1].isBlank()) {
                    out.println("[Server] how to use: /join <room>");
                    break;
                }
                String roomName = parts[1].trim();

                //room 없을 시 에러
                Room target = rooms.get(roomName);
                if (target == null) {
                    out.println("[Server] room '" + roomName + "' does not exist. Use /createRoom <room> first.");
                    break;
                }

                //room 없을 시 생성
//                Room target = rooms.computeIfAbsent(roomName, Room::new);

                Room current = session.getCurrentRoom();

                // 대상 방이 현재 방인지 여부
                if (current != null && current == target) {
                    out.println("[Server] in " + roomName + " already");
                    break;
                }

                // 퇴장(대상방 <> 현재방일 시)
                if (current != null) {
                    current.leave(session);
                    current.broadcastToAll("[OUT] " + session.getNickname() + " leaves " + current.getName());
//                    break;
                }

                // 대상방으로 입장
                target.join(session);
                target.broadcastToAll("[IN] " + session.getNickname() + " join " + roomName);
                out.println("[SEVER] join'" + roomName + "'");
                break;
            }

            // 참여 정보(퇴장)
            case "/leave": {
                Room current = session.getCurrentRoom();
                // 현재 방 없음
                if (current == null) {
                    out.println("[Server] not in any room");
                    break;
                }

                // 현재 방이 로비라면 퇴장 불가 -> 종료
                if (DEFAULT_ROOM.equals(current.getName())) {
                    out.println("[Server] alreay in lobby; use /join <room> to move");
                    break;
                }

                // 퇴장 브로드캐스트
                current.leave(session);
                current.broadcastToAll("[OUT] " + session.getNickname() + " leaves " + current.getName());

                // 로비로 이동
                Room lobby = rooms.get(DEFAULT_ROOM); // 서버 시작 시 미리 putIfAbsent
                lobby.join(session);
                lobby.broadcastToAll("[IN] " + session.getNickname() + " join " + DEFAULT_ROOM);
                out.println("[Sever] moved to " + DEFAULT_ROOM);
                break;
            }

            // 방 멤버 보기
            case "/members" : {
                if (rooms.get(content) == null) {
                    out.println("[Server] room " + content + " does not exist");
                    break;
                } else {
                    out.println("[Server] " + content + " : " + rooms.get(content));
                    break;
                }
            }

            // 방 만들기
            case "/createRoom" : {
                // 명령어 생성 (서버) v
                // 명령어 입력 (클라이언트) v
                // 방 이름 입력 (클라이언트) v
                // 방 정보 서버 전달 (클라이언트->서버)
                    // 방 유효성 체크
                        // 이름 있는지
                if (content == null || content.isBlank()) {
                    out.println("[Server] No name");
                    break;
                }
                        // 중복 체크
                if (rooms.containsKey(content)) {
                    out.println("[Server] Duplicated room name");
                    break;
                }
                    // 검증 통과 ->

                // 방 생성 & 방 목록 업데이트 (서버)
                    // 방 객체 생성
                Room room = new Room(content);
                    // 방 목록에 추가
                rooms.put(room.getName(), room);


                // 방 참여 : 본인 소속 채팅방 정보 변경 (서버)
                // 방,본인 정보 전달 (서버->클라이언트)

                // 방으로 이동
                    //방 퇴장
                Room current = session.getCurrentRoom();
                current.leave(session);
                    // 현재 방 세팅
                session.setCurrentRoom(room);
                    //방 입장
                room.join(session);
                room.broadcastToAll("[IN] " + session.getNickname() + " join " + content);
                out.println("[Server] moved to " + content);
                break;

            }


            default: {
                out.println("[Server] unknown command: " + keyword);
                out.println("[Server] /rooms, /join <room>, /leave");
            }

            // 사용자 정보
                // 사용법
                // 닉네임 중복체크
                // 닉네임 변경
                // 연결 종료
        }

        cleanupEmptyRooms();
    }


    private void cleanupEmptyRooms() {
        rooms.entrySet().removeIf(e ->
                !DEFAULT_ROOM.equals(e.getKey()) && e.getValue().isEmpty()
        );
    }

    private static class Room {
        private final String name;
        private final Set<ClientSession> members = ConcurrentHashMap.newKeySet();

        Room(String name) {
            this.name = name;
        }

        String getName() { return name; }

        void join(ClientSession session) {
            members.add(session);
            session.setCurrentRoom(this);
        }

        void leave(ClientSession session) {
            members.remove(session);
            session.setCurrentRoom(null);
        }

        boolean isEmpty() { return members.isEmpty(); }

        void broadcastToAll(String message) {
            for (ClientSession m : members) {
                try {
                    m.getOut().println(message);
                } catch (Exception ignored) {}
            }
        }

        void broadcastToOthers(String message, ClientSession exclude) {
            if (members.size() == 1 && members.contains(exclude)) return;

            for (ClientSession m : members) {
                if (m == exclude) continue;
                try {
                    m.getOut().println(message);
                } catch (Exception ignored) {}
            }
        }
    }

    private static class ClientSession {
        private volatile String nickname;
        private final Socket socket;
        private final PrintWriter out;
        private volatile Room currentRoom;

        ClientSession(String nickname, Socket socket, PrintWriter out) {
            this.nickname = nickname;
            this.socket = socket;
            this.out = out;
        }

        String getNickname() { return nickname; }
        void setNickname(String nickname) { this.nickname = nickname; }

        Socket getSocket() { return socket; }
        PrintWriter getOut() { return out; }

        Room getCurrentRoom() { return currentRoom; }
        void setCurrentRoom(Room room) { this.currentRoom = room; }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.startChatServer();
        System.out.println("Chat server is running on port: " + SERVER_PORT);
    }

}
