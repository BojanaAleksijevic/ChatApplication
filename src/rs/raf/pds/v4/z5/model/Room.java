package rs.raf.pds.v4.z5.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.esotericsoftware.kryonet.Connection;

import rs.raf.pds.v4.z5.messages.RoomMessage;

public class Room {
	private String roomName;
	private final Map<String, Connection> users; // Mapiranje korisničkog imena na konekciju
	private final CopyOnWriteArrayList<RoomMessage> allMessages = new CopyOnWriteArrayList<>();
    private final LinkedList<RoomMessage> messageHistory; // Čuva poslednjih 10 poruka

    
    private int messageCounter = 1;
    
    
    
    public Room() {
        this.users = new HashMap<>();
        this.messageHistory = new LinkedList<>();
    }

    
    public Room(String roomName) {
        this.roomName = roomName;
        this.users = new HashMap<>();
        this.messageHistory = new LinkedList<>();
    }
	
    public Room(String roomName, List<String> userNames, List<RoomMessage> messages) {
        this.roomName = roomName;
        this.users = new ConcurrentHashMap<>();
        for (String userName : userNames) {
            this.users.put(userName, null); // Konekcija se može naknadno dodeliti
        }
        this.messageHistory = new LinkedList<>();
        if (messages != null) {
            for (RoomMessage message : messages) {
                addMessage(message); // Dodavanje postojećih poruka uz poštovanje limita
            }
        }
    }


	public String getRoomName() {
		return roomName;
	}
	
	public String getRoomName(Room room) {
	    return room.getRoomName();
	}

	
	public void addUser(String userName, Connection connection) {
	    users.put(userName, connection);
	}

	
	public void removeUser(String userName) {
		users.remove(userName);
	}
	
	public List<String> getUsers() {
	    return new ArrayList<>(users.keySet());
	}

	
	public boolean isEmpty() {
        return users.isEmpty();
    }
	
	public void addMessage(RoomMessage message) {
	    if (message == null) {
	        return;
	    }

	    if (message.isRoomMessage()) { 
	        message.setId(messageCounter++);
	        allMessages.add(message);  // Čuva sve poruke
	        messageHistory.add(message);  // Čuva samo poslednjih 10

	        System.out.println(">>> Dodata poruka sa ID: " + message.getId() + " - " + message.getTxt());
	        System.out.println("messageHistory trenutno ima: " + messageHistory.size());

	        if (messageHistory.size() > 10) {
	            messageHistory.removeFirst(); // Ukloni najstariju poruku
	        }
	        
	    }
	}



	public List<RoomMessage> getMessageHistory() {
		System.out.println("Returning message history, size: " + messageHistory.size());
	    for (RoomMessage msg : messageHistory) {
	        System.out.println("History message: " + msg.getUser() + ": " + msg.getTxt());
	    }
	    
	    
        return List.copyOf(messageHistory);
    }
	
	public List<RoomMessage> getAllMessages() {
	    return List.copyOf(allMessages);
	}
	
	public RoomMessage getMessageById(int id) {
        for (RoomMessage message : allMessages) {
            if (message.getId() == id) {
                return message;
            }
        }
        return null; // Poruka nije pronađena
    }
	
	

    
	public void broadcast(RoomMessage message, Connection exception) {
	    for (Connection conn : users.values()) {
	        if (conn != exception && conn != null && conn.isConnected()) {
	            conn.sendTCP(message); // Pošaljite poruku (bilo koji RoomMessage ili ReplyMessage objekat)
	        }
	    }
	}

    
    @Override
    public String toString() {
        return roomName;
    }

	
}
