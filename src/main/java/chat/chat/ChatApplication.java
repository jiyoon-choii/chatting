package chat.chat;

import chat.chat.client.ChatClient;
import chat.chat.client.ChatClient2;
import chat.chat.server.ChatServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatApplication {

	public static void main(String[] args) {

		SpringApplication.run(ChatApplication.class, args);

	}


}
