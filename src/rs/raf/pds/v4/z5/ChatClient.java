package rs.raf.pds.v4.z5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.CreateRoom;
import rs.raf.pds.v4.z5.messages.InfoMessage;
import rs.raf.pds.v4.z5.messages.InviteUser;
import rs.raf.pds.v4.z5.messages.JoinRoom;
import rs.raf.pds.v4.z5.messages.KryoUtil;
import rs.raf.pds.v4.z5.messages.ListUsers;
import rs.raf.pds.v4.z5.messages.Login;
import rs.raf.pds.v4.z5.messages.RoomMessage;
import rs.raf.pds.v4.z5.messages.WhoRequest;

public class ChatClient implements Runnable{

	public static int DEFAULT_CLIENT_READ_BUFFER_SIZE = 1000000;
	public static int DEFAULT_CLIENT_WRITE_BUFFER_SIZE = 1000000;
	
	private volatile Thread thread = null;
	
	volatile boolean running = false;
	
	final Client client;
	final String hostName;
	final int portNumber;
	final String userName;
	
	private Map<String, Map<Integer, RoomMessage>> roomMessages = new HashMap<>();


	public ChatClient(String hostName, int portNumber, String userName) {
		this.client = new Client(DEFAULT_CLIENT_WRITE_BUFFER_SIZE, DEFAULT_CLIENT_READ_BUFFER_SIZE);
		
		this.hostName = hostName;
		this.portNumber = portNumber;
		this.userName = userName;
		KryoUtil.registerKryoClasses(client.getKryo());
		registerListener();
	}


	public void addMessageToRoom(String roomName, RoomMessage message) {
	    roomMessages.putIfAbsent(roomName, new HashMap<>());  // Kreira novu mapu ako ne postoji
	    roomMessages.get(roomName).put(message.getId(), message);
	}

	public String getMessageTextById(String roomName, int messageId) {
	    return roomMessages.getOrDefault(roomName, new HashMap<>())
	                        .getOrDefault(messageId, new RoomMessage("Unknown", "", "Unknown message", 0, ""))
	                        .getTxt();
	}

	
	// za front
	
	// Interfejs za prosleđivanje primljenih poruka GUI-ju
	public interface RoomMessageListener {
	    void onRoomMessageReceived(RoomMessage roomMessage);
	}

	public interface TextMessageListener {
	    void onTextMessageReceived(String message);
	}


	// Postavljanje listener-a
	private RoomMessageListener roomMessageListener;
	private TextMessageListener textMessageListener;

	public void setOnRoomMessageReceivedListener(RoomMessageListener listener) {
	    this.roomMessageListener = listener;
	}

	public void setOnTextMessageReceivedListener(TextMessageListener listener) {
	    this.textMessageListener = listener;
	}

	

	// Slanje poruka sa validacijom konekcije
	public void sendObject(Object message) {
	    if (client.isConnected()) {
	        client.sendTCP(message);
	    } else {
	        System.out.println("Niste povezani na server.");
	    }
	}


	private void showChatMessage(Object message) {
	    String formattedMessage = "";
	    String currentRoom = "Global Chat";

	    if (message instanceof ChatMessage) {
	        ChatMessage chatMessage = (ChatMessage) message;

	        System.out.println("DEBUG: Client userName = " + userName + ", Message user = " + chatMessage.getUser());

	        // IGNORIŠI ako je poruka od mene
	        if (chatMessage.getUser().equals(userName)) {
	            return;
	        }

	        formattedMessage = chatMessage.getUser() + ": " + chatMessage.getTxt();

	        if (textMessageListener != null) {
	            textMessageListener.onTextMessageReceived(formattedMessage);
	        }

	    } else if (message instanceof RoomMessage) {
	        RoomMessage roomMessage = (RoomMessage) message;

	        System.out.println("DEBUG: Client userName = " + userName + ", RoomMessage user = " + roomMessage.getUser());

	        // IGNORIŠI ako je poruka od mene
	        if (roomMessage.getUser().equals(userName)) {
	            return;
	        }

	        formattedMessage = "[" + roomMessage.getRoomName() + "] " +
	                           roomMessage.getUser() + ": " +
	                           roomMessage.getTxt();
	        currentRoom = roomMessage.getRoomName();

	        if (roomMessageListener != null) {
	            roomMessageListener.onRoomMessageReceived(roomMessage);
	        }
	    }

	    System.out.println(formattedMessage);
	}




	// za front
	
	private void registerListener() {
		client.addListener(new Listener() {
			public void connected (Connection connection) {
				Login loginMessage = new Login(userName);
				client.sendTCP(loginMessage);
			}
			
			public void received (Connection connection, Object object) {
				if (object instanceof ChatMessage) {
					ChatMessage chatMessage = (ChatMessage)object;
					showChatMessage(chatMessage);
					return;
				}

				if (object instanceof ListUsers) {
					ListUsers listUsers = (ListUsers)object;
					showOnlineUsers(listUsers.getUsers());
					return;
				}
				
				if (object instanceof InfoMessage) {
					InfoMessage message = (InfoMessage)object;
					 // Pozovi listener za GUI
				    if (textMessageListener != null) {
				    	textMessageListener.onTextMessageReceived("Server:" + message.getTxt());
				    }
					showMessage("Server:"+message.getTxt());
					return;
				}
				/*
				if (object instanceof ChatMessage) {
					ChatMessage message = (ChatMessage)object;
					showMessage(message.getUser()+"r:"+message.getTxt());
					return;
				}*/
				
				if (object instanceof ChatMessage) {
				    ChatMessage chatMessage = (ChatMessage) object;
				    if (chatMessage.getTxt().startsWith("(Room")) {
				        System.out.println(chatMessage.getTxt());
				    } else {
				        showChatMessage(chatMessage);
				    }
				    return;
				}
				
				 if (object instanceof String) {
					 System.out.println("Server: " + object);
					 return;
				 }
				 
				 if (object instanceof RoomMessage) {
					 RoomMessage roomMessage = (RoomMessage) object;

					 // Osiguraj da postoji lista poruka za sobu
					 roomMessages.putIfAbsent(roomMessage.getRoomName(), new HashMap<>());

					 // mora novi ID kada je nova soba
					 int newMessageId = roomMessages.get(roomMessage.getRoomName()).size() + 1;

					 roomMessage.setId(newMessageId);

					 roomMessages.get(roomMessage.getRoomName()).put(newMessageId, roomMessage);

					 if (roomMessage.getReplyToMessageId() != 0) {
						 System.out.println(roomMessage.getRoomName() + " Reply to [" + roomMessage.getReplyToMessageId() + "] '" 
					            + getMessageTextById(roomMessage.getRoomName(), roomMessage.getReplyToMessageId()) + "' by " 
					            + roomMessage.getUser() + ": [" + newMessageId + "] " + roomMessage.getTxt());
					 } else {
					        System.out.println(roomMessage.getRoomName() + " [" + newMessageId + "] " + roomMessage.getUser() + ": " + roomMessage.getTxt());
					 }
					 

				// --- obavestenje za gui ---
					 if (roomMessageListener != null) {
				        roomMessageListener.onRoomMessageReceived(roomMessage);
					 }
				 }


			}
			
			public void disconnected(Connection connection) {
				
			}
		});
	}
	
	// vrati ako ne radi
/*	private void showChatMessage(ChatMessage chatMessage) {
		System.out.println(chatMessage.getUser()+":"+chatMessage.getTxt());
	}*/

	private void showMessage(String txt) {
		System.out.println(txt);
	}
	private void showOnlineUsers(String[] users) {
		System.out.print("Server:");
		for (int i=0; i<users.length; i++) {
			String user = users[i];
			System.out.print(user);
			System.out.printf((i==users.length-1?"\n":", "));
		}
	}
	public void start() throws IOException {
		client.start();
		connect();
		
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}
	public void stop() {
		Thread stopThread = thread;
		thread = null;
		running = false;
		if (stopThread != null)
			stopThread.interrupt();
	}
	
	public void connect() throws IOException {
		client.connect(1000, hostName, portNumber);
	}
	public void run() {
		
		try (
				BufferedReader stdIn = new BufferedReader(
	                    new InputStreamReader(System.in))	// Za citanje sa standardnog ulaza - tastature!
	        ) {
					            
				String userInput;
				running = true;
				
	            while (running) {
	            	userInput = stdIn.readLine();
	            	if (userInput == null || "BYE".equalsIgnoreCase(userInput)) // userInput - tekst koji je unet sa tastature!
	            	{
	            		running = false;
	            		
	            	} else if ("WHO".equalsIgnoreCase(userInput)){
	            		client.sendTCP(new WhoRequest());
	            	} 
	            	// sobe
	            	else if (userInput.startsWith("CREATE ROOM ")) {
	            	    // Logika za kreiranje sobe
	            	    String roomName = userInput.substring(12).trim();
	                    client.sendTCP(new CreateRoom(roomName, userName));
	                } else if (userInput.startsWith("JOIN ROOM ")) {
	                    String roomName = userInput.substring(10).trim();
	                    client.sendTCP(new JoinRoom(roomName, userName));
	                } else if (userInput.startsWith("ROOM MSG ")) {
	                    String[] parts = userInput.split(" ", 4);
	                    if (parts.length < 4) {
	                        System.out.println("Usage: ROOM MSG <roomName> <message>");
	                    } else {
	                        String roomName = parts[2];
	                        String message = parts[3];
	                        client.sendTCP(new RoomMessage(userName, roomName, message));
	                    }
	                } 
	            	
	                else if (userInput.startsWith("INVITE USER ")) {
	                    String[] parts = userInput.split(" ", 4);
	                    if (parts.length < 4) {
	                        System.out.println("Usage: INVITE USER <roomName> <userName>");
	                    } else {
	                        String roomName = parts[2];
	                        String invitedUser = parts[3];
	                        client.sendTCP(new InviteUser(roomName, invitedUser));
	                    }
	                }
	                else if (userInput.startsWith("/getAllMessages")) {
	                    String[] parts = userInput.split(" ", 2); // Delimo unos na komandu i ime sobe
	                    if (parts.length < 2) {
	                        System.out.println("Usage: /getAllMessages <roomName>");
	                    } else {
	                        String roomName = parts[1].trim();
	                        System.out.println("Debug: Sending /getAllMessages " + roomName);
	                        client.sendTCP("/getAllMessages " + roomName); // Šaljemo serveru zahtev
	                    }
	                }
	            	
	                else if (userInput.startsWith("ROOM REPLY ")) {
	                    String[] parts = userInput.split(" ", 5);
	                    if (parts.length < 5) {
	                        System.out.println("Usage: ROOM REPLY <roomName> <messageId> <message>");
	                    } else {
	                        String roomName = parts[2];
	                        int replyToMessageId;
	                        try {
	                            replyToMessageId = Integer.parseInt(parts[3]);
	                        } catch (NumberFormatException e) {
	                            System.out.println("Invalid message ID format.");
	                            continue;
	                        }
	                        String message = parts[4];
	                        String replyToText = getMessageTextById(roomName, replyToMessageId); // nalazenje originalnog teksta poruke

	                        //System.out.println(">>> Pokušaj odgovora na poruku ID: " + replyToMessageId + " (Tekst: " + replyToText + ")");

	                        RoomMessage roomMessage = new RoomMessage(userName, roomName, message, replyToMessageId, replyToText);
	                      
	                        client.sendTCP(roomMessage);
	                    }
	                }



	            	
	            	else if (userInput.contains(":")) {
	            	    // Ovo je privatna poruka
	            		ChatMessage chatMessage = new ChatMessage(userName, userInput);
	            	    client.sendTCP(chatMessage);
	            	    showChatMessage(chatMessage); // da korisnik vidi i svoju poruku
	            	} else {
	            	    // Regularna poruka
	            	    ChatMessage message = new ChatMessage(userName, userInput);
	            	    client.sendTCP(message);
	            	    showChatMessage(message); 
	            	}
	            	
	            	if (!client.isConnected() && running)
	            		connect();
	            	
	           }
	            
	    } catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			running = false;
			System.out.println("CLIENT SE DISCONNECTUJE");
			client.close();;
		}
	}
	public static void main(String[] args) {
		if (args.length != 3) {
		
            System.err.println(
                "Usage: java -jar chatClient.jar <host name> <port number> <username>");
            System.out.println("Recommended port number is 54555");
            System.exit(1);
        }
 
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        String userName = args[2];
        
        try{
        	ChatClient chatClient = new ChatClient(hostName, portNumber, userName);
        	chatClient.start();
        }catch(IOException e) {
        	e.printStackTrace();
        	System.err.println("Error:"+e.getMessage());
        	System.exit(-1);
        }
	}



}
