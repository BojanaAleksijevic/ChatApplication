package rs.raf.pds.v4.z5;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

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
import rs.raf.pds.v4.z5.model.Room;




public class ChatServer implements Runnable {

	private volatile Thread thread = null;

	volatile boolean running = false;
	final Server server;
	final int portNumber;
	ConcurrentMap<String, Connection> userConnectionMap = new ConcurrentHashMap<String, Connection>();
	ConcurrentMap<Connection, String> connectionUserMap = new ConcurrentHashMap<Connection, String>();

	// za sobe
	private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>(); // Sobe
	ConcurrentMap<String, Set<String>> roomUsers = new ConcurrentHashMap<>(); // Koji korisnici su u kojoj sobi

	
	
	public ChatServer(int portNumber) {
		this.server = new Server();

		this.portNumber = portNumber;
		KryoUtil.registerKryoClasses(server.getKryo());
		registerListener();
	}

	private void registerListener() {
		server.addListener(new Listener() {
			@Override
	        public void connected(Connection connection) {
	            // Slanje liste soba novom korisniku
	            if (!rooms.isEmpty()) {
	                StringBuilder roomList = new StringBuilder("Available rooms: ");
	                for (String roomName : rooms.keySet()) {
	                    roomList.append(roomName).append(", ");
	                }
	                // Uklanja poslednji zarez i razmak
	                roomList.setLength(roomList.length() - 2);
	                connection.sendTCP(new InfoMessage(roomList.toString()));
	            } else {
	                connection.sendTCP(new InfoMessage("No rooms available at the moment."));
	            }
	        }
			
			public void received(Connection connection, Object object) {
				if (object instanceof Login) {
					Login login = (Login) object;
					newUserLogged(login, connection);
					connection.sendTCP(new InfoMessage("Hello " + login.getUserName()));
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return;
				}

				if (object instanceof ChatMessage) {
					ChatMessage chatMessage = (ChatMessage) object;
					System.out.println(chatMessage.getUser() + ": " + chatMessage.getTxt());
					broadcastChatMessage(chatMessage, connection);
					return;
				}

				if (object instanceof WhoRequest) {
					ListUsers listUsers = new ListUsers(getAllUsers());
					connection.sendTCP(listUsers);
					return;
				}
				
				if (object instanceof CreateRoom) {
				    CreateRoom createRoom = (CreateRoom) object;

				    // Debug log za ime sobe i korisnika
				    System.out.println("Debug: Room Name = " + createRoom.getRoomName());
				    System.out.println("Debug: User Name = " + createRoom.getUserName());

				    // Provera da li soba već postoji
				    if (rooms.containsKey(createRoom.getRoomName())) {
				        connection.sendTCP(new InfoMessage("Room " + createRoom.getRoomName() + " already exists."));
				        return;
				    }

				    // Provera da ime sobe i korisnika nisu isto
				    if (createRoom.getRoomName().equals(createRoom.getUserName())) {
				        connection.sendTCP(new InfoMessage("Error: Room name and user name cannot be the same."));
				        return;
				    }


				    Room room = new Room(createRoom.getRoomName());
				    rooms.put(createRoom.getRoomName(), room);
				    System.out.println("Debug: Room created: " + room.getRoomName());

				    // Dodavanje kreatora sobe kao člana sobe
				    room.addUser(createRoom.getUserName(), connection); // Dodavanje korisnika direktno u objekat Room
				    roomUsers.computeIfAbsent(createRoom.getRoomName(), k -> new HashSet<>()).add(createRoom.getUserName());
				    userConnectionMap.put(createRoom.getUserName(), connection);
				    System.out.println("Debug: User " + createRoom.getUserName() + " added to room " + createRoom.getRoomName());

				    // Prikaz trenutnih članova sobe
				    Set<String> currentMembers = roomUsers.get(createRoom.getRoomName());
				    System.out.println("Trenutni clanovi sobe " + createRoom.getRoomName() + ": " + currentMembers);

				    connection.sendTCP(new InfoMessage("Room " + createRoom.getRoomName() + " created, and you joined it."));
				}

				
				if (object instanceof JoinRoom) {
				    JoinRoom joinRoom = (JoinRoom) object;

				    // Provera da li soba postoji
				    Room room = rooms.get(joinRoom.getRoomName());
				    if (room == null) {
				        connection.sendTCP(new InfoMessage("Room " + joinRoom.getRoomName() + " not found."));
				        return;
				    }

				    // Provera da li je korisnik već član sobe
				    Set<String> usersInRoom = roomUsers.computeIfAbsent(joinRoom.getRoomName(), k -> new HashSet<>());
				    if (usersInRoom.contains(joinRoom.getUserName())) {
				        connection.sendTCP(new InfoMessage("You are already a member of room " + joinRoom.getRoomName() + "."));
				        return;
				    }

				    // Dodavanje korisnika u sobu
				    usersInRoom.add(joinRoom.getUserName());
				    room.addUser(joinRoom.getUserName(), connection);

				    // Slanje poslednjih 10 poruka iz sobe korisniku
				    List<String> messageHistory = room.getMessageHistory();
				    for (String message : messageHistory) {
				        connection.sendTCP(new ChatMessage("Room " + joinRoom.getRoomName(), message));
				    }

				    
				    room.broadcast(joinRoom.getUserName() + " has joined the room.", null);

				    System.out.println("User " + joinRoom.getUserName() + " has joined the room " + joinRoom.getRoomName());
				    System.out.println("Trenutni clanovi sobe " + joinRoom.getRoomName() + ": " + usersInRoom);

				    connection.sendTCP(new InfoMessage("You have joined the room " + joinRoom.getRoomName() + ". WELCOME!"));
			
				}

				
				if (object instanceof RoomMessage) {
				    RoomMessage roomMessage = (RoomMessage) object;
				    Room room = rooms.get(roomMessage.getRoomName());

				    System.out.println("Debug: Room Name = " + roomMessage.getRoomName());
				    System.out.println("Debug: User Name = " + roomMessage.getUser());

				    if (room == null) {
				        connection.sendTCP(new InfoMessage("Room " + roomMessage.getRoomName() + " does not exist."));
				        return;
				    }

				    // Proveri da li je korisnik član sobe
				    Set<String> usersInRoom = roomUsers.get(roomMessage.getRoomName());
				    if (usersInRoom == null || !usersInRoom.contains(roomMessage.getUser())) {
				        connection.sendTCP(new InfoMessage("You are not a member of room " + roomMessage.getRoomName() + "."));
				        return;
				    }
				    
				    // Dodajemo poruku u istoriju sobe
				    room.addMessage(roomMessage.getUser() + ": " + roomMessage.getTxt());


				    // Pošalji poruku svim članovima sobe
				    room.broadcast(roomMessage.getUser() + ": " + roomMessage.getTxt(), connection);
				    
				    System.out.println("Debug: RoomMessage received. Room: " + roomMessage.getRoomName() + ", User: " + roomMessage.getUser() + ", Text: " + roomMessage.getTxt());
				}


				
				if (object instanceof InviteUser) {
				    InviteUser inviteUser = (InviteUser) object;

				    // Proveri da li soba postoji
				    Room room = rooms.get(inviteUser.getRoomName());
				    if (room != null) {
				        // Ispis trenutnih članova sobe
				        Set<String> usersInRoom = roomUsers.getOrDefault(inviteUser.getRoomName(), new HashSet<>());
				        System.out.println("Trenutni clanovi sobe " + inviteUser.getRoomName() + ": " + usersInRoom);

				        // Pronađi korisničko ime pozivaoca
				        String invitingUser = null;
				        for (Entry<String, Connection> entry : userConnectionMap.entrySet()) {
				            if (entry.getValue().equals(connection)) {
				                invitingUser = entry.getKey();
				                break;
				            }
				        }

				        // Ako pozivalac nije pronađen
				        if (invitingUser == null) {
				            connection.sendTCP(new InfoMessage("Error: Could not determine the inviting user."));
				            return;
				        }

				        // Proveri da li je pozivalac član sobe
				        if (!usersInRoom.contains(invitingUser)) {
				            connection.sendTCP(new InfoMessage("You must be a member of room " + inviteUser.getRoomName() + " to invite other users."));
				            return;
				        }

				        // Proveri da li je korisnik već u sobi
				        if (usersInRoom.contains(inviteUser.getInvitedUser())) {
				            connection.sendTCP(new InfoMessage("User " + inviteUser.getInvitedUser() + " is already a member of room " + inviteUser.getRoomName() + "."));
				        } else {
				            // Pošalji obaveštenje pozvanom korisniku
				            Connection invitedConnection = userConnectionMap.get(inviteUser.getInvitedUser());
				            if (invitedConnection != null) {
				                invitedConnection.sendTCP(new InfoMessage("You have been invited to room " + inviteUser.getRoomName() + "."));
				                connection.sendTCP(new InfoMessage("User " + inviteUser.getInvitedUser() + " invited to room " + inviteUser.getRoomName()));
				            } else {
				                connection.sendTCP(new InfoMessage("User " + inviteUser.getInvitedUser() + " is not online."));
				            }
				        }
				    } else {
				        connection.sendTCP(new InfoMessage("Room " + inviteUser.getRoomName() + " does not exist."));
				    }
				}



			}

			public void disconnected(Connection connection) {
				String user = connectionUserMap.get(connection);
				connectionUserMap.remove(connection);
				userConnectionMap.remove(user);
				showTextToAll(user + " has disconnected!", connection);
			}
		});
	}

	String[] getAllUsers() {
		String[] users = new String[userConnectionMap.size()];
		int i = 0;
		for (String user : userConnectionMap.keySet()) {
			users[i] = user;
			i++;
		}

		return users;
	}

	void newUserLogged(Login loginMessage, Connection conn) {
		userConnectionMap.put(loginMessage.getUserName(), conn);
		connectionUserMap.put(conn, loginMessage.getUserName());
		showTextToAll("User " + loginMessage.getUserName() + " has connected!", conn);
	}

	private void broadcastChatMessage(ChatMessage message, Connection exception) {
		String text = message.getTxt();

		// proverava da li je poruka privatna
		if (text.contains(":")) {
			String[] parts = text.split(":", 2);
			String recipientsPart = parts[0].trim();
			String privateMessage = parts[1].trim();

			if (recipientsPart.contains(",")) {
				// multicast
				String[] recipients = recipientsPart.split(",");
				for (String recipientName : recipients) {
					recipientName = recipientName.trim(); // bez razmaka
					Connection recipientConnection = userConnectionMap.get(recipientName);

					if (recipientConnection != null && recipientConnection.isConnected()) {
						recipientConnection.sendTCP(new ChatMessage(message.getUser(), "(Group) " + privateMessage));
					} else {
						exception.sendTCP(new InfoMessage("User " + recipientName + " is not available."));
					}
				}

			} else {
				// privatne poruke

				Connection recipientConnection = userConnectionMap.get(recipientsPart);
				if (recipientConnection != null && recipientConnection.isConnected()) {
					// salje privatnu poruku samo primaocu
					recipientConnection.sendTCP(new ChatMessage(message.getUser(), "(Private) " + privateMessage));
					// obavesti posiljaoca (exception) da je poruka poslata
					exception.sendTCP(new InfoMessage("Private message sent to user: " + recipientsPart));
				} else {
					// obavesti posiljaoca da korisnik ne postoji
					exception.sendTCP(new InfoMessage("User " + recipientsPart + " is not available."));
				}
			}
		} else {
			// u suprotnom, prosledi poruku svima (broadcast)
			for (Connection conn : userConnectionMap.values()) {
				if (conn.isConnected() && conn != exception) {
					conn.sendTCP(message);
				}
			}
		}
	}
	private void showTextToAll(String txt, Connection exception) {
		System.out.println(txt);
		for (Connection conn : userConnectionMap.values()) {
			if (conn.isConnected() && conn != exception)
				conn.sendTCP(new InfoMessage(txt));
		}
	}

	public void start() throws IOException {
		server.start();
		server.bind(portNumber);

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

	@Override
	public void run() {
		running = true;

		while (running) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {

		if (args.length != 1) {
			System.err.println("Usage: java -jar chatServer.jar <port number>");
			System.out.println("Recommended port number is 54555");
			System.exit(1);
		}

		int portNumber = Integer.parseInt(args[0]);
		try {
			ChatServer chatServer = new ChatServer(portNumber);
			chatServer.start();

			chatServer.thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
