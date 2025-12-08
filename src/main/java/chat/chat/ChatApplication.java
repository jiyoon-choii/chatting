package chat.chat;

import chat.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatApplication {

	public static void main(String[] args) {

		ChatClient chatClient = new ChatClient();
		chatClient.startChatSession();

//		SpringApplication.run(ChatApplication.class, args);

	}


}
